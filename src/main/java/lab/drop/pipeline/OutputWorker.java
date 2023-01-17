package lab.drop.pipeline;

/**
 * A worker containing an output pipe.
 * @param <O> The output drops type.
 */
public interface OutputWorker<O> {

    /**
     * Returns the output pipe.
     */
    Pipe<O> getOutput();

    /**
     * Constructs a drop consumer draining the output pipe of this worker.
     */
    default DropConsumer<O> drain() {
        return new Drain<>(getOutput());
    }

    /**
     * Constructs a drop consumer forwarding the output pipe of this worker into a supply gate.
     */
    default DropConsumer<O> forward(SupplyGate<O> supplyGate) {
        return new Forward<>(getOutput(), supplyGate);
    }
}
