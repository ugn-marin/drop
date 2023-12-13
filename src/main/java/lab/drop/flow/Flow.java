package lab.drop.flow;

import lab.drop.concurrent.InterruptedRuntimeException;
import lab.drop.concurrent.Lazy;
import lab.drop.data.Range;
import lab.drop.functional.Functional;
import lab.drop.functional.Reducer;
import lab.drop.functional.UnsafeConsumer;
import lab.drop.functional.UnsafeRunnable;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Various flow utilities.
 */
public class Flow {

    private Flow() {}

    /**
     * Returns a supplier calling the callable and returning the result, or a function result if thrown an exception.
     * @param callable The callable.
     * @param onException A function returning a result on the callable exception.
     * @param <T> The callable return type.
     * @return A supplier of the callable result if returned, or else the function result.
     */
    public static <T> Supplier<T> orElse(Callable<T> callable, Function<Exception, T> onException) {
        Objects.requireNonNull(callable, "Callable is null.");
        Objects.requireNonNull(onException, "Exception function is null.");
        return () -> {
            try {
                return callable.call();
            } catch (Exception e) {
                return onException.apply(e);
            }
        };
    }

    /**
     * Returns a supplier calling the callable and testing its result for success.
     * @param callable The callable.
     * @param success A predicate testing the result for success.
     * @param <T> The callable return type.
     * @return A supplier of the success test result. False might mean a false test result, failure of the callable, or
     * failure of the predicate.
     */
    public static <T> Supplier<Boolean> success(Callable<T> callable, Predicate<T> success) {
        Objects.requireNonNull(callable, "Callable is null.");
        Objects.requireNonNull(success, "Success predicate is null.");
        return orElse(() -> success.test(callable.call()), e -> false);
    }

    /**
     * Iterates from 0 (inclusive) to <code>times</code> (exclusive).
     * @param times The iteration range.
     * @param consumer The consumer.
     */
    public static void iterate(int times, Consumer<Integer> consumer) {
        Range.of(0, times).forEach(consumer);
    }

    /**
     * Repeats a runnable call forever (or until throws a runtime exception or error).
     * @param runnable The runnable.
     */
    public static void forever(Runnable runnable) {
        Objects.requireNonNull(runnable, "Runnable is null.");
        whileTrue(() -> {
            runnable.run();
            return true;
        });
    }

    /**
     * Repeats a supplier call as long as it returns true.
     * @param supplier The supplier.
     */
    @SuppressWarnings("StatementWithEmptyBody")
    public static void whileTrue(Supplier<Boolean> supplier) {
        Objects.requireNonNull(supplier, "Supplier is null.");
        while (supplier.get());
    }

    /**
     * Produces values from the callable as long as they pass the predicate.
     * @param callable The callable producing the values.
     * @param predicate The predicate testing the values.
     * @param <T> The values type.
     * @return The first value that failed the predicate.
     * @throws Exception Any exception thrown by the implementations.
     */
    public static <T> T getWhile(Callable<T> callable, Predicate<T> predicate) throws Exception {
        Objects.requireNonNull(predicate, "Predicate is null.");
        T value = Objects.requireNonNull(callable, "Callable is null.").call();
        while (predicate.test(value))
            value = callable.call();
        return value;
    }

    /**
     * Produces optional values from the callable as long as they are not present.
     * @param callable The callable producing the optional values.
     * @param <T> The values type.
     * @return The first present optional.
     * @throws Exception Any exception thrown by the callable.
     */
    public static <T> T getWhileNotPresent(Callable<Optional<T>> callable) throws Exception {
        return getWhile(callable, Predicate.not(Optional::isPresent)).orElseThrow();
    }

    /**
     * Produces and accepts values into the consumer as long as they pass the predicate.
     * @param callable The callable producing the values.
     * @param consumer The values consumer.
     * @param predicate The predicate testing the values.
     * @param <T> The values type.
     * @throws Exception Any exception thrown by the implementations.
     */
    public static <T> void acceptWhile(Callable<T> callable, UnsafeConsumer<T> consumer, Predicate<T> predicate)
            throws Exception {
        Objects.requireNonNull(consumer, "Consumer is null.");
        Objects.requireNonNull(predicate, "Predicate is null.");
        T value = Objects.requireNonNull(callable, "Callable is null.").call();
        while (predicate.test(value)) {
            consumer.accept(value);
            value = callable.call();
        }
    }

    /**
     * Produces and accepts optional values into the consumer until empty.
     * @param callable The callable producing the optional values.
     * @param consumer The values consumer.
     * @param <T> The values type.
     * @throws Exception Any exception thrown by the implementations.
     */
    public static <T> void acceptWhilePresent(Callable<Optional<T>> callable, UnsafeConsumer<T> consumer)
            throws Exception {
        Objects.requireNonNull(consumer, "Consumer is null.");
        acceptWhile(callable, optional -> consumer.accept(optional.orElseThrow()), Optional::isPresent);
    }

    /**
     * Accepts the stream elements into the unsafe consumer. Converts the stream to sequential if isn't.
     * @param stream The stream.
     * @param consumer The consumer.
     * @param <T> The elements type.
     * @throws Exception Any exception thrown by the consumer.
     */
    public static <T> void forEach(Stream<T> stream, UnsafeConsumer<T> consumer) throws Exception {
        Objects.requireNonNull(consumer, "Consumer is null.");
        var iterator = Objects.requireNonNull(stream, "Stream is null.").sequential().iterator();
        acceptWhilePresent(() -> Functional.next(iterator), consumer);
    }

    /**
     * Returns the exception as is if runtime exception, or wrapped in a new UndeclaredThrowableException otherwise.
     */
    public static RuntimeException sneaky(Exception e) {
        Objects.requireNonNull(e, "Exception is null.");
        if (e instanceof RuntimeException re)
            return re;
        else if (e instanceof InterruptedException ie)
            return new InterruptedRuntimeException(ie);
        return new UndeclaredThrowableException(e);
    }

    /**
     * Returns the throwable as an exception.
     * @param throwable A throwable.
     * @return The throwable if an exception or null, or wrapped in a new UndeclaredThrowableException otherwise.
     */
    public static Exception toException(Throwable throwable) {
        if (throwable instanceof Exception e)
            return e;
        return throwable == null ? null : new UndeclaredThrowableException(throwable);
    }

    /**
     * Throws the throwable as an exception, or as Error if is an Error.
     * @param throwable A throwable.
     * @throws Exception The throwable if not null, thrown as is if instance of Exception or Error, or wrapped in a new
     * UndeclaredThrowableException otherwise.
     */
    public static void throwIfNonNull(Throwable throwable) throws Exception {
        if (throwable == null)
            return;
        if (throwable instanceof Error e)
            throw e;
        throw toException(throwable);
    }

    /**
     * Throws the exception reduced from the list, if any.
     * @param exceptionsReducer The exceptions reducer.
     * @param exceptions A lazy list of exceptions.
     * @throws Exception The reduced exception.
     */
    public static void throwIfAny(Reducer<Exception> exceptionsReducer, Lazy<List<Exception>> exceptions)
            throws Exception {
        Objects.requireNonNull(exceptionsReducer, "Reducer is null.");
        maybe(exceptions, list -> throwIfNonNull(exceptionsReducer.apply(list)));
    }

    /**
     * Performs an unsafe operation on the lazy value if and only if computed.
     * @param lazy The lazy value supplier.
     * @param consumer The value consumer.
     * @param <T> The value type.
     * @throws Exception The consumer exception.
     */
    public static <T> void maybe(Lazy<T> lazy, UnsafeConsumer<T> consumer) throws Exception {
        Objects.requireNonNull(consumer, "Consumer is null.");
        if (Objects.requireNonNull(lazy, "Lazy is null.").isComputed())
            consumer.accept(lazy.get());
    }

    /**
     * Runs the provided unsafe runnable steps with guaranteed execution: For each step, subsequent steps are executed
     * in the <code>finally</code> block. Throwables are accepted by the throwable consumer.
     * @param steps The steps.
     * @param throwableConsumer The consumer of the steps' throwables.
     */
    public static void runSteps(Iterator<UnsafeRunnable> steps, Consumer<Throwable> throwableConsumer) {
        Objects.requireNonNull(steps, "Steps iterator is null.");
        Objects.requireNonNull(throwableConsumer, "Throwable consumer is null.");
        if (!steps.hasNext()) {
            return;
        }
        try {
            Objects.requireNonNull(steps.next(), "Step runnable is null.").run();
        } catch (Throwable t) {
            throwableConsumer.accept(t);
        } finally {
            runSteps(steps, throwableConsumer);
        }
    }

    /**
     * Runs the provided unsafe runnable steps with guaranteed execution: For each step, subsequent steps are executed
     * in the <code>finally</code> block. Throwables are thrown if Error, otherwise reduced by the provided reducer.
     * @param steps The steps.
     * @param exceptionsReducer The exceptions reducer for all throwables except Errors.
     * @throws Exception The reduced exception if any step(s) failed.
     */
    public static void runSteps(Iterator<UnsafeRunnable> steps, Reducer<Exception> exceptionsReducer) throws Exception {
        Lazy<List<Exception>> exceptions = new Lazy<>(ArrayList::new);
        Objects.requireNonNull(exceptionsReducer, "Reducer is null.");
        runSteps(steps, throwable -> {
            if (throwable instanceof Error e)
                throw e;
            exceptions.get().add(toException(throwable));
        });
        throwIfAny(exceptionsReducer, exceptions);
    }
}
