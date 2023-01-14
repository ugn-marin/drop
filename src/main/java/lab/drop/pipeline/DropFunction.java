package lab.drop.pipeline;

import lab.drop.Sugar;
import lab.drop.function.UnsafeFunction;

import java.util.Objects;

/**
 * A pipeline worker consuming drops from an input pipe, applying a function on them and supplying the output to an
 * output pipe.
 * @param <I> The input drops type.
 * @param <O> The output drops type.
 */
public abstract class DropFunction<I, O> extends PipelineWorker implements UnsafeFunction<I, O>, InputWorker<I>,
        OutputWorker<O> {
    private final Pipe<I> input;
    private final Pipe<O> output;

    /**
     * Constructs a single-threaded function.
     * @param input The input pipe.
     * @param output The output pipe.
     */
    public DropFunction(Pipe<I> input, Pipe<O> output) {
        this(input, output, 1);
    }

    /**
     * Constructs a multithreaded function.
     * @param input The input pipe.
     * @param output The output pipe.
     * @param concurrency The maximum parallel drops applying to allow.
     */
    public DropFunction(Pipe<I> input, Pipe<O> output, int concurrency) {
        super(false, Sugar.requireRange(concurrency, 1, null));
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
        input.drain(drop -> submit(() -> output.push(new Drop<>(drop.index(), busyGet(() -> apply(drop.drop()))))));
    }

    /**
     * Applies the function on an input drop.
     * @param drop The input drop.
     * @return The output drop.
     * @throws Exception An exception terminating the pipeline.
     */
    public abstract O apply(I drop) throws Exception;

    @Override
    public void cancel(Throwable throwable) {
        super.cancel(throwable);
        input.setEndOfInput();
    }

    @Override
    void internalClose() {
        output.setEndOfInput();
    }
}
