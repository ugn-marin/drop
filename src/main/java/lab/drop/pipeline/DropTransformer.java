package lab.drop.pipeline;

import lab.drop.function.UnsafeFunction;

import java.util.Collection;
import java.util.Objects;

/**
 * A pipeline worker consuming drops from an input pipe, applying a function to them and supplying them for an output
 * supply pipe in the provided order. The transformation function may return 0 to N output drops, thus transforming the
 * index scope of the pipeline workers down the line.
 * @param <I> The input drops type.
 * @param <O> The output drops type.
 */
public abstract class DropTransformer<I, O> extends PipelineWorker implements UnsafeFunction<I, Collection<O>>,
        InputWorker<I>, OutputWorker<O> {
    private final Pipe<I> input;
    private final SupplyPipe<O> output;

    /**
     * Constructs a single-threaded transformer.
     * @param input The input pipe.
     * @param output The output pipe.
     */
    public DropTransformer(Pipe<I> input, SupplyPipe<O> output) {
        this(input, output, 1);
    }

    /**
     * Constructs a multithreaded transformer.
     * @param input The input pipe.
     * @param output The output pipe.
     * @param concurrency The maximum parallel drops transforming to allow.
     */
    public DropTransformer(Pipe<I> input, SupplyPipe<O> output, int concurrency) {
        super(false, concurrency);
        this.input = Objects.requireNonNull(input, "Input pipe is required.");
        this.output = Objects.requireNonNull(output, "Output pipe is required.");
    }

    @Override
    public Pipe<I> getInput() {
        return input;
    }

    @Override
    public Pipe<O> getOutput() {
        return output;
    }

    @Override
    void work() throws Exception {
        input.drain(drop -> submit(() -> push(busyGet(() -> apply(drop.drop())))));
        submit(() -> push(busyGet(this::getLastDrops)));
    }

    private void push(Collection<O> transformedDrops) throws Exception {
        if (transformedDrops != null)
            for (O transformedItem : transformedDrops)
                output.push(transformedItem);
    }

    /**
     * Applies the function on an input drop.
     * @param drop The input drop
     * @return The transformed drops. If empty or null - skipped, else each output drop is pushed into the output pipe.
     * @throws Exception An exception terminating the pipeline.
     */
    public abstract Collection<O> apply(I drop) throws Exception;

    /**
     * Supplies leftover output drops when no drops left to transform. This would usually only make sense in a shrinking
     * transformer (returning 0 or 1 outputs per input) with an accumulative logic.
     * @return The transformed drops left to return at the end of input. If empty or null - skipped, else each output
     * drop is pushed into the output pipe.
     * @throws Exception An exception terminating the pipeline.
     */
    protected abstract Collection<O> getLastDrops() throws Exception;

    @Override
    public void cancel(Throwable throwable) {
        super.cancel(throwable);
        input.setEndOfInput(throwable);
    }

    @Override
    void internalClose() {
        output.setEndOfInput();
    }
}
