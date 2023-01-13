package lab.drop.function;

import lab.drop.Sugar;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A consumer allowing exceptions, and is convertible to functional.
 * @param <I> The input type.
 */
@FunctionalInterface
public interface UnsafeConsumer<I> {

    void accept(I t) throws Exception;

    /**
     * Wraps this consumer implementation in a Consumer throwing sneaky.
     */
    default Consumer<I> toSneakyConsumer() {
        return t -> {
            try {
                accept(t);
            } catch (Exception e) {
                throw Sugar.sneaky(e);
            }
        };
    }

    /**
     * Wraps this consumer implementation in a Consumer swallowing the exception.
     */
    default Consumer<I> toSilentConsumer() {
        return toMonadicConsumer()::apply;
    }

    /**
     * Wraps this consumer implementation in a Function returning a monadic wrapper of the result.
     */
    default Function<I, Unsafe<Void>> toMonadicConsumer() {
        return t -> {
            try {
                accept(t);
                return Unsafe.success();
            } catch (Exception e) {
                return Unsafe.failure(e);
            }
        };
    }

    /**
     * Wraps this consumer implementation in a consumer handling exceptions by the provided exception consumer.
     */
    default Consumer<I> toHandledConsumer(Consumer<Exception> exceptionConsumer) {
        Objects.requireNonNull(exceptionConsumer, "Exception consumer is null.");
        return t -> {
            try {
                accept(t);
            } catch (Exception e) {
                exceptionConsumer.accept(e);
            }
        };
    }
}
