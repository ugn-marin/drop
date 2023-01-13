package lab.drop.function;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A monadic wrapper of an unsafe operation result - a value or an exception (checked or unchecked).
 * @param value A value if success result.
 * @param exception An exception if failure result.
 * @param <T> The expected value type.
 */
public record Unsafe<T>(T value, Exception exception) implements Monad<T> {

    /**
     * Returns a success wrapping of the value.
     */
    public static <T> Unsafe<T> success(T value) {
        return new Unsafe<>(value, null);
    }

    /**
     * Returns a success wrapping with no value.
     */
    public static Unsafe<Void> success() {
        return new Unsafe<>(null, null);
    }

    /**
     * Returns a failure wrapping of the exception.
     */
    public static <T> Unsafe<T> failure(Exception exception) {
        return new Unsafe<>(null, Objects.requireNonNull(exception, "Exception is null."));
    }

    /**
     * Separates checked exceptions wrapping from unchecked ones. If this is a wrapping of a failure result on a runtime
     * exception, throws the exception. Else, if a wrapping of a success result, or a failure on a checked exception,
     * returns a checked record wrapping the result as is.
     */
    public Checked<T> checked() throws RuntimeException {
        return new Checked<>(this);
    }

    /**
     * Wraps the consumer implementation in a Function returning a monadic wrapper of the result.
     * @param consumer The unsafe consumer.
     * @param <I> The input type.
     * @return The consumer in a monadic form.
     * @apiNote This function is to be used as a short way to create a monadic unsafe consumer from a lambda expression.
     */
    public static <I> Function<I, Unsafe<Void>> consumer(UnsafeConsumer<I> consumer) {
        return Objects.requireNonNull(consumer, "Consumer is null.").toMonadicConsumer();
    }

    /**
     * Wraps the supplier implementation in a Supplier returning a monadic wrapper of the result.
     * @param supplier The unsafe supplier.
     * @param <O> The output type.
     * @return The supplier in a monadic form.
     * @apiNote This function is to be used as a short way to create a monadic unsafe supplier from a lambda expression.
     */
    public static <O> Supplier<Unsafe<O>> supplier(UnsafeSupplier<O> supplier) {
        return Objects.requireNonNull(supplier, "Supplier is null.").toMonadicSupplier();
    }

    /**
     * Wraps the function implementation in a Function returning a monadic wrapper of the result.
     * @param function The unsafe function.
     * @param <I> The input type.
     * @param <O> The output type.
     * @return The function in a monadic form.
     * @apiNote This function is to be used as a short way to create a monadic unsafe function from a lambda expression.
     */
    public static <I, O> Function<I, Unsafe<O>> function(UnsafeFunction<I, O> function) {
        return Objects.requireNonNull(function, "Function is null.").toMonadicFunction();
    }

    /**
     * Wraps the runnable implementation in a Supplier returning a monadic wrapper of the result.
     * @param runnable The unsafe runnable.
     * @return The runnable in a monadic form.
     * @apiNote This function is to be used as a short way to create a monadic unsafe runnable from a lambda expression.
     */
    public static Supplier<Unsafe<Void>> runnable(UnsafeRunnable runnable) {
        return Objects.requireNonNull(runnable, "Runnable is null.").toMonadicRunnable();
    }
}
