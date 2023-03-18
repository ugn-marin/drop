package lab.drop.flow;

import lab.drop.Sugar;
import lab.drop.function.Checked;
import lab.drop.function.Reducer;
import lab.drop.function.UnsafeConsumer;
import lab.drop.function.UnsafeSupplier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A simple object pool.
 * @param <T> The object type.
 */
public class ObjectPool<T> implements UnsafeSupplier<T>, Consumer<T> {
    private final Supplier<Checked<T>> supplier;
    private final Queue<T> objectsQueue = new ConcurrentLinkedQueue<>();

    /**
     * Constructs an empty object pool.
     * @param supplier The objects supplier.
     */
    public ObjectPool(UnsafeSupplier<T> supplier) {
        this(supplier, 0);
    }

    /**
     * Constructs an object pool with an initial set of objects. The pool will attempt to supply the required amount of
     * objects, ignoring checked failures to do so. The ready pool size will be between 0 and the required size,
     * depending on the rate of success. Unchecked exceptions from the supplier will be thrown.
     * @param supplier The objects supplier.
     * @param initialSize The initial count of objects in the pool.
     */
    public ObjectPool(UnsafeSupplier<T> supplier, int initialSize) {
        this.supplier = Checked.supplier(supplier);
        Stream.generate(this.supplier).limit(initialSize).flatMap(Checked::stream).forEach(this);
    }

    /**
     * Gets a used object from the pool, or supplies a new one if the pool is empty.
     * @return The object.
     * @throws Exception If an attempt to supply a new object failed.
     */
    @Override
    public T get() throws Exception {
        return Objects.requireNonNullElse(objectsQueue.poll(), Objects.requireNonNull(supplier.get().unwrap(),
                "Object is null."));
    }

    /**
     * Accepts an object back into the pool.
     * @param object The object.
     */
    @Override
    public void accept(T object) {
        objectsQueue.add(Objects.requireNonNull(object, "Object is null."));
    }

    /**
     * Returns the number of objects in the pool.
     */
    public int size() {
        return objectsQueue.size();
    }

    /**
     * Drains the pool while performing an action on each remaining object.
     * @param action The action to perform on each remaining object.
     * @throws Exception A reduced exception of the action calls.
     */
    public void drain(UnsafeConsumer<T> action) throws Exception {
        drain(action, Reducer.suppressor());
    }

    /**
     * Drains the pool while performing an action on each remaining object.
     * @param action The action to perform on each remaining object.
     * @param exceptionReducer A reducer for the action calls' exceptions.
     * @throws Exception A reduced exception of the action calls.
     */
    public void drain(UnsafeConsumer<T> action, Reducer<Exception> exceptionReducer) throws Exception {
        Objects.requireNonNull(exceptionReducer, "Exception reducer is null.");
        List<Exception> exceptions = new ArrayList<>(size());
        Sugar.acceptWhile(objectsQueue::poll, Objects.requireNonNull(action, "Action is null.")
                .toHandledConsumer(exceptions::add)::accept, Objects::nonNull);
        if (!exceptions.isEmpty())
            Sugar.throwIfNonNull(exceptionReducer.apply(exceptions));
    }

    /**
     * Clears the pool.
     */
    public void clear() {
        objectsQueue.clear();
    }
}
