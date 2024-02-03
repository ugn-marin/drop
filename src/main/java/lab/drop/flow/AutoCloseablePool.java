package lab.drop.flow;

import lab.drop.concurrent.Concurrent;
import lab.drop.data.Data;
import lab.drop.functional.Reducer;
import lab.drop.functional.UnsafeConsumer;
import lab.drop.functional.UnsafeSupplier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple object pool of auto-closeable objects.
 * @param <T> The auto-closable type.
 */
public class AutoCloseablePool<T extends AutoCloseable> extends ObjectPool<T> implements AutoCloseable {
    private final int maximumSize;
    private final long ttl;
    private final Map<T, Long> objectsCreationTime = new ConcurrentHashMap<>();

    /**
     * Constructs an object pool with an initial set of objects. The pool will attempt to supply the required amount of
     * objects, ignoring checked failures to do so. The ready pool size will be between 0 and the required initial size,
     * depending on the rate of success. Unchecked exceptions from the supplier will be thrown.<br>
     * The pool maximum size will be enforced upon accepting objects back into the pool, whereas the time to live will
     * be enforced upon getting objects from the pool. In both cases, objects that are removed from the pool will be
     * closed asynchronously, ignoring potential errors. To catch all such errors or close objects synchronously, these
     * limits can't be used. Instead, you may close the objects manually, or wait for the pool to close all remaining
     * objects upon closing the pool itself, which does throw an exception if any of the close calls failed.
     * @param supplier The objects supplier.
     * @param initialSize The initial count of objects in the pool.
     * @param maximumSize The maximum count of objects in the pool. Must be positive and at least as large as the
     *                    initial size.
     * @param ttl Objects' time to live in milliseconds. Non-positive means no limit.
     */
    public AutoCloseablePool(UnsafeSupplier<T> supplier, int initialSize, int maximumSize, long ttl) {
        super(supplier, initialSize);
        this.maximumSize = Data.requireRange(maximumSize, Math.max(initialSize, 1), null);
        this.ttl = ttl;
    }

    /**
     * Gets a used object from the pool, or supplies a new one if the pool is empty. Removes unused objects that
     * exceeded their time to live, and closes them asynchronously.
     * @return The object.
     * @throws Exception If an attempt to supply a new object failed.
     */
    @Override
    public T get() throws Exception {
        return Flow.getWhile(super::get, this::removeIfOutdated);
    }

    /**
     * Accepts an object back into the pool. Removes an unused object if the pool is full, and closes it asynchronously.
     * @param object The object.
     */
    @Override
    public void accept(T object) {
        Flow.whileTrue(() -> maximumSize > 0 && size() >= maximumSize && remove(objectsQueue.poll()));
        super.accept(object);
    }

    private boolean removeIfOutdated(T object) {
        if (ttl > 0 && System.currentTimeMillis() - objectsCreationTime.computeIfAbsent(object,
                any -> System.currentTimeMillis()) > ttl)
            return remove(object);
        return false;
    }

    private boolean remove(T object) {
        if (object == null)
            return false;
        objectsCreationTime.remove(object);
        Concurrent.virtual().run(object::close);
        return true;
    }

    /**
     * Drains the pool while closing each remaining object.
     * @throws Exception A reduced exception of the <code>close</code> calls that failed.
     */
    @Override
    public void close() throws Exception {
        List<Exception> exceptions = new ArrayList<>(size());
        UnsafeConsumer<T> action = object -> {
            objectsCreationTime.remove(object);
            object.close();
        };
        Flow.acceptWhile(objectsQueue::poll, action.toHandledConsumer(exceptions::add)::accept, Objects::nonNull);
        Flow.throwIfNonNull(Reducer.suppressor().apply(exceptions));
    }

    /**
     * Has no effect if the pool is already closed, otherwise throws an <code>UnsupportedOperationException</code>.
     */
    @Override
    public void clear() {
        if (size() > 0)
            throw new UnsupportedOperationException("An auto-closeable pool cannot be cleared.");
    }
}
