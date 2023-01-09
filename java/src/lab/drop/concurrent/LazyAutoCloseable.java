package lab.drop.concurrent;

import lab.drop.Sugar;

import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A lazy auto-closeable supplier, calculating the value if and only if it hasn't been already, and closing it if and
 * only if calculated.
 * @param <T> The value type.
 */
public class LazyAutoCloseable<T extends AutoCloseable> extends Lazy<T> implements AutoCloseable {

    /**
     * Constructs a lazy auto-closeable supplier.
     * @param valueSupplier The value supplier. Will be calculated on the first attempt to get the value.
     */
    public LazyAutoCloseable(Supplier<T> valueSupplier) {
        super(valueSupplier);
    }

    /**
     * Constructs a lazy auto-closeable supplier.
     * @param callable A callable supplying the value. Will be calculated on the first attempt to get the value.
     * @param onException A function returning a value if the callable throws an exception.
     */
    public LazyAutoCloseable(Callable<T> callable, Function<Exception, T> onException) {
        super(callable, onException);
    }

    /**
     * Closes the value if and only if calculated.
     */
    @Override
    public void close() throws Exception {
        Sugar.maybe(this, AutoCloseable::close);
    }
}
