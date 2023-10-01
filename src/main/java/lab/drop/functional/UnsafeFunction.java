package lab.drop.functional;

import lab.drop.flow.Flow;

import java.util.function.Function;

/**
 * A function allowing exceptions, and is convertible to functional.
 * @param <I> The input type.
 * @param <O> The output type.
 */
@FunctionalInterface
public interface UnsafeFunction<I, O> {

    O apply(I t) throws Exception;

    /**
     * Wraps this function implementation in a Function throwing sneaky.
     */
    default Function<I, O> toSneakyFunction() {
        return t -> {
            try {
                return apply(t);
            } catch (Exception e) {
                throw Flow.sneaky(e);
            }
        };
    }

    /**
     * Wraps this function implementation in a Function returning a monadic wrapper of the result.
     */
    default Function<I, Unsafe<O>> toMonadicFunction() {
        return t -> {
            try {
                return Unsafe.success(apply(t));
            } catch (Exception e) {
                return Unsafe.failure(e);
            }
        };
    }
}
