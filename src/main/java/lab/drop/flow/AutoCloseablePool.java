package lab.drop.flow;

import lab.drop.concurrent.Concurrent;
import lab.drop.data.Data;
import lab.drop.functional.Reducer;
import lab.drop.functional.UnsafeConsumer;
import lab.drop.functional.UnsafeSupplier;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
     * objects, ignoring checked failures to do so. The ready pool size will be between 0 and the required size,
     * depending on the rate of success. Unchecked exceptions from the supplier will be thrown.
     * @param supplier The objects supplier.
     * @param initialSize The initial count of objects in the pool.
     * @param maximumSize The maximum count of objects in the pool.
     * @param ttl Objects' time to live in milliseconds. Non-positive means no limit.
     */
    public AutoCloseablePool(UnsafeSupplier<T> supplier, int initialSize, int maximumSize, long ttl) {
        super(supplier, initialSize);
        this.maximumSize = Data.requireRange(maximumSize, initialSize, null);
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
        var outdated = removeOutdated();
        return Flow.getWhileNotPresent(() -> {
            T object = super.get();
            if (outdated.isEmpty())
                return Optional.of(object);
            if (outdated.get().contains(object)) {
                objectsQueue.remove(object);
                return Optional.empty();
            }
            objectsCreationTime.computeIfAbsent(object, any -> System.currentTimeMillis());
            return Optional.of(object);
        });
    }

    /**
     * Accepts an object back into the pool. Removes unused objects if the pool is full, and closes them asynchronously.
     * @param object The object.
     */
    @Override
    public void accept(T object) {
        if (objectsCreationTime != null) {
            synchronized (objectsCreationTime) {
                while (size() >= maximumSize)
                    remove(objectsQueue.poll());
            }
        }
        super.accept(object);
    }

    private Optional<Set<T>> removeOutdated() {
        if (ttl <= 0)
            return Optional.empty();
        synchronized (objectsCreationTime) {
            return Optional.of(Map.copyOf(objectsCreationTime).entrySet().stream()
                    .filter(entry -> System.currentTimeMillis() - entry.getValue() > ttl).map(Map.Entry::getKey)
                    .filter(objectsQueue::contains).peek(this::remove).collect(Collectors.toSet()));
        }
    }

    private void remove(T object) {
        objectsCreationTime.remove(object);
        objectsQueue.remove(object);
        Concurrent.run(object::close);
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
