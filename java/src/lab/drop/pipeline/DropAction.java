package lab.drop.pipeline;

import lab.drop.function.UnsafeConsumer;

/**
 * A drop function acting upon drops from an input pipe, and passing them to an output pipe as soon as done. Essentially
 * an identity function with a side effect.
 * @param <D> The drops type.
 */
public abstract class DropAction<D> extends DropFunction<D, D> implements UnsafeConsumer<D> {

    /**
     * Constructs a single-threaded action.
     * @param input The input pipe.
     * @param output The output pipe.
     */
    public DropAction(Pipe<D> input, Pipe<D> output) {
        super(input, output);
    }

    /**
     * Constructs a multithreaded action.
     * @param input The input pipe.
     * @param output The output pipe.
     * @param concurrency The maximum parallel drops accepting to allow.
     */
    public DropAction(Pipe<D> input, Pipe<D> output, int concurrency) {
        super(input, output, concurrency);
    }

    @Override
    public D apply(D drop) throws Exception {
        accept(drop);
        return drop;
    }

    /**
     * Accepts a drop from the input pipe.
     * @param drop The drop.
     * @throws Exception An exception terminating the pipeline.
     */
    public abstract void accept(D drop) throws Exception;
}
