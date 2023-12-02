package lab.drop.pipeline;

/**
 * A queue of drops moved between pipeline workers, preserving the index scope.
 * @param <D> The drops type.
 */
public class ScopePipe<D> extends Pipe<D> {

    /**
     * Constructs a scope pipe.
     * @param baseCapacity The base capacity (<code>BC</code>) of the pipe. Used as the capacity for the in-order queue,
     *                     as well as the out-of-order drops cache. Together with the in-push drops, which depends on
     *                     the number of the pushing threads <code>N</code>, the total maximum theoretical capacity of
     *                     the pipe can reach <code>BC+N</code>.
     */
    public ScopePipe(int baseCapacity) {
        this(baseCapacity, "-P");
    }

    /**
     * Constructs a scope pipe.
     * @param baseCapacity The base capacity (<code>BC</code>) of the pipe. Used as the capacity for the in-order queue,
     *                     as well as the out-of-order drops cache. Together with the in-push drops, which depends on
     *                     the number of the pushing threads <code>N</code>, the total maximum theoretical capacity of
     *                     the pipe can reach <code>BC+N</code>.
     * @param name The name of the pipe.
     */
    public ScopePipe(int baseCapacity, String name) {
        super(baseCapacity, name);
    }
}
