package lab.drop.pipeline;

import lab.drop.Sugar;
import lab.drop.data.Matrix;
import lab.drop.flow.OneShot;
import lab.drop.flow.Retry;
import lab.drop.function.Converter;
import lab.drop.function.Reducer;
import lab.drop.function.Unsafe;
import lab.drop.pipeline.monitoring.PipeMonitoring;
import lab.drop.pipeline.monitoring.PipelineComponentMonitoring;
import lab.drop.pipeline.monitoring.PipelineWorkerMonitoring;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A directed acyclic graph (DAG) of concurrent workers connected by pipes. May start at a supplier (closed pipeline) or
 * a supply pipe (open pipeline) and go through a number of functions and transformers. Ends with one or more consumers.
 * @param <S> The type of drops supplied at the start of the pipeline.
 */
public final class Pipeline<S> extends PipelineWorker implements SupplyGate<S> {
    private static final Converter<Object, Pipe<?>> mainPipeGetter = new Converter<Object, Pipe<?>>()
            .put(InputWorker.class, InputWorker::getInput).put(OutputWorker.class, OutputWorker::getOutput)
            .orElseThrow();

    private final List<PipelineWorker> pipelineWorkers;
    private final List<PipelineWorker> externalWorkers;
    private final SupplyPipe<S> supplyPipe;
    private final boolean isOpen;
    private final Set<PipelineWarning> pipelineWarnings;
    private final String simpleName;
    private final String string;
    private final Matrix<PipelineComponentMonitoring> componentsMonitoringMatrix;

    /**
     * Constructs a builder of a closed pipeline, and attaches the suppliers provided.
     * @param dropSuppliers One or more suppliers feeding the pipeline.
     * @param <S> The type of the supplied drops.
     * @return The pipeline builder.
     */
    @SafeVarargs
    public static <S> Builder<S> from(DropSupplier<S>... dropSuppliers) {
        return new Builder<>(dropSuppliers);
    }

    /**
     * Constructs a builder of an open pipeline.
     * @param supplyPipe The supply pipe feeding the pipeline.
     * @param <S> The type of the supplied drops.
     * @return The pipeline builder.
     */
    public static <S> Builder<S> from(SupplyPipe<S> supplyPipe) {
        return new Builder<>(supplyPipe);
    }

    private Pipeline(List<PipelineWorker> pipelineWorkers, SupplyPipe<S> supplyPipe,
                     Set<PipelineWarning> allowedWarnings) {
        super(true, pipelineWorkers.size());
        this.pipelineWorkers = pipelineWorkers;
        this.supplyPipe = supplyPipe;
        externalWorkers = pipelineWorkers.stream().filter(Predicate.not(PipelineWorker::isInternal)).toList();
        isOpen = externalWorkers.stream().noneMatch(pw -> pw instanceof DropSupplier);
        simpleName = isOpen ? "Open pipeline" : "Pipeline";
        StringBuilder sb = new StringBuilder(String.format("%s of %d workers on %d working threads:\n", simpleName,
                externalWorkers.size(), getConcurrency()));
        var pipelineChart = new PipelineChart(pipelineWorkers, supplyPipe);
        sb.append(pipelineChart);
        pipelineWarnings = pipelineChart.getWarnings();
        var unexpectedWarnings = pipelineWarnings.stream().filter(Predicate.not(allowedWarnings::contains))
                .collect(Collectors.toSet());
        if (!unexpectedWarnings.isEmpty())
            throw new PipelineConfigurationException(unexpectedWarnings, sb.toString());
        for (var warning : pipelineWarnings)
            sb.append("\n").append("Warning: ").append(warning.getDescription());
        string = sb.toString();
        componentsMonitoringMatrix = pipelineChart.getComponentsMonitoringMatrix();
    }

    /**
     * Returns the maximum number of auto-allocated threads that this pipeline's workers can use. That doesn't include
     * the threads managing and joining the workers, and the various internal workers.
     */
    @Override
    public int getConcurrency() {
        return externalWorkers.stream().mapToInt(PipelineWorker::getConcurrency).sum();
    }

    @Override
    public void setRetryBuilder(Retry.Builder retryBuilder) {
        externalWorkers.forEach(pw -> pw.setRetryBuilder(retryBuilder));
    }

    @Override
    public int getCanceledWork() {
        return pipelineWorkers.stream().mapToInt(PipelineWorker::getCanceledWork).sum();
    }

    @Override
    public double getCurrentUtilization() {
        return externalWorkers.stream().mapToDouble(pw -> pw.getCurrentUtilization() * pw.getConcurrency()).sum() /
                getConcurrency();
    }

    @Override
    public double getAverageUtilization() {
        return externalWorkers.stream().mapToDouble(pw -> pw.getAverageUtilization() * pw.getConcurrency()).sum() /
                getConcurrency();
    }

    @Override
    void work() {
        pipelineWorkers.forEach(this::submit);
    }

    /**
     * Pushes a drop into the supply pipe feeding the pipeline. This is the entry point of an open pipeline, although
     * it might be used for additional supply for a closed pipeline as well, as long as end of input wasn't reached.
     * @param drop The drop.
     * @throws InterruptedException If interrupted while attempting to push the drop.
     */
    @Override
    public void push(S drop) throws InterruptedException {
        supplyPipe.push(drop);
    }

    /**
     * Cancels the execution of all internal work, interrupts if possible. Does not wait for work to stop. The pipeline
     * will not throw an exception as a result of this operation. Equivalent to:
     * <pre>
     * cancel(null)
     * </pre>
     */
    public void stop() {
        cancel(null);
    }

    /**
     * Returns a monadic wrapper of the pipeline execution result. Equivalent to:
     * <pre>
     * toMonadicRunnable().get()
     * </pre>
     */
    public Unsafe<Void> get() {
        return toMonadicRunnable().get();
    }

    @Override
    void internalClose() throws Exception {
        if (isOpen)
            setEndOfInput();
        pipelineWorkers.forEach(pipelineWorker -> pipelineWorker.cancel(getThrowable()));
        Sugar.forEach(Stream.concat(Sugar.instancesOf(pipelineWorkers, InputWorker.class).stream(),
                Sugar.instancesOf(pipelineWorkers, OutputWorker.class).stream()).map(mainPipeGetter), Pipe::clear);
    }

    /**
     * Marks end of input for the pipeline supply. The pipeline will stop after all workers finish handling the drops
     * already in the pipes. Use for open pipelines only: If the pipeline has suppliers, they may fail on trying to push
     * after end of input.
     */
    public void setEndOfInput() {
        supplyPipe.setEndOfInput();
    }

    /**
     * Returns the warnings detected on the pipeline construction.
     */
    public Set<PipelineWarning> getWarnings() {
        return pipelineWarnings;
    }

    /**
     * Returns a matrix of the pipeline components (workers and pipes) according to their position in the pipeline as
     * discovered by the validation. Can be used for monitoring visualization - align objects according to the matrix.
     * Pipelines with certain construction warnings may return null.
     */
    public Matrix<PipelineComponentMonitoring> getComponentsMonitoringMatrix() {
        return componentsMonitoringMatrix;
    }

    /**
     * Returns the components matrix mapped using the mapping functions provided, according to their type.
     * @param pipeFunction The function mapping the pipes monitoring.
     * @param pipelineWorkerFunction The function mapping the pipeline workers monitoring.
     * @param <T> The type the components are mapped to.
     * @return An unmodifiable matrix of the mapping results, or null if the components matrix is null.
     */
    public <T> Matrix<T> getComponentsMonitoringMatrix(Function<PipeMonitoring, T> pipeFunction,
                                                       Function<PipelineWorkerMonitoring, T> pipelineWorkerFunction) {
        Objects.requireNonNull(pipeFunction, "Pipe function is null.");
        Objects.requireNonNull(pipelineWorkerFunction, "Pipeline worker function is null.");
        return componentsMonitoringMatrix == null ? null : Matrix.unmodifiableCopy(componentsMonitoringMatrix.map(
                new Converter<>(Map.of(PipeMonitoring.class, pipeFunction, PipelineWorkerMonitoring.class,
                        pipelineWorkerFunction))));
    }

    /**
     * Returns a set of input workers which have an input pipe average load of over 95%, during or after the execution.
     * An empty set means the supplier(s) probably hadn't subjected the pipeline to its potential throughput. Note that
     * bottlenecks according to this definition don't necessarily mean slowness, but only that the input pipes of said
     * workers are on average loaded to the max. The pipes themselves might be the bottleneck, perhaps simply due to the
     * structure of the pipeline, which directs more drops through them than through other pipes.
     */
    public Set<InputWorker<?>> getBottlenecks() {
        return Sugar.<InputWorker<?>>instancesOf(externalWorkers, InputWorker.class).stream()
                .filter(iw -> iw.getInput().getAverageLoad() > 0.95).collect(Collectors.toSet());
    }

    @Override
    public String getName() {
        return simpleName;
    }

    @Override
    public String toString() {
        return string;
    }

    /**
     * A pipeline builder.
     * @param <S> The type of drops supplied at the start of the pipeline.
     */
    public static final class Builder<S> {
        private final OneShot oneShot = new OneShot();
        private final List<PipelineWorker> pipelineWorkers = new ArrayList<>();
        private final SupplyPipe<S> supplyPipe;

        @SafeVarargs
        private Builder(DropSupplier<S>... dropSuppliers) {
            var supplyPipes = Stream.of(Sugar.requireFull(dropSuppliers)).map(DropSupplier::getOutput)
                    .collect(Collectors.toSet());
            if (supplyPipes.size() != 1)
                throw new PipelineConfigurationException("The pipeline suppliers must feed exactly 1 supply pipe.");
            supplyPipe = (SupplyPipe<S>) supplyPipes.stream().findFirst().get();
            attach(dropSuppliers);
        }

        private Builder(SupplyPipe<S> supplyPipe) {
            this.supplyPipe = Objects.requireNonNull(supplyPipe, "Supply pipe is null.");
        }

        /**
         * Attaches one or more functions to the pipeline.
         * @param dropFunctions One or more functions.
         * @return This builder.
         */
        public Builder<S> through(DropFunction<?, ?>... dropFunctions) {
            return attach(dropFunctions);
        }

        /**
         * Attaches one or more transformers to the pipeline.
         * @param dropTransformers One or more transformers.
         * @return This builder.
         */
        public Builder<S> through(DropTransformer<?, ?>... dropTransformers) {
            return attach(dropTransformers);
        }

        /**
         * Creates a fork from the input pipe into the output pipes.
         * @param input The input pipe.
         * @param outputs The output pipes.
         * @param <D> The drops type.
         * @return This builder.
         */
        @SafeVarargs
        public final <D> Builder<S> fork(Pipe<D> input, Pipe<D>... outputs) {
            return attach(new Fork<>(input, outputs));
        }

        /**
         * Creates a fork from the input pipe into the input workers (outputs of the fork). This does not attach the
         * workers to the pipeline.
         * @param input The input pipe.
         * @param outputs The input workers.
         * @param <D> The drops type.
         * @return This builder.
         */
        @SafeVarargs
        @SuppressWarnings("unchecked")
        public final <D> Builder<S> fork(Pipe<D> input, InputWorker<D>... outputs) {
            return fork(input, Stream.of(Sugar.requireFull(outputs)).map(InputWorker::getInput).toArray(Pipe[]::new));
        }

        /**
         * Creates a fork from the output worker (input of the fork) into the input workers (outputs of the fork). This
         * does not attach the workers to the pipeline.
         * @param input The output worker.
         * @param outputs The input workers.
         * @param <D> The drops type.
         * @return This builder.
         */
        @SafeVarargs
        public final <D> Builder<S> fork(OutputWorker<D> input, InputWorker<D>... outputs) {
            return fork(input.getOutput(), outputs);
        }

        /**
         * Creates a join from the input pipes into the output pipe.
         * @param output The output pipe.
         * @param inputs The input pipes.
         * @param <D> The drops type.
         * @return This builder.
         */
        @SafeVarargs
        public final <D> Builder<S> join(Pipe<D> output, Pipe<D>... inputs) {
            return join(null, output, inputs);
        }

        /**
         * Creates a join from the output workers (inputs of the join) into the output pipe. This does not attach the
         * workers to the pipeline.
         * @param output The output pipe.
         * @param inputs The output workers.
         * @param <D> The drops type.
         * @return This builder.
         */
        @SafeVarargs
        public final <D> Builder<S> join(Pipe<D> output, OutputWorker<D>... inputs) {
            return join(null, output, inputs);
        }

        /**
         * Creates a join from the output workers (inputs of the join) into the input worker (output of the join). This
         * does not attach the workers to the pipeline.
         * @param output The input worker.
         * @param inputs The output workers.
         * @param <D> The drops type.
         * @return This builder.
         */
        @SafeVarargs
        public final <D> Builder<S> join(InputWorker<D> output, OutputWorker<D>... inputs) {
            return join(null, output, inputs);
        }

        /**
         * Creates a join from the input pipes into the output pipe.
         * @param reducer An optional reducer for the join input drops.
         * @param output The output pipe.
         * @param inputs The input pipes.
         * @param <D> The drops type.
         * @return This builder.
         */
        @SafeVarargs
        public final <D> Builder<S> join(Reducer<D> reducer, Pipe<D> output, Pipe<D>... inputs) {
            return attach(new Join<>(reducer, output, inputs));
        }

        /**
         * Creates a join from the output workers (inputs of the join) into the output pipe. This does not attach the
         * workers to the pipeline.
         * @param reducer An optional reducer for the join input drops.
         * @param output The output pipe.
         * @param inputs The output workers.
         * @param <D> The drops type.
         * @return This builder.
         */
        @SafeVarargs
        @SuppressWarnings("unchecked")
        public final <D> Builder<S> join(Reducer<D> reducer, Pipe<D> output, OutputWorker<D>... inputs) {
            return join(reducer, output, Stream.of(Sugar.requireFull(inputs)).map(OutputWorker::getOutput)
                    .toArray(Pipe[]::new));
        }

        /**
         * Creates a join from the output workers (inputs of the join) into the input worker (output of the join). This
         * does not attach the workers to the pipeline.
         * @param reducer An optional reducer for the join input drops.
         * @param output The input worker.
         * @param inputs The output workers.
         * @param <D> The drops type.
         * @return This builder.
         */
        @SafeVarargs
        public final <D> Builder<S> join(Reducer<D> reducer, InputWorker<D> output, OutputWorker<D>... inputs) {
            return join(reducer, output.getInput(), inputs);
        }

        /**
         * Attaches one or more consumers to the pipeline.
         * @param dropConsumers One or more consumers.
         * @return The ready builder.
         */
        public ReadyBuilder<S> into(DropConsumer<?>... dropConsumers) {
            oneShot.check("The pipeline ready builder can be produced only once by a builder instance.");
            return new ReadyBuilder<>(attach(dropConsumers).pipelineWorkers, supplyPipe);
        }

        private Builder<S> attach(PipelineWorker... pipelineWorkers) {
            this.pipelineWorkers.addAll(List.of(Sugar.requireFull(pipelineWorkers)));
            return this;
        }
    }

    /**
     * A ready pipeline builder.
     * @param <S> The type of drops supplied at the start of the pipeline.
     */
    public static final class ReadyBuilder<S> {
        private final OneShot oneShot = new OneShot();
        private final List<PipelineWorker> pipelineWorkers;
        private final SupplyPipe<S> supplyPipe;

        private ReadyBuilder(List<PipelineWorker> pipelineWorkers, SupplyPipe<S> supplyPipe) {
            this.pipelineWorkers = pipelineWorkers;
            this.supplyPipe = supplyPipe;
        }

        /**
         * Builds the pipeline.
         * @param allowedWarnings Optional allowed pipeline warnings. Use with caution.
         * @return The pipeline.
         * @throws PipelineConfigurationException If got pipeline warnings not listed in <code>allowedWarnings</code>.
         */
        public Pipeline<S> build(PipelineWarning... allowedWarnings) {
            oneShot.check("The pipeline can be produced only once by a ready builder instance.");
            return new Pipeline<>(pipelineWorkers, supplyPipe, Set.of(allowedWarnings));
        }
    }
}
