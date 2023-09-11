package lab.drop.pipeline;

import lab.drop.function.ConditionalConsumer;
import lab.drop.function.UnsafeConsumer;
import lab.drop.function.UnsafeFunction;
import lab.drop.function.UnsafeSupplier;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Utility methods for creating simple pipelines and pipeline workers.
 */
public class Pipelines {

    private Pipelines() {}

    /**
     * Returns the number of processors available to the JVM to use as a worker concurrency level.
     */
    public static int fullConcurrency() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * Constructs a pipeline from the supplier into the consumer. Equivalent to:
     * <pre>
     * Pipeline.from(supplier).into(consumer).build()
     * </pre>
     * @param dropSupplier The supplier.
     * @param dropConsumer The consumer.
     * @param <D> The drops type.
     * @return The pipeline.
     */
    public static <D> Pipeline<D> direct(DropSupplier<D> dropSupplier, DropConsumer<D> dropConsumer) {
        return Pipeline.from(dropSupplier).into(dropConsumer).build();
    }

    /**
     * Constructs a pipeline of non-null elements from the supplier into the consumer using a simple supply pipe.
     * @param get The supplier get implementation.
     * @param accept The consumer accept implementation.
     * @param <D> The drops type.
     * @return The pipeline.
     */
    public static <D> Pipeline<D> direct(UnsafeSupplier<D> get, UnsafeConsumer<D> accept) {
        SupplyPipe<D> supplyPipe = new SupplyPipe<>(1);
        return direct(supplier(supplyPipe, get), consumer(supplyPipe, accept));
    }

    /**
     * Constructs a pipeline from a simple supplier of the non-null elements from the stream, into the consumer.
     * @param stream The stream.
     * @param dropConsumer The consumer.
     * @param <D> The drops type.
     * @return The pipeline.
     */
    public static <D> Pipeline<D> direct(Stream<D> stream, DropConsumer<D> dropConsumer) {
        if (!(Objects.requireNonNull(dropConsumer, "Consumer is null.").getInput() instanceof SupplyPipe))
            throw new PipelineConfigurationException("The direct pipeline consumer input pipe must be a supply pipe.");
        return direct(supplier((SupplyPipe<D>) dropConsumer.getInput(), stream), dropConsumer);
    }

    /**
     * Constructs a pipeline from a simple supplier of the non-null elements from the stream, into a simple
     * multithreaded consumer. Similar to (plus the pipeline functionality of cancellation, concurrency control etc.):
     * <pre>
     * stream.filter(Objects::nonNull).parallel().forEach(accept);
     * </pre>
     * @param stream The stream.
     * @param concurrency The maximum parallel drops consuming to allow.
     * @param accept The consumer accept implementation.
     * @param <D> The drops type.
     * @return The pipeline.
     */
    public static <D> Pipeline<D> direct(Stream<D> stream, int concurrency, UnsafeConsumer<D> accept) {
        SupplyPipe<D> supplyPipe = new SupplyPipe<>(concurrency);
        return direct(supplier(supplyPipe, stream), consumer(supplyPipe, concurrency, accept));
    }

    /**
     * Constructs a star pipeline forking from the supplier into all the consumers.
     * @param dropSupplier The supplier.
     * @param dropConsumers The drop consumers.
     * @param <D> The drops type.
     * @return The pipeline.
     */
    @SafeVarargs
    public static <D> Pipeline<D> star(DropSupplier<D> dropSupplier, DropConsumer<D>... dropConsumers) {
        return Pipeline.from(dropSupplier).fork(dropSupplier, dropConsumers).into(dropConsumers).build();
    }

    /**
     * Constructs an open star pipeline forking from the supply pipe into all the consumers.
     * @param supplyPipe The supply pipe.
     * @param dropConsumers The drop consumers.
     * @param <D> The drops type.
     * @return The pipeline.
     */
    @SafeVarargs
    public static <D> Pipeline<D> star(SupplyPipe<D> supplyPipe, DropConsumer<D>... dropConsumers) {
        return Pipeline.from(supplyPipe).fork(supplyPipe, dropConsumers).into(dropConsumers).build();
    }

    /**
     * Constructs an open star pipeline forking from a supply pipe into the two consumers by the predicate result.
     * @param conditionalConsumer A conditional consumer containing the predicate and respective consumers.
     * @param <D> The drops type.
     * @return The pipeline.
     */
    public static <D> Pipeline<D> split(ConditionalConsumer<D> conditionalConsumer) {
        return split(conditionalConsumer.predicate(), conditionalConsumer.positive()::accept,
                conditionalConsumer.negative()::accept);
    }

    /**
     * Constructs an open star pipeline forking from a supply pipe into the two consumers by the predicate result.
     * @param predicate The predicate by which to accept the drops.
     * @param acceptTrue The consumer for drops passing the predicate.
     * @param acceptFalse The consumer for drops not passing the predicate.
     * @param <D> The drops type.
     * @return The pipeline.
     */
    public static <D> Pipeline<D> split(Predicate<D> predicate, UnsafeConsumer<D> acceptTrue,
                                        UnsafeConsumer<D> acceptFalse) {
        return split(Map.of(predicate, acceptTrue, Predicate.not(predicate), acceptFalse));
    }

    /**
     * Constructs an open star pipeline forking from a supply pipe into consumers by predicates' results.
     * @param splitConsumers The predicates mapped to consumers.
     * @param <D> The drops type.
     * @return The pipeline.
     */
    @SuppressWarnings("unchecked")
    public static <D> Pipeline<D> split(Map<Predicate<D>, UnsafeConsumer<D>> splitConsumers) {
        return star(new SupplyPipe<>(1), splitConsumers.entrySet().stream().map(entry -> consumer(
                new SupplyPipe<>(1, entry.getKey()), entry.getValue())).toArray(DropConsumer[]::new));
    }

    /**
     * Constructs a simple drop supplier of non-null elements.
     * @param output The output pipe.
     * @param get The get implementation.
     * @param <O> The output drops type.
     * @return The supplier.
     */
    public static <O> DropSupplier<O> supplier(SupplyPipe<O> output, UnsafeSupplier<O> get) {
        return supplier(output, 1, get);
    }

    /**
     * Constructs a simple multithreaded drop supplier of non-null elements.
     * @param output The output pipe.
     * @param concurrency The maximum parallel drops supplying to allow.
     * @param get The get implementation.
     * @param <O> The output drops type.
     * @return The supplier.
     */
    public static <O> DropSupplier<O> supplier(SupplyPipe<O> output, int concurrency, UnsafeSupplier<O> get) {
        Objects.requireNonNull(get, "Get supplier is required.");
        return new DropSupplier<>(output, concurrency) {

            @Override
            public Optional<O> get() throws Exception {
                return Optional.ofNullable(get.get());
            }
        };
    }

    /**
     * Constructs a simple drop supplier of the non-null elements from the stream.
     * @param output The output pipe.
     * @param stream The stream.
     * @param <O> The output drops type.
     * @return The supplier.
     */
    public static <O> DropSupplier<O> supplier(SupplyPipe<O> output, Stream<O> stream) {
        var iterator = Objects.requireNonNull(stream, "Stream is null.").filter(Objects::nonNull).iterator();
        return supplier(output, () -> iterator.hasNext() ? iterator.next() : null);
    }

    /**
     * Constructs a drop supplier of a single non-null object.
     * @param object The object to supply.
     * @param <O> The object type.
     * @return The supplier.
     */
    public static <O> DropSupplier<O> supplier(O object) {
        return supplier(new SupplyPipe<>(1), Stream.of(Objects.requireNonNull(object, "Object is null.")));
    }

    /**
     * Constructs a simple drop function.
     * @param input The input pipe.
     * @param output The output pipe.
     * @param apply The apply implementation.
     * @param <I> The input drops type.
     * @param <O> The output drops type.
     * @return The function.
     */
    public static <I, O> DropFunction<I, O> function(Pipe<I> input, Pipe<O> output, UnsafeFunction<I, O> apply) {
        return function(input, output, 1, apply);
    }

    /**
     * Constructs a simple multithreaded drop function.
     * @param input The input pipe.
     * @param output The output pipe.
     * @param concurrency The maximum parallel drops applying to allow.
     * @param apply The apply implementation.
     * @param <I> The input drops type.
     * @param <O> The output drops type.
     * @return The function.
     */
    public static <I, O> DropFunction<I, O> function(Pipe<I> input, Pipe<O> output, int concurrency,
                                                     UnsafeFunction<I, O> apply) {
        Objects.requireNonNull(apply, "Apply function is required.");
        return new DropFunction<>(input, output, concurrency) {

            @Override
            public O apply(I drop) throws Exception {
                return apply.apply(drop);
            }
        };
    }

    /**
     * Constructs a simple drop action.
     * @param accept The accept implementation.
     * @param <D> The drops type.
     * @return The action.
     */
    public static <D> DropAction<D> action(UnsafeConsumer<D> accept) {
        return action(1, accept);
    }

    /**
     * Constructs a simple multithreaded drop action.
     * @param concurrency The maximum parallel drops accepting to allow.
     * @param accept The accept implementation.
     * @param <D> The drops type.
     * @return The action.
     */
    public static <D> DropAction<D> action(int concurrency, UnsafeConsumer<D> accept) {
        return action(new ScopePipe<>(concurrency), new ScopePipe<>(concurrency), concurrency, accept);
    }

    /**
     * Constructs a simple drop action.
     * @param input The input pipe.
     * @param output The output pipe.
     * @param accept The accept implementation.
     * @param <D> The drops type.
     * @return The action.
     */
    public static <D> DropAction<D> action(Pipe<D> input, Pipe<D> output, UnsafeConsumer<D> accept) {
        return action(input, output, 1, accept);
    }

    /**
     * Constructs a simple multithreaded drop action.
     * @param input The input pipe.
     * @param output The output pipe.
     * @param concurrency The maximum parallel drops accepting to allow.
     * @param accept The accept implementation.
     * @param <D> The drops type.
     * @return The action.
     */
    public static <D> DropAction<D> action(Pipe<D> input, Pipe<D> output, int concurrency, UnsafeConsumer<D> accept) {
        Objects.requireNonNull(accept, "Accept consumer is required.");
        return new DropAction<>(input, output, concurrency) {

            @Override
            public void accept(D drop) throws Exception {
                accept.accept(drop);
            }
        };
    }

    /**
     * Constructs a simple drop transformer.
     * @param input The input pipe.
     * @param output The output pipe.
     * @param apply The apply implementation.
     * @param getLastDrops The getLastDrops implementation (optional).
     * @param <I> The input drops type.
     * @param <O> The output drops type.
     * @return The transformer.
     */
    public static <I, O> DropTransformer<I, O> transformer(Pipe<I> input, SupplyPipe<O> output,
                                                           UnsafeFunction<I, Collection<O>> apply,
                                                           UnsafeSupplier<Collection<O>> getLastDrops) {
        return transformer(input, output, 1, apply, getLastDrops);
    }

    /**
     * Constructs a simple multithreaded drop transformer.
     * @param input The input pipe.
     * @param output The output pipe.
     * @param concurrency The maximum parallel drops transforming to allow.
     * @param apply The apply implementation.
     * @param getLastDrops The getLastDrops implementation (optional).
     * @param <I> The input drops type.
     * @param <O> The output drops type.
     * @return The transformer.
     */
    public static <I, O> DropTransformer<I, O> transformer(Pipe<I> input, SupplyPipe<O> output, int concurrency,
                                                           UnsafeFunction<I, Collection<O>> apply,
                                                           UnsafeSupplier<Collection<O>> getLastDrops) {
        Objects.requireNonNull(apply, "Apply function is required.");
        return new DropTransformer<>(input, output, concurrency) {

            @Override
            public Collection<O> apply(I drop) throws Exception {
                return apply.apply(drop);
            }

            @Override
            protected Collection<O> getLastDrops() throws Exception {
                return getLastDrops != null ? getLastDrops.get() : null;
            }
        };
    }

    /**
     * Constructs a simple drop consumer.
     * @param accept The accept implementation.
     * @param <I> The input drops type.
     * @return The consumer.
     */
    public static <I> DropConsumer<I> consumer(UnsafeConsumer<I> accept) {
        return consumer(1, accept);
    }

    /**
     * Constructs a simple multithreaded drop consumer.
     * @param concurrency The maximum parallel drops consuming to allow.
     * @param accept The accept implementation.
     * @param <I> The input drops type.
     * @return The consumer.
     */
    public static <I> DropConsumer<I> consumer(int concurrency, UnsafeConsumer<I> accept) {
        return consumer(new ScopePipe<>(concurrency), concurrency, accept);
    }

    /**
     * Constructs a simple drop consumer.
     * @param input The input pipe.
     * @param accept The accept implementation.
     * @param <I> The input drops type.
     * @return The consumer.
     */
    public static <I> DropConsumer<I> consumer(Pipe<I> input, UnsafeConsumer<I> accept) {
        return consumer(input, 1, accept);
    }

    /**
     * Constructs a simple multithreaded drop consumer.
     * @param input The input pipe.
     * @param concurrency The maximum parallel drops consuming to allow.
     * @param accept The accept implementation.
     * @param <I> The input drops type.
     * @return The consumer.
     */
    public static <I> DropConsumer<I> consumer(Pipe<I> input, int concurrency, UnsafeConsumer<I> accept) {
        Objects.requireNonNull(accept, "Accept consumer is required.");
        return new DropConsumer<>(input, concurrency) {

            @Override
            public void accept(I drop) throws Exception {
                accept.accept(drop);
            }
        };
    }
}
