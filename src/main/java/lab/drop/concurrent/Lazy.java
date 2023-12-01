package lab.drop.concurrent;

import lab.drop.flow.Flow;
import lab.drop.functional.Functional;
import lab.drop.functional.Unsafe;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A lazy supplier, computing the value if and only if it hasn't been already.
 * @param <T> The value type.
 */
public class Lazy<T> implements Supplier<T> {
    private final Supplier<T> valueSupplier;
    private volatile boolean isComputed;
    private T value;

    /**
     * Constructs a lazy supplier.
     * @param valueSupplier The value supplier. Will be computed on the first attempt to get the value.
     */
    public Lazy(Supplier<T> valueSupplier) {
        this.valueSupplier = Objects.requireNonNull(valueSupplier, "Value supplier is null.");
    }

    /**
     * Constructs a lazy supplier.
     * @param callable A callable supplying the value. Will be computed on the first attempt to get the value.
     * @param onException A function returning a value if the callable throws an exception.
     */
    public Lazy(Callable<T> callable, Function<Exception, T> onException) {
        this(Flow.orElse(callable, onException));
    }

    /**
     * Returns a lazy supplier returning a monadic wrapper of the result of the given callable.
     * @param callable A callable supplying the value. Will be computed on the first attempt to get the value.
     * @param <T> The value type.
     * @return The new lazy supplier.
     */
    public static <T> Lazy<Unsafe<T>> monadic(Callable<T> callable) {
        return new Lazy<>(Functional.toMonadicSupplier(callable));
    }

    /**
     * Computes the value if called for the first time and returns it, else returns the previously computed value. Upon
     * computation marks this instance as <i>computed</i>, unless the computation fails, in which case the method will
     * continue to fail on subsequent calls until a value is successfully computed.
     */
    @Override
    public T get() {
        if (!isComputed) {
            synchronized (valueSupplier) {
                if (!isComputed) {
                    value = valueSupplier.get();
                    isComputed = true;
                }
            }
        }
        return value;
    }

    /**
     * Returns a lazy supplier of the result of applying the given function to the element of this lazy supplier.
     * @param function The function to apply.
     * @param <O> The output type.
     * @return The new lazy supplier.
     */
    public <O> Lazy<O> map(Function<T, O> function) {
        Objects.requireNonNull(function, "Function is null.");
        return new Lazy<>(() -> function.apply(get()));
    }

    /**
     * Performs an operation on the value if and only if computed.
     * @param consumer The value consumer.
     */
    public void maybe(Consumer<T> consumer) {
        if (isComputed)
            Objects.requireNonNull(consumer, "Consumer is null.").accept(value);
    }

    /**
     * Returns true if the value has been computed, else false.
     */
    public boolean isComputed() {
        return isComputed;
    }
}
