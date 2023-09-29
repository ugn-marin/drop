package lab.drop.functional;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * A supplier allowing exceptions, and is convertible to Callable and functional.
 * @param <O> The output type.
 */
@FunctionalInterface
public interface UnsafeSupplier<O> {

    O get() throws Exception;

    /**
     * Wraps this supplier implementation in a Supplier throwing sneaky.
     */
    default Supplier<O> toSneakySupplier() {
        return Functional.toSneakySupplier(toCallable());
    }

    /**
     * Wraps this supplier implementation in a Supplier returning a monadic wrapper of the result.
     */
    default Supplier<Unsafe<O>> toMonadicSupplier() {
        return Functional.toMonadicSupplier(toCallable());
    }

    /**
     * Wraps this supplier implementation in a Callable.
     */
    default Callable<O> toCallable() {
        return this::get;
    }
}
