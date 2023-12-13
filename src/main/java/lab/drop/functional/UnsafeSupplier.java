package lab.drop.functional;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A supplier allowing exceptions, and is convertible to Callable and functional.
 * @param <O> The output type.
 */
@FunctionalInterface
public interface UnsafeSupplier<O> {

    /**
     * A lambda unsafe supplier factory.
     * @param supplier The supplier implementation.
     * @param <O> The output type.
     * @return The UnsafeSupplier.
     */
    static <O> UnsafeSupplier<O> of(UnsafeSupplier<O> supplier) {
        return supplier;
    }

    O get() throws Exception;

    /**
     * Returns a supplier of a value computed by the provided mapper function applied on the value from the supplier.
     * @param mapper The mapper function.
     * @param <T> The output type.
     * @return The new supplier.
     */
    default <T> UnsafeSupplier<T> map(Function<O, T> mapper) {
        Objects.requireNonNull(mapper, "Mapper is null.");
        return () -> mapper.apply(get());
    }

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
     * Returns a supplier of an optional of a nullable value from the provided supplier. Equivalent to:
     * <pre>
     * map(Optional::ofNullable)
     * </pre>
     * @return The supplier of the optional value.
     */
    default UnsafeSupplier<Optional<O>> toOptionalSupplier() {
        return map(Optional::ofNullable);
    }

    /**
     * Wraps this supplier implementation in a Callable.
     */
    default Callable<O> toCallable() {
        return this::get;
    }
}
