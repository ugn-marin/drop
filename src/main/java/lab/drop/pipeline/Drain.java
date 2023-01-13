package lab.drop.pipeline;

/**
 * A drop consumer draining a pipe with no additional logic.
 * @param <I> The input drops type.
 */
final class Drain<I> extends DropConsumer<I> {

    Drain(Pipe<I> input) {
        super(true, input, 0);
    }

    @Override
    void work() throws Exception {
        getInput().drain();
    }

    @Override
    public void accept(I drop) {}

    @Override
    public String getName() {
        return "x";
    }
}
