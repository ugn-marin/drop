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
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Various concurrency utilities.
 */
public class Concurrent {
    private static final Lazy<ConcurrentExecutor> physicalExecutor = new Lazy<>(() -> new ConcurrentExecutor(
            Executors.newCachedThreadPool(Concurrent.namedThreadFactory("Physical", true))));
    private static final Lazy<ConcurrentExecutor> virtualExecutor = new Lazy<>(() -> new ConcurrentExecutor(
            Executors.newVirtualThreadPerTaskExecutor()));

    private Concurrent() {}

    /**
     * Returns the internal physical threads concurrent executor.
     */
    public static ConcurrentExecutor physical() {
        return physicalExecutor.get();
    }

    /**
     * Returns the internal virtual threads concurrent executor.
     */
    public static ConcurrentExecutor virtual() {
        return virtualExecutor.get();
    }

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
