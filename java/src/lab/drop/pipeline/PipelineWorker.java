package lab.drop.pipeline;

import lab.drop.Sugar;
import lab.drop.concurrent.*;
import lab.drop.flow.OneShot;
import lab.drop.flow.Retry;
import lab.drop.flow.UtilizationCounter;
import lab.drop.function.UnsafeRunnable;
import lab.drop.function.UnsafeSupplier;
import lab.drop.pipeline.monitoring.PipelineWorkerMonitoring;

import java.util.Objects;
import java.util.Optional;
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
    private final Lazy<ExecutorService> executorService;
    private final Lazy<CancellableSubmitter> cancellableSubmitter;
    private final OneShot oneShot = new OneShot();
    private final Latch latch = new Latch();
    private final AtomicInteger cancelledWork = new AtomicInteger();
    private UtilizationCounter utilizationCounter;
    private Retry.Builder retryBuilder;
    private Throwable throwable;
    private PipelineWorkerState state;

    PipelineWorker(boolean internal, int concurrency) {
        this.internal = internal;
        this.concurrency = concurrency;
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
        executorService = new Lazy<>(() -> new BlockingThreadPoolExecutor(concurrency, String.format("PW %d (%s)",
                workerPoolNumber.incrementAndGet(), getName())));
        cancellableSubmitter = new Lazy<>(() -> new CancellableSubmitter(executorService.get()));
        if (!internal)
            utilizationCounter = new UtilizationCounter(concurrency);
        state = PipelineWorkerState.Ready;
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
     * Executes the worker synchronously until all internal work is done, or an exception is thrown.
     * @throws Exception An exception terminating the pipeline. May come from a worker, or the cancel argument.
     */
    @Override
    public void run() throws Exception {
        oneShot.check("The pipeline worker instance cannot be reused.");
        state = PipelineWorkerState.Running;
        var optionalUtilizationCounter = Optional.ofNullable(utilizationCounter);
        Sugar.runSteps(Stream.<UnsafeRunnable>of(
                        () -> optionalUtilizationCounter.ifPresent(UtilizationCounter::start),
                        () -> {
                            work();
                            if (executorService.isCalculated())
                                Concurrent.join(executorService.get());
                        },
                        this::close,
                        this::internalClose,
                        () -> optionalUtilizationCounter.ifPresent(UtilizationCounter::stop),
                        () -> executorService.maybe(ExecutorService::shutdown)).iterator(),
                this::setThrowable);
        boolean silentStop = throwable instanceof SilentStop;
        state = throwable != null ? PipelineWorkerState.Cancelled : PipelineWorkerState.Done;
        latch.release();
        if (!silentStop)
            Sugar.throwIfNonNull(throwable);
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
     * Submits internal work as a cancellable task. Blocked if concurrency level reached. The work execution failure
     * will trigger cancellation of all submitted work and failure of the entire worker.
     * @param work Internal work.
     * @throws InterruptedRuntimeException If interrupted while trying to submit the work.
     */
    void submit(UnsafeRunnable work) {
        cancellableSubmitter.get().submit(() -> {
            Sugar.throwIfNonNull(throwable);
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
        if (cancellableSubmitter.isCalculated())
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
     * Cancels the execution of all internal work, interrupts if possible. Does not wait for work to stop. Cancelling a
     * worker in a pipeline is equivalent to cancelling the pipeline or the worker failing with the provided throwable.
     * @param throwable The throwable for the worker to throw. If null, nothing will be thrown upon stoppage. Note that
     *                  cancelling a worker (not a pipeline) with a null may cause dependent workers and the entire
     *                  pipeline to hang. To stop the pipeline without exception, use the <code>stop</code> method.
     */
    public void cancel(Throwable throwable) {
        setThrowable(throwable);
        executorService.maybe(ExecutorService::shutdown);
        cancellableSubmitter.maybe(cs -> cancelledWork.addAndGet(cs.cancelSubmitted()));
    }

    /**
     * Cancels the execution of all internal work, interrupts if possible. Does not wait for work to stop. The worker
     * will throw an InterruptedException.
     */
    public void interrupt() {
        cancel(new InterruptedException(getName() + " interrupted."));
    }

    /**
     * Returns the total number of tasks that failed, were cancelled after submitting, or interrupted. The full count is
     * only reached after the execution returns or throws an exception.
     */
    public int getCancelledWork() {
        return internal ? 0 : cancelledWork.get();
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
     * Called automatically when the worker is done executing or failed.
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
