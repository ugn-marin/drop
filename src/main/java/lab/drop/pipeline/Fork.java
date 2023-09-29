package lab.drop.pipeline;

import lab.drop.data.Data;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * An internal worker sending the input drop reference to several output pipes simultaneously. The fork can create a new
 * index scope for any output pipe that is a conditional supply pipe. Every supply pipe is pushed into synchronously to
 * enforce the drops order, potentially blocking other available pipes pushing.
 * @param <D> The drops type.
 */
final class Fork<D> extends PipelineWorker implements InputWorker<D> {
    private final Pipe<D> input;
    private final Pipe<D>[] outputs;

    @SafeVarargs
    Fork(Pipe<D> input, Pipe<D>... outputs) {
        super(true, (int) Stream.of(Data.requireNoneNull(outputs)).filter(p -> !(p instanceof SupplyGate)).count());
        if (outputs.length < 2)
            throw new PipelineConfigurationException("Fork requires at least 2 output pipes.");
        this.input = Objects.requireNonNull(input, "Input pipe is required.");
        this.outputs = outputs;
    }

    @Override
    public Pipe<D> getInput() {
        return input;
    }

    Pipe<D>[] getOutputs() {
        return outputs;
    }

    @Override
    void work() throws Exception {
        input.drain(drop -> {
            for (var output : outputs)
                if (output instanceof SupplyGate)
                    output.push(drop);
                else
                    submit(() -> output.push(drop));
        });
    }

    @Override
    void internalClose() {
        Stream.of(outputs).forEach(Pipe::setEndOfInput);
    }
}
