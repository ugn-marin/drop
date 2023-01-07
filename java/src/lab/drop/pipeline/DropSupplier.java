package lab.drop.pipeline;

import lab.drop.Sugar;
import lab.drop.function.UnsafeSupplier;

import java.util.Objects;
import java.util.Optional;

/**
 * A pipeline worker supplying drops for a supply pipe.
 * @param <O> The output drops type.
 */
public abstract class DropSupplier<O> extends PipelineWorker implements UnsafeSupplier<Optional<O>>, SupplyGate<O>,
        OutputWorker<O> {
    private final SupplyPipe<O> output;

    /**
     * Constructs a single-threaded supplier.
     * @param output The output pipe.
     */
    public DropSupplier(SupplyPipe<O> output) {
        this(output, 1);
    }

    /**
     * Constructs a multithreaded supplier.
     * @param output The output pipe.
     * @param concurrency The maximum parallel drops supplying to allow.
     */
    public DropSupplier(SupplyPipe<O> output, int concurrency) {
        super(false, Sugar.requireRange(concurrency, 1, null));
        this.output = Objects.requireNonNull(output, "Output pipe is required.");
    }

    @Override
    public Pipe<O> getOutput() {
        return output;
    }

    @Override
    public void push(O drop) throws InterruptedException {
        output.push(drop);
    }

    @Override
    void work() {
        Sugar.iterate(getConcurrency(), i -> submit(() -> Sugar.acceptWhilePresent(() -> busyGet(this), this::push)));
    }

    /**
     * Supplies an optional drop for the output pipe.
     * @return An optional of the next drop to supply, or empty if no more drops available.
     * @throws Exception An exception terminating the pipeline.
     */
    public abstract Optional<O> get() throws Exception;

    @Override
    void internalClose() {
        output.setEndOfInput();
    }
}
