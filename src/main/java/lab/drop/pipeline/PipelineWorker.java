package lab.drop.pipeline;

import lab.drop.concurrent.*;
import lab.drop.data.Data;
import lab.drop.flow.Flow;
import lab.drop.flow.OneShot;
import lab.drop.flow.Retry;
import lab.drop.flow.UtilizationCounter;
import lab.drop.functional.Functional;
import lab.drop.functional.UnsafeRunnable;
import lab.drop.functional.UnsafeSupplier;
import lab.drop.pipeline.monitoring.PipelineWorkerMonitoring;
import lab.drop.pipeline.monitoring.PipelineWorkerState;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * An unsafe runnable executing in a pipeline.
 */
public abstract class PipelineWorker implements PipelineWorkerMonitoring, UnsafeRunnable {
    private static final AtomicInteger workerPoolNumber = new AtomicInteger();

    private final boolean internal;
    private final int concurrency;
    private final Lazy<String> simpleName;
    private final Lazy<String> string;
    private final Lazy<String> threadsName;
    private final Lazy<ExecutorService> executorService;
    private final Lazy<CancelableSubmitter> cancelableSubmitter;
    private final Map<String, Integer> threadIndexes;
    private final OneShot oneShot = new OneShot();
    private final Latch latch = new Latch();
    private final AtomicInteger canceledWork = new AtomicInteger();
    private UtilizationCounter utilizationCounter;
    private Retry.Builder retryBuilder;
    private Throwable throwable;
    private PipelineWorkerState state = PipelineWorkerState.Ready;

    PipelineWorker(boolean internal, int concurrency) {
        this.internal = internal;
        this.concurrency = Data.requireRange(concurrency, internal ? null : 1, null);
        simpleName = new Lazy<>(() -> {
            Class<?> clazz = getClass();
            String simpleName = clazz.getSimpleName();
            while (simpleName.isEmpty()) {
                clazz = clazz.getSuperclass();
                simpleName = clazz.getSimpleName();
            }
            if (simpleName.length() > 5 && clazz.getPackage().equals(PipelineWorker.class.getPackage()))
                simpleName = String.valueOf(simpleName.toCharArray()[4]);
            else if (internal)
                simpleName = simpleName.toLowerCase();
            return simpleName;
        });
        string = new Lazy<>(() -> {
            String string = getName();
            if (!internal && concurrency != 1)
                string += String.format("[%d]", concurrency);
            return string;
        });
        threadsName = new Lazy<>(() -> String.format("PW %d (%s)", workerPoolNumber.incrementAndGet(), getName()));
        executorService = new Lazy<>(() -> new BlockingThreadPoolExecutor(concurrency, threadsName.get()));
        cancelableSubmitter = new Lazy<>(() -> new CancelableSubmitter(executorService.get()));
        threadIndexes = new ConcurrentHashMap<>(concurrency);
        if (!internal)
            utilizationCounter = new UtilizationCounter(concurrency);
    }

    /**
     * Returns the name of the worker.
     */
    @Override
    public String getName() {
        return simpleName.get();
    }

    @Override
    public final boolean isInternal() {
        return internal;
    }

    @Override
    public PipelineWorkerState getState() {
        return state;
    }

    @Override
    public int getConcurrency() {
        return concurrency;
    }

    /**
     * Returns the index of the current worker thread - a number between 0 and concurrency - 1. To be used in workers
     * where a thread should be tied to a specific resource between drops.
     * @throws UnsupportedOperationException If called from a thread other than the worker's drop handling thread.
     */
    protected int getThreadIndex() throws UnsupportedOperationException {
        var threadName = Thread.currentThread().getName();
        if (!threadsName.isComputed() || !threadName.startsWith(threadsName.get()))
            throw new UnsupportedOperationException(threadName + " is not a drop handling thread of " + getName() +
                    " - no index can be allocated.");
        return threadIndexes.computeIfAbsent(threadName, t -> threadIndexes.size() % concurrency);
    }

    /**
     * Executes the worker synchronously until all internal work is done, or an exception is thrown.
     * @throws Exception An exception terminating the pipeline. May come from a worker, or the cancel argument. A worker
     * implementation may throw an exception at any stage, including the <code>close</code> function. Any exception that
     * isn't the first to register will be added to the first exception's suppressed exceptions list.
     */
    @Override
    public void run() throws Exception {
        oneShot.check("The pipeline worker instance cannot be reused.");
        state = PipelineWorkerState.Running;
        var optionalUtilizationCounter = Optional.ofNullable(utilizationCounter);
        Flow.runSteps(Stream.of(() -> optionalUtilizationCounter.ifPresent(UtilizationCounter::start),
                        Functional.merge(this::work, () -> Flow.maybe(executorService, Concurrent::join)),
                        () -> state = throwable != null ? PipelineWorkerState.Aborting : PipelineWorkerState.Closing,
                        this::close, this::internalClose,
                        () -> optionalUtilizationCounter.ifPresent(UtilizationCounter::stop),
                        () -> executorService.maybe(ExecutorService::shutdown)).iterator(),
                this::setThrowable);
        state = throwable != null ? PipelineWorkerState.Aborted : PipelineWorkerState.Done;
        latch.release();
        if (!(throwable instanceof SilentStop))
            Flow.throwIfNonNull(throwable);
    }

    /**
     * Causes the current thread to wait until all internal work is done, or an exception is thrown. Returns normally
     * regardless of the result.
     * @throws InterruptedException If current thread was interrupted.
     */
    public void await() throws InterruptedException {
        latch.await();
    }

    /**
     * Submits internal work as a cancelable task. Blocked if concurrency level reached. The work execution failure
     * will trigger cancellation of all submitted work and failure of the entire worker.
     * @param work Internal work.
     * @throws InterruptedRuntimeException If interrupted while trying to submit the work.
     */
    void submit(UnsafeRunnable work) {
        cancelableSubmitter.get().submit(() -> {
            Flow.throwIfNonNull(throwable);
            try {
                return retryBuilder != null ? retryBuilder.build(work).call() : work.toVoidCallable().call();
            } catch (Throwable t) {
                cancel(t);
                throw t;
            }
        });
    }

    /**
     * Executes internal work in a busy context.
     */
    void busyRun(UnsafeRunnable work) throws Exception {
        busyGet(work.toVoidCallable()::call);
    }

    /**
     * Executes internal work in a busy context.
     */
    <T> T busyGet(UnsafeSupplier<T> work) throws Exception {
        try {
            utilizationCounter.busy();
            return work.get();
        } finally {
            utilizationCounter.idle();
        }
    }

    /**
     * Defines retry behavior to all internal work. Call prior to execution only.
     * @param retryBuilder A stateless retry builder. A null builder sets the default behavior of no retries.
     */
    public void setRetryBuilder(Retry.Builder retryBuilder) {
        if (cancelableSubmitter.isComputed())
            throw new IllegalStateException("The pipeline worker is already running.");
        this.retryBuilder = retryBuilder;
    }

    Throwable getThrowable() {
        return throwable;
    }

    private void setThrowable(Throwable throwable) {
        synchronized (executorService) {
            if (this.throwable == null)
                this.throwable = Objects.requireNonNullElse(throwable, new SilentStop());
            else if (throwable != null && !this.throwable.equals(throwable) && !(this.throwable instanceof SilentStop))
                this.throwable.addSuppressed(throwable);
        }
    }

    /**
     * Cancels the execution of all internal work, interrupts if possible. Does not wait for work to stop. Canceling a
     * worker in a pipeline is equivalent to canceling the pipeline or the worker failing with the provided throwable.
     * @param throwable The throwable for the worker to throw. If null, nothing will be thrown upon stoppage. Note that
     *                  canceling a worker (not a pipeline) with a null may cause dependent workers and the entire
     *                  pipeline to hang. To stop the pipeline without exception, use the <code>stop</code> method.
     */
    public void cancel(Throwable throwable) {
        setThrowable(throwable);
        executorService.maybe(ExecutorService::shutdown);
        cancelableSubmitter.maybe(cs -> canceledWork.addAndGet(cs.cancelSubmitted()));
    }

    /**
     * Cancels the execution of all internal work, interrupts if possible. Does not wait for work to stop. The worker
     * will throw an InterruptedException.
     */
    public void interrupt() {
        cancel(new InterruptedException(getName() + " interrupted."));
    }

    /**
     * Returns the total number of tasks that failed, were canceled after submitting, or interrupted. The full count is
     * only reached after the execution returns or throws an exception.
     */
    public int getCanceledWork() {
        return internal ? 0 : canceledWork.get();
    }

    @Override
    public double getCurrentUtilization() {
        return internal ? 0 : utilizationCounter.getCurrentUtilization();
    }

    @Override
    public double getAverageUtilization() {
        return internal ? 0 : utilizationCounter.getAverageUtilization();
    }

    /**
     * Submits all internal work.
     * @throws Exception If interrupted, or internal work failed.
     */
    abstract void work() throws Exception;

    /**
     * Called automatically when the worker is done executing or aborted. The worker state during this method will be
     * either Closing if finished normally, or Aborting if work was canceled, failed or interrupted.
     * @throws Exception A possible exception from the closing logic. Will be thrown by the pipeline if and only if it
     * isn't already in the process of throwing a different exception.
     */
    protected void close() throws Exception {}

    void internalClose() throws Exception {}

    @Override
    public String toString() {
        return string.get();
    }

    private static class SilentStop extends Throwable {}
}
