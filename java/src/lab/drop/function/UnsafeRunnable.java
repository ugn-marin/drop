package lab.drop.function;

import lab.drop.Sugar;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * A runnable allowing exceptions, and is convertible to Runnable, Void Callable and functional.
 */
@FunctionalInterface
public interface UnsafeRunnable {

    void run() throws Exception;

    /**
     * Wraps this runnable implementation in a Runnable throwing sneaky.
     */
    default Runnable toSneakyRunnable() {
        return () -> {
            try {
                run();
            } catch (Exception e) {
                throw Sugar.sneaky(e);
            }
        };
    }

    /**
     * Wraps this runnable implementation in a Runnable swallowing the exception.
     */
    default Runnable toSilentRunnable() {
        return toMonadicRunnable()::get;
    }

    /**
     * Wraps this runnable implementation in a Supplier returning a monadic wrapper of the result.
     */
    default Supplier<Unsafe<Void>> toMonadicRunnable() {
        return Sugar.toMonadicSupplier(toVoidCallable());
    }

    /**
     * Wraps this runnable implementation in a Void Callable.
     */
    default Callable<Void> toVoidCallable() {
        return () -> {
            run();
            return null;
        };
    }
}
