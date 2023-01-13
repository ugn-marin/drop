package lab.drop.pipeline;

/**
 * A worker containing an input pipe.
 * @param <I> The input drops type.
 */
public interface InputWorker<I> {

    /**
     * Return the input pipe.
     */
    Pipe<I> getInput();
}
