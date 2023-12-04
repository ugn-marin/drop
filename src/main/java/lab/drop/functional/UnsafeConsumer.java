package lab.drop.functional;

import lab.drop.flow.Flow;

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
        return t -> Functional.sneaky(() -> accept(t));
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
        return t -> Flow.orElse(() -> {
            accept(t);
            return Unsafe.success();
        }, Unsafe::<Void>failure).get();
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
