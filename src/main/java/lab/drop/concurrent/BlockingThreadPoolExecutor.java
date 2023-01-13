package lab.drop.concurrent;

import java.util.Objects;
import java.util.concurrent.*;

/**
 * A thread pool executor with a maximum pool size, where reaching that size will get threads trying to submit any more
 * tasks blocked. In other words, this executor limits the size of the blocking queue according to the pool size, but,
 * unlike the default implementation, never rejects any tasks.<br>
 * This is different from using the <code>CallerRunsPolicy</code>, since this pool implementation with a maximum size of
 * 1 still guarantees the order of execution, whereas having a <code>CallerRunsPolicy</code> rejected execution handler
 * does not.<br>
 * In addition, unlike a fixed thread pool, the threads in this pool are terminated if idle for over a minute.<br>
 * Note that task submissions into this pool might throw an <code>InterruptedRuntimeException</code>.
 */
public class BlockingThreadPoolExecutor extends ThreadPoolExecutor {
    private final Limiter limiter;

    /**
     * Constructs a new blocking single-thread pool executor.
     */
    public BlockingThreadPoolExecutor() {
        this(1);
    }

    /**
     * Constructs a new blocking thread pool executor.
     * @param maximumPoolSize The maximum number of threads to allow in the pool.
     */
    public BlockingThreadPoolExecutor(int maximumPoolSize) {
        this(maximumPoolSize, Executors.defaultThreadFactory());
    }

    /**
     * Constructs a new blocking single-thread pool executor.
     * @param name The name for the thread to use.
     */
    public BlockingThreadPoolExecutor(String name) {
        this(1, name);
    }

    /**
     * Constructs a new blocking thread pool executor.
     * @param maximumPoolSize The maximum number of threads to allow in the pool.
     * @param name The name for the named thread factory to use.
     */
    public BlockingThreadPoolExecutor(int maximumPoolSize, String name) {
        this(maximumPoolSize, Concurrent.namedThreadFactory(name, maximumPoolSize > 1));
    }

    /**
     * Constructs a new blocking thread pool executor.
     * @param maximumPoolSize The maximum number of threads to allow in the pool.
     * @param threadFactory The factory to use when creating a new thread.
     */
    public BlockingThreadPoolExecutor(int maximumPoolSize, ThreadFactory threadFactory) {
        super(maximumPoolSize, maximumPoolSize, 1, TimeUnit.MINUTES, new LinkedBlockingQueue<>(), threadFactory);
        allowCoreThreadTimeOut(true);
        limiter = new Limiter(maximumPoolSize);
    }

    /**
     * Returns the number of threads blocked trying to submit new tasks.
     */
    public int getBlocked() {
        return limiter.getBlocked();
    }

    @Override
    public Future<?> submit(Runnable task) throws InterruptedRuntimeException {
        return super.submit(task);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) throws InterruptedRuntimeException {
        return super.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) throws InterruptedRuntimeException {
        return super.submit(task, result);
    }

    @Override
    public void execute(Runnable command) throws InterruptedRuntimeException {
        Objects.requireNonNull(command);
        Interruptible.begin(limiter);
        try {
            super.execute(() -> {
                try {
                    command.run();
                } finally {
                    limiter.end();
                }
            });
        } catch (RejectedExecutionException e) {
            limiter.end();
            throw e;
        }
    }
}
