package lab.drop.concurrent;

import lab.drop.data.Data;
import lab.drop.functional.Functional;
import lab.drop.functional.Reducer;
import lab.drop.functional.Unsafe;
import lab.drop.functional.UnsafeRunnable;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * An executor service tasks submitting and results handling wrapper.
 */
@FunctionalInterface
public interface ConcurrentExecutor {

    /**
     * Returns the executor service.
     */
    ExecutorService executor();

    /**
     * Submits an unsafe runnable into the cached pool.
     * @param task A task.
     * @return The task's future.
     */
    default Future<Void> run(UnsafeRunnable task) {
        return run(Objects.requireNonNull(task, "Task is null.").toVoidCallable());
    }

    /**
     * Submits a callable into the cached pool.
     * @param task A task.
     * @param <T> The task's result type.
     * @return The task's future.
     */
    @SuppressWarnings("resource")
    default <T> Future<T> run(Callable<T> task) {
        return executor().submit(Objects.requireNonNull(task, "Task is null."));
    }

    /**
     * Submits an unsafe runnable into the cached pool. Returns a supplier of a monadic wrapper of the result.
     * Equivalent to:
     * <pre>
     * Concurrent.monadic(run(task))
     * </pre>
     * @param task A task.
     * @return A supplier of the task's monadic result.
     */
    default Supplier<Unsafe<Void>> monadicRun(UnsafeRunnable task) {
        return Concurrent.monadic(run(task));
    }

    /**
     * Submits a callable into the cached pool. Returns a supplier of a monadic wrapper of the result. Equivalent to:
     * <pre>
     * Concurrent.monadic(run(task))
     * </pre>
     * @param task A task.
     * @param <T> The task's result type.
     * @return A supplier of the task's monadic result.
     */
    default <T> Supplier<Unsafe<T>> monadicRun(Callable<T> task) {
        return Concurrent.monadic(run(task));
    }

    /**
     * Submits a callable into the cached pool. Returns a supplier of the result if succeeded, or else the result of the
     * exception supplier.
     * @param task A task.
     * @param onException A supplier of the result if the task failed.
     * @param <T> The task's result type.
     * @return A supplier of the task's result if returned, or else the supplier result.
     */
    default <T> Supplier<T> orElse(Callable<T> task, Supplier<T> onException) {
        Objects.requireNonNull(onException, "Exception supplier is null.");
        return orElse(task, e -> onException.get());
    }

    /**
     * Submits a callable into the cached pool. Returns a supplier of the result if succeeded, or else the result of the
     * exception function.
     * @param task A task.
     * @param onException A function computing the result if the task failed.
     * @param <T> The task's result type.
     * @return A supplier of the task's result if returned, or else the function result.
     */
    default <T> Supplier<T> orElse(Callable<T> task, Function<Exception, T> onException) {
        Objects.requireNonNull(onException, "Exception function is null.");
        return Functional.map(monadicRun(task), unsafe -> unsafe.orElse(onException));
    }

    /**
     * Returns an unsafe runnable running the provided unsafe runnable tasks in the cached pool. Equivalent to:
     * <pre>
     * () -> run(exceptionsReducer, tasks)
     * </pre>
     * @param exceptionsReducer A reducer of the tasks exceptions list, returning the exception to throw.
     * @param tasks The tasks.
     * @return An unsafe runnable waiting for all tasks completion.
     */
    default UnsafeRunnable merge(Reducer<Exception> exceptionsReducer, UnsafeRunnable... tasks) {
        return () -> run(exceptionsReducer, tasks);
    }

    /**
     * Submits several unsafe runnable tasks into the cached pool, waits for all tasks completion.
     * @param exceptionsReducer A reducer of the tasks exceptions list, returning the exception to throw.
     * @param tasks The tasks.
     */
    default void run(Reducer<Exception> exceptionsReducer, UnsafeRunnable... tasks) throws Exception {
        Concurrent.getAll(exceptionsReducer, Stream.of(Data.requireNoneNull(tasks)).map(this::run)
                .toArray(Future[]::new));
    }
}
