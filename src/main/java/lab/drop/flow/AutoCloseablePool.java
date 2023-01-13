package lab.drop.flow;

import lab.drop.function.UnsafeSupplier;

/**
 * A simple object pool of auto-closeable objects.
 * @param <T> The auto-closable type.
 */
public class AutoCloseablePool<T extends AutoCloseable> extends ObjectPool<T> implements AutoCloseable {

    /**
     * Constructs an auto-closeable pool.
     * @param supplier The objects supplier.
     */
    public AutoCloseablePool(UnsafeSupplier<T> supplier) {
        super(supplier);
    }

    /**
     * Constructs an object pool with an initial set of objects. The pool will attempt to supply the required amount of
     * objects, ignoring checked failures to do so. The ready pool size will be between 0 and the required size,
     * depending on the rate of success. Unchecked exceptions from the supplier will be thrown.
     * @param supplier The objects supplier.
     * @param initialSize The initial count of objects in the pool.
     */
    public AutoCloseablePool(UnsafeSupplier<T> supplier, int initialSize) {
        super(supplier, initialSize);
    }

    /**
     * Drains the pool while closing each remaining object.
     * @throws Exception A reduced exception of the <code>close</code> calls.
     */
    @Override
    public void close() throws Exception {
        drain(AutoCloseable::close);
    }
}
