package lab.drop.concurrent;

import lab.drop.Sugar;
import lab.drop.functional.UnsafeConsumer;
import lab.drop.functional.UnsafeFunction;
import lab.drop.functional.UnsafeRunnable;
import lab.drop.functional.UnsafeSupplier;

import javax.naming.InterruptedNamingException;
import java.io.InterruptedIOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileLockInterruptionException;
import java.nio.channels.InterruptedByTimeoutException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Utility methods and interfaces for handling interruptible flows, and wrapping interrupted exceptions if required.
 */
public class Interruptible {

    private Interruptible() {}

    /**
     * An unsafe supplier throwing <code>InterruptedException</code>.
     */
    public interface Supplier<O> extends UnsafeSupplier<O> {

        @Override
        O get() throws InterruptedException;
    }

    /**
     * An unsafe consumer throwing <code>InterruptedException</code>.
     */
    public interface Consumer<I> extends UnsafeConsumer<I> {

        @Override
        void accept(I t) throws InterruptedException;
    }

    /**
     * An unsafe function throwing <code>InterruptedException</code>.
     */
    public interface Function<I, O> extends UnsafeFunction<I, O> {

        @Override
        O apply(I t) throws InterruptedException;
    }

    /**
     * A functional runnable throwing <code>InterruptedException</code>.
     */
    public interface Runnable extends UnsafeRunnable {

        @Override
        void run() throws InterruptedException;
    }

    /**
     * Runs the interruptible supplier, wrapping the <code>InterruptedException</code> in an
     * <code>InterruptedRuntimeException</code>.
     * @param supplier The interruptible supplier.
     * @param <O> The supplier output type.
     * @return The supplier output.
     * @throws InterruptedRuntimeException If interrupted.
     */
    public static <O> O get(Supplier<O> supplier) {
        return Objects.requireNonNull(supplier, "Interruptible supplier is null.").toSneakySupplier().get();
    }

    /**
     * Runs the interruptible consumer, wrapping the <code>InterruptedException</code> in an
     * <code>InterruptedRuntimeException</code>.
     * @param consumer The interruptible consumer.
     * @param t The consumer input.
     * @param <I> The consumer input type.
     * @throws InterruptedRuntimeException If interrupted.
     */
    public static <I> void accept(Consumer<I> consumer, I t) {
        Objects.requireNonNull(consumer, "Interruptible consumer is null.").toSneakyConsumer().accept(t);
    }

    /**
     * Runs the interruptible function, wrapping the <code>InterruptedException</code> in an
     * <code>InterruptedRuntimeException</code>.
     * @param function The interruptible function.
     * @param t The function input.
     * @param <I> The function input type.
     * @param <O> The function output type.
     * @return The function output.
     * @throws InterruptedRuntimeException If interrupted.
     */
    public static <I, O> O apply(Function<I, O> function, I t) {
        return Objects.requireNonNull(function, "Interruptible function is null.").toSneakyFunction().apply(t);
    }

    /**
     * Runs the interruptible runnable, wrapping the <code>InterruptedException</code> in an
     * <code>InterruptedRuntimeException</code>.
     * @param runnable The interruptible runnable.
     * @throws InterruptedRuntimeException If interrupted.
     */
    public static void run(Runnable runnable) {
        Objects.requireNonNull(runnable, "Interruptible runnable is null.").toSneakyRunnable().run();
    }

    /**
     * Runs the object's <code>wait</code> method, wrapping the <code>InterruptedException</code> in an
     * <code>InterruptedRuntimeException</code>. Equivalent to:
     * <pre>
     * Interruptible.run(object::wait)
     * </pre>
     * @param object The object.
     * @throws InterruptedRuntimeException If interrupted.
     */
    public static void wait(Object object) {
        run(object::wait);
    }

    /**
     * Runs the object's <code>wait</code> method, wrapping the <code>InterruptedException</code> in an
     * <code>InterruptedRuntimeException</code>. Equivalent to:
     * <pre>
     * Interruptible.accept(object::wait, millis)
     * </pre>
     * @param object The object.
     * @param millis The maximum time to wait in milliseconds.
     * @throws InterruptedRuntimeException If interrupted.
     */
    public static void wait(Object object, long millis) {
        accept(object::wait, millis);
    }

    /**
     * Runs the <code>sleep</code> method, wrapping the <code>InterruptedException</code> in an
     * <code>InterruptedRuntimeException</code>. Equivalent to:
     * <pre>
     * Interruptible.accept(Thread::sleep, millis)
     * </pre>
     * @param millis The length of time to sleep in milliseconds.
     * @throws InterruptedRuntimeException If interrupted.
     */
    public static void sleep(long millis) {
        accept(Thread::sleep, millis);
    }

    /**
     * Runs the <code>join</code> method with the executor service, wrapping the <code>InterruptedException</code> in an
     * <code>InterruptedRuntimeException</code>. Equivalent to:
     * <pre>
     * Interruptible.accept(Concurrent::join, executorService)
     * </pre>
     * @param executorService The executor service.
     * @throws InterruptedRuntimeException If interrupted.
     */
    public static void join(ExecutorService executorService) {
        accept(Concurrent::join, executorService);
    }

    /**
     * Runs the limiter's <code>begin</code> method, wrapping the <code>InterruptedException</code> in an
     * <code>InterruptedRuntimeException</code>. Equivalent to:
     * <pre>
     * Interruptible.run(limiter::begin)
     * </pre>
     * @param limiter The limiter.
     * @throws InterruptedRuntimeException If interrupted.
     */
    public static void begin(Limiter limiter) {
        run(limiter::begin);
    }

    /**
     * Validates the interrupted status of the thread, and throws InterruptedException if set. The interrupted status of
     * the thread is unaffected by this method.
     * @throws InterruptedException If this thread is marked as interrupted.
     */
    public static void validateInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedException("Interruption detected.");
    }

    /**
     * Validates the interrupted status of the thread, and throws InterruptedRuntimeException if set. The interrupted
     * status of the thread is unaffected by this method. Equivalent to:
     * <pre>
     * Interruptible.run(Interruptible::validateInterrupted)
     * </pre>
     * @throws InterruptedRuntimeException If this thread is marked as interrupted.
     */
    public static void validateInterruptedRuntime() {
        run(Interruptible::validateInterrupted);
    }

    /**
     * Returns true if the exception is a result of a thread interruption.
     */
    public static boolean isInterruption(Exception e) {
        return Sugar.instanceOfAny(e, InterruptedException.class, InterruptedRuntimeException.class,
                ClosedByInterruptException.class, InterruptedIOException.class, FileLockInterruptionException.class,
                InterruptedNamingException.class, InterruptedByTimeoutException.class);
    }
}
