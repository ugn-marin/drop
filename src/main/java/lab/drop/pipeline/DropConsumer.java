package lab.drop.pipeline;

import lab.drop.function.UnsafeConsumer;

import java.util.Objects;

/**
 * A pipeline worker consuming drops from a pipe.
 * @param <I> The input drops type.
 */
public abstract class DropConsumer<I> extends PipelineWorker implements UnsafeConsumer<I>, InputWorker<I> {
    private final Pipe<I> input;

    /**
     * Constructs a single-threaded consumer.
     * @param input The input pipe.
     */
    public DropConsumer(Pipe<I> input) {
        this(input, 1);
    }

    /**
     * Constructs a multithreaded consumer.
     * @param input The input pipe.
     * @param concurrency The maximum parallel drops consuming to allow.
     */
    public DropConsumer(Pipe<I> input, int concurrency) {
        this(false, input, concurrency);
    }

    DropConsumer(boolean internal, Pipe<I> input, int concurrency) {
        super(internal, concurrency);
        this.input = Objects.requireNonNull(input, "Input pipe is required.");
    }

    @Override
    public Pipe<I> getInput() {
        return input;
    }

    @Override
    void work() throws Exception {
        input.drain(drop -> submit(() -> busyRun(() -> accept(drop.drop()))));
    }

    /**
     * Consumes a drop from the input pipe.
     * @param drop The drop.
     * @throws Exception An exception terminating the pipeline.
     */
    public abstract void accept(I drop) throws Exception;

    @Override
    public void cancel(Throwable throwable) {
        super.cancel(throwable);
        input.setEndOfInput(throwable);
    }
}
