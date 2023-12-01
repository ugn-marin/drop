package lab.drop.flow;

import lab.drop.functional.Checked;
import lab.drop.functional.UnsafeConsumer;
import lab.drop.functional.UnsafeSupplier;

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
    protected final Queue<T> objectsQueue = new ConcurrentLinkedQueue<>();

    /**
     * Constructs an empty object pool.
     * @param supplier The objects supplier.
     */
    public ObjectPool(UnsafeSupplier<T> supplier) {
        this(supplier, 0);
    }

    /**
     * Constructs an object pool with an initial set of objects. The pool will attempt to supply the required amount of
     * objects, ignoring checked failures to do so. The ready pool size will be between 0 and the required initial size,
     * depending on the rate of success. Unchecked exceptions from the supplier will be thrown.
     * @param supplier The objects supplier.
     * @param initialSize The required initial count of objects in the pool.
     */
    public ObjectPool(UnsafeSupplier<T> supplier, int initialSize) {
        this.supplier = Checked.supplier(supplier);
        Stream.generate(this.supplier).limit(initialSize).flatMap(Checked::stream).filter(Objects::nonNull)
                .forEach(this);
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
     * Clears the pool.
     */
    public void clear() {
        objectsQueue.clear();
    }

    /**
     * A simple pool object usage template: Gets an object from the pool, uses it, and accepts it back into the pool.
     * @param consumer The object consumer.
     * @throws Exception If an attempt to get an object from the pool failed, or if the consumer threw an exception.
     */
    public void use(UnsafeConsumer<T> consumer) throws Exception {
        T object = get();
        try {
            Objects.requireNonNull(consumer, "Consumer is null.").accept(object);
        } finally {
            accept(object);
        }
    }
}
