package lab.drop.pipeline;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * A queue of drops supplied by a pipeline worker, and/or from the outside.
 * @param <D> The drops type.
 */
public class SupplyPipe<D> extends Pipe<D> implements SupplyGate<D> {
    private final AtomicLong index = new AtomicLong();
    private final Predicate<D> predicate;

    /**
     * Constructs a supply pipe.
     * @param baseCapacity The base capacity (<code>BC</code>) of the pipe. Used as the capacity for the in-order queue,
     *                     as well as the out-of-order drops cache. Together with the in-push drops, which depends on
     *                     the number of the pushing threads <code>N</code>, the total maximum theoretical capacity of
     *                     the pipe can reach <code>BC+N</code>.
     */
    public SupplyPipe(int baseCapacity) {
        this(baseCapacity, (Predicate<D>) null);
    }

    /**
     * Constructs a supply pipe.
     * @param baseCapacity The base capacity (<code>BC</code>) of the pipe. Used as the capacity for the in-order queue,
     *                     as well as the out-of-order drops cache. Together with the in-push drops, which depends on
     *                     the number of the pushing threads <code>N</code>, the total maximum theoretical capacity of
     *                     the pipe can reach <code>BC+N</code>.
     * @param name The name of the pipe.
     */
    public SupplyPipe(int baseCapacity, String name) {
        this(baseCapacity, name, null);
    }

    /**
     * Constructs a conditional supply pipe.
     * @param baseCapacity The base capacity (<code>BC</code>) of the pipe. Used as the capacity for the in-order queue,
     *                     as well as the out-of-order drops cache. Together with the in-push drops, which depends on
     *                     the number of the pushing threads <code>N</code>, the total maximum theoretical capacity of
     *                     the pipe can reach <code>BC+N</code>.
     * @param predicate The predicate by which to accept pushed drops into the pipe. Ignored if null.
     */
    public SupplyPipe(int baseCapacity, Predicate<D> predicate) {
        this(baseCapacity, String.format("*%sP", predicate != null ? "?" : ""), predicate);
    }

    /**
     * Constructs a conditional supply pipe.
     * @param baseCapacity The base capacity (<code>BC</code>) of the pipe. Used as the capacity for the in-order queue,
     *                     as well as the out-of-order drops cache. Together with the in-push drops, which depends on
     *                     the number of the pushing threads <code>N</code>, the total maximum theoretical capacity of
     *                     the pipe can reach <code>BC+N</code>.
     * @param name The name of the pipe.
     * @param predicate The predicate by which to accept pushed drops into the pipe. Ignored if null.
     */
    public SupplyPipe(int baseCapacity, String name, Predicate<D> predicate) {
        super(baseCapacity, name);
        this.predicate = predicate;
    }

    @Override
    public void push(D drop) throws Exception {
        if (predicate == null || predicate.test(drop))
            super.push(new Drop<>(index.getAndIncrement(), drop));
    }

    @Override
    void push(Drop<D> drop) throws Exception {
        push(drop.drop());
    }
}
