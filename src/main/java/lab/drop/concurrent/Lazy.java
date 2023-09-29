package lab.drop.concurrent;

import lab.drop.flow.Flow;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A lazy supplier, calculating the value if and only if it hasn't been already.
 * @param <T> The value type.
 */
public class Lazy<T> implements Supplier<T> {
    private final Supplier<T> valueSupplier;
    private volatile boolean isCalculated;
    private T value;

    /**
     * Constructs a lazy supplier.
     * @param valueSupplier The value supplier. Will be calculated on the first attempt to get the value.
     */
    public Lazy(Supplier<T> valueSupplier) {
        this.valueSupplier = Objects.requireNonNull(valueSupplier, "Value supplier is null.");
    }

    /**
     * Constructs a lazy supplier.
     * @param callable A callable supplying the value. Will be calculated on the first attempt to get the value.
     * @param onException A function returning a value if the callable throws an exception.
     */
    public Lazy(Callable<T> callable, Function<Exception, T> onException) {
        this(Flow.orElse(callable, onException));
    }

    /**
     * Calculates the value if called for the first time and returns it, else returns the previously calculated value.
     * Upon calculation marks this instance as <i>calculated</i>, unless the calculation fails, in which case the method
     * will continue to fail on subsequent calls until a value is successfully calculated.
     */
    @Override
    public T get() {
        if (!isCalculated) {
            synchronized (valueSupplier) {
                if (!isCalculated) {
                    value = valueSupplier.get();
                    isCalculated = true;
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
     * Performs an operation on the value if and only if calculated.
     * @param consumer The value consumer.
     */
    public void maybe(Consumer<T> consumer) {
        if (isCalculated)
            Objects.requireNonNull(consumer, "Consumer is null.").accept(value);
    }

    /**
     * Returns true if the value has been calculated, else false.
     */
    public boolean isCalculated() {
        return isCalculated;
    }
}
