package lab.drop.concurrent;

import lab.drop.data.Data;
import lab.drop.flow.Flow;
import lab.drop.functional.Functional;
import lab.drop.functional.Reducer;
import lab.drop.functional.Unsafe;
import lab.drop.functional.UnsafeRunnable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Various concurrency utilities.
 */
public class Concurrent {
    private static final Lazy<ExecutorService> cachedPool = new Lazy<>(() -> Executors.newCachedThreadPool(
            namedThreadFactory(Concurrent.class.getSimpleName(), true)));

    private Concurrent() {}

    /**
     * Creates a concurrent set.
     */
    public static <T> Set<T> set() {
        return ConcurrentHashMap.newKeySet();
    }

    /**
     * Creates a copy-or-write array list.
     * @param elements Optional initial elements.
     * @param <T> The elements type.
     * @return The list.
     */
    @SafeVarargs
    public static <T> List<T> list(T... elements) {
        return new CopyOnWriteArrayList<>(elements);
    }

    /**
     * Submits an unsafe runnable into the cached pool.
     * @param task A task.
     * @return The task's future.
     */
    public static Future<Void> run(UnsafeRunnable task) {
        Objects.requireNonNull(task, "Task is null.");
        return run(task.toVoidCallable());
    }

    /**
     * Submits a callable into the cached pool.
     * @param task A task.
     * @param <T> The task's result type.
     * @return The task's future.
     */
    public static <T> Future<T> run(Callable<T> task) {
        Objects.requireNonNull(task, "Task is null.");
        return cachedPool.get().submit(task);
    }

    /**
     * Submits an unsafe runnable into the cached pool. Returns a supplier of a monadic wrapper of the result.
     * Equivalent to:
     * <pre>
     * Concurrent.monadic(Concurrent.run(task))
     * </pre>
     * @param task A task.
     * @return A supplier of the task's monadic result.
     */
    public static Supplier<Unsafe<Void>> monadicRun(UnsafeRunnable task) {
        return monadic(run(task));
    }

    /**
     * Submits a callable into the cached pool. Returns a supplier of a monadic wrapper of the result. Equivalent to:
     * <pre>
     * Concurrent.monadic(Concurrent.run(task))
     * </pre>
     * @param task A task.
     * @param <T> The task's result type.
     * @return A supplier of the task's monadic result.
     */
    public static <T> Supplier<Unsafe<T>> monadicRun(Callable<T> task) {
        return monadic(run(task));
    }

    /**
     * Wraps the future <code>get</code> call in a Supplier returning a monadic wrapper of the result. Equivalent to:
     * <pre>
     * Functional.toMonadicSupplier(future::get)
     * </pre>
     * @param future A future.
     * @param <T> The future's result type.
     * @return A supplier of the future's result.
     */
    public static <T> Supplier<Unsafe<T>> monadic(Future<T> future) {
        Objects.requireNonNull(future, "Future is null.");
        return Functional.toMonadicSupplier(future::get);
    }

    /**
     * Submits a callable into the cached pool. Returns a supplier of the result if succeeded, or else the result of the
     * exception supplier.
     * @param task A task.
     * @param onException A supplier of the result if the task failed.
     * @param <T> The task's result type.
     * @return A supplier of the task's result if returned, or else the supplier result.
     */
    public static <T> Supplier<T> orElse(Callable<T> task, Supplier<T> onException) {
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
    public static <T> Supplier<T> orElse(Callable<T> task, Function<Exception, T> onException) {
        Objects.requireNonNull(onException, "Exception function is null.");
        return Functional.map(monadicRun(task), unsafe -> unsafe.orElse(onException));
    }

    /**
     * Returns an unsafe runnable running the provided unsafe runnable tasks in the cached pool. Equivalent to:
     * <pre>
     * () -> Concurrent.run(exceptionsReducer, tasks)
     * </pre>
     * @param exceptionsReducer A reducer of the tasks exceptions list, returning the exception to throw.
     * @param tasks The tasks.
     * @return An unsafe runnable waiting for all tasks completion.
     */
    public static UnsafeRunnable merge(Reducer<Exception> exceptionsReducer, UnsafeRunnable... tasks) {
        return () -> run(exceptionsReducer, tasks);
    }

    /**
     * Submits several unsafe runnable tasks into the cached pool, waits for all tasks completion.
     * @param exceptionsReducer A reducer of the tasks exceptions list, returning the exception to throw.
     * @param tasks The tasks.
     */
    public static void run(Reducer<Exception> exceptionsReducer, UnsafeRunnable... tasks) throws Exception {
        getAll(exceptionsReducer, Stream.of(Data.requireNoneNull(tasks)).map(Concurrent::run).toArray(Future[]::new));
    }

    /**
     * Calls <code>get</code> for each future. In other words, waits if necessary for all futures' tasks completion.
     * @param exceptionsReducer A reducer of the futures exceptions list, returning the exception to throw.
     * @param futures The futures.
     */
    public static void getAll(Reducer<Exception> exceptionsReducer, Future<?>... futures) throws Exception {
        Flow.runSteps(Stream.of(Data.requireNoneNull(futures)).map(future -> (UnsafeRunnable) future::get).iterator(),
                exceptionsReducer);
    }

    /**
     * Cancels all futures with interruption. Returns the number of futures that were canceled.
     * @param futures The futures.
     * @return The number of futures that were canceled.
     */
    public static int cancelAll(Future<?>... futures) {
        return (int) Stream.of(Data.requireNoneNull(futures)).map(future -> future.cancel(true))
                .filter(Functional::is).count();
    }

    /**
     * Shuts the executor service down and awaits termination indefinitely.
     * @param executorService The executor service.
     * @throws InterruptedException If interrupted.
     */
    public static void join(ExecutorService executorService) throws InterruptedException {
        Objects.requireNonNull(executorService, "Executor service is null.").shutdown();
        if (!executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS))
            throw new InterruptedException();
    }

    /**
     * Constructs a thread factory naming the threads as name and thread number if required: <code>"name #"</code>.<br>
     * The enumeration start from 1. Without enumeration, all threads produced by the factory get the same name.
     * @param name The name.
     * @param enumerate Whether to enumerate the produced threads. Generally it's recommended to pass true if the thread
     *                  factory is intended to be used for multiple threads, and false if intended for a single thread
     *                  executor.
     * @return The thread factory.
     */
    public static ThreadFactory namedThreadFactory(String name, boolean enumerate) {
        return new NamedThreadFactory(name, enumerate);
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private final String name;
        private AtomicInteger threadNumber;

        NamedThreadFactory(String name, boolean enumerate) {
            this.name = Objects.requireNonNull(name, "Name is null.");
            if (enumerate)
                threadNumber = new AtomicInteger();
        }

        @Override
        public Thread newThread(Runnable task) {
            String threadName = name;
            if (threadNumber != null)
                threadName += " " + threadNumber.incrementAndGet();
            var thread = new Thread(task, threadName);
            if (thread.isDaemon())
                thread.setDaemon(false);
            if (thread.getPriority() != Thread.NORM_PRIORITY)
                thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    }
}
