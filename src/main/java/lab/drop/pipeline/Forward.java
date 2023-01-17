package lab.drop.pipeline;

import java.util.Objects;

/**
 * A forward consumer pushing the input drops into a supply gate.
 * @param <I> The input drops type.
 */
final class Forward<I> extends DropConsumer<I> {
    final SupplyGate<I> output;

    Forward(Pipe<I> input, SupplyGate<I> output) {
        super(true, input, 1);
        this.output = Objects.requireNonNull(output, "Supply gate is required.");
    }

    @Override
    void work() throws Exception {
        getInput().drain(drop -> submit(() -> output.push(drop.drop())));
    }

    @Override
    public void accept(I drop) {}

    @Override
    public String getName() {
        return "->";
    }
}
