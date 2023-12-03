package lab.drop.functional;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * A wrapper of an Unsafe monad, only accepting a success result, or a failure on a checked exception.
 * @param unsafe The monadic result.
 * @param <T> The expected value type.
 */
public record Checked<T>(Unsafe<T> unsafe) implements Monad<T> {

    public Checked {
        if (Objects.requireNonNull(unsafe, "Unsafe is null.").exception() instanceof RuntimeException re)
            throw re;
    }

    @Override
    public T value() {
        return unsafe.value();
    }

    @Override
    public Exception exception() {
        return unsafe.exception();
    }

    /**
     * Returns a checked unsafe instance computed according to monad result. If this is a wrapping of a failure result,
     * the exception is preserved as is.
     * @param mapper The value function to apply if this is a wrapping of a success result.
     * @param <O> The output value type.
     * @return The new checked unsafe instance.
     */
    public <O> Checked<O> map(Function<T, O> mapper) {
        return unsafe.map(mapper).checked();
    }

    /**
     * Returns a checked unsafe instance computed according to monad result. If this is a wrapping of a failure result
     * and the failure operator returns a runtime exception, the exception is thrown.
     * @param success The value function to apply if this is a wrapping of a success result.
     * @param failure The exception function to apply if this is a wrapping of a failure result.
     * @param <O> The output value type.
     * @return The new checked unsafe instance.
     */
    public <O> Checked<O> map(Function<T, O> success, UnaryOperator<Exception> failure)  throws RuntimeException {
        return unsafe.map(success, failure).checked();
    }

    /**
     * Wraps the monadic supplier implementation in a Supplier returning a monadic wrapper of the checked result.
     * @param supplier The monadic supplier.
     * @param <O> The output type.
     * @return The supplier in a checked monadic form.
     */
    public static <O> Supplier<Checked<O>> compose(Supplier<Unsafe<O>> supplier) {
        Objects.requireNonNull(supplier, "Supplier is null.");
        return () -> supplier.get().checked();
    }

    /**
     * Wraps the monadic function implementation in a Function returning a monadic wrapper of the checked result.
     * @param function The monadic function.
     * @param <I> The input type.
     * @param <O> The output type.
     * @return The function in a checked monadic form.
     */
    public static <I, O> Function<I, Checked<O>> compose(Function<I, Unsafe<O>> function) {
        return Objects.requireNonNull(function, "Function is null.").andThen(Unsafe::checked);
    }

    /**
     * Wraps the consumer implementation in a Function returning a monadic wrapper of the checked result.
     * @param consumer The unsafe consumer.
     * @param <I> The input type.
     * @return The consumer in a checked monadic form.
     * @apiNote This function is to be used as a short way to create a checked monad consumer from a lambda expression.
     */
    public static <I> Function<I, Checked<Void>> consumer(UnsafeConsumer<I> consumer) {
        return compose(Unsafe.consumer(consumer));
    }

    /**
     * Wraps the supplier implementation in a Supplier returning a monadic wrapper of the checked result.
     * @param supplier The unsafe supplier.
     * @param <O> The output type.
     * @return The supplier in a checked monadic form.
     * @apiNote This function is to be used as a short way to create a checked monad supplier from a lambda expression.
     */
    public static <O> Supplier<Checked<O>> supplier(UnsafeSupplier<O> supplier) {
        return compose(Unsafe.supplier(supplier));
    }

    /**
     * Wraps the function implementation in a Function returning a monadic wrapper of the checked result.
     * @param function The unsafe function.
     * @param <I> The input type.
     * @param <O> The output type.
     * @return The function in a checked monadic form.
     * @apiNote This function is to be used as a short way to create a checked monad function from a lambda expression.
     */
    public static <I, O> Function<I, Checked<O>> function(UnsafeFunction<I, O> function) {
        return compose(Unsafe.function(function));
    }

    /**
     * Wraps the runnable implementation in a Supplier returning a monadic wrapper of the checked result.
     * @param runnable The unsafe runnable.
     * @return The runnable in a checked monadic form.
     * @apiNote This function is to be used as a short way to create a checked monad runnable from a lambda expression.
     */
    public static Supplier<Checked<Void>> runnable(UnsafeRunnable runnable) {
        return compose(Unsafe.runnable(runnable));
    }
}
