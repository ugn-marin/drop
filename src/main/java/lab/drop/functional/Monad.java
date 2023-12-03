package lab.drop.functional;

import lab.drop.flow.Flow;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A monadic wrapper of an unsafe operation result - a value or an exception.
 * @param <T> The expected value type.
 */
public interface Monad<T> {

    T value();

    Exception exception();

    /**
     * Returns true if this is a wrapping of a success result, else false.
     */
    default boolean succeeded() {
        return exception() == null;
    }

    /**
     * Returns true if this is a wrapping of a failure result, else false.
     */
    default boolean failed() {
        return !succeeded();
    }

    /**
     * Converts this wrapping to an Optional of the value, whether wrapping a success or failure result. In other words,
     * the Optional will be empty if this is a wrapping of a failure result, or if the value is null. Equivalent to:
     * <pre>
     * Optional.ofNullable(value())
     * </pre>
     */
    default Optional<T> toOptional() {
        return Optional.ofNullable(value());
    }

    /**
     * Converts this wrapping to a Stream of the value Optional, whether wrapping a success or failure result. In other
     * words, the Stream will be empty if this is a wrapping of a failure result, or if the value is null. Otherwise,
     * returns a Stream of the value. Equivalent to:
     * <pre>
     * toOptional().stream()
     * </pre>
     */
    default Stream<T> stream() {
        return toOptional().stream();
    }

    /**
     * Returns the value if this is a wrapping of a success result, or else other value.
     */
    default T orElse(T other) {
        return orElse(() -> other);
    }

    /**
     * Returns the value if this is a wrapping of a success result, or else the value returned by the supplier.
     */
    default T orElse(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "Supplier is null.");
        return match(Function.identity(), e -> supplier.get());
    }

    /**
     * Returns a value computed according to the monad result.
     * @param success The value function if this is a wrapping of a success result.
     * @param failure The value function if this is a wrapping of a failure result.
     * @param <O> The output value type.
     * @return The output value.
     */
    default <O> O match(Function<T, O> success, Function<Exception, O> failure) {
        Objects.requireNonNull(success, "Success function is null.");
        Objects.requireNonNull(failure, "Failure function is null.");
        return succeeded() ? success.apply(value()) : failure.apply(exception());
    }

    /**
     * Unwraps the monad: Returns the value if this is a wrapping of a success result, or throws the exception if this
     * is a wrapping of a failure result.
     */
    default T unwrap() throws Exception {
        Flow.throwIfNonNull(exception());
        return value();
    }
}
