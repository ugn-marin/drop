package lab.drop.concurrent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * A wrapper for an executor service, submitting tasks that can all be canceled or interrupted at once.
 */
public class CancelableSubmitter {
    private final ExecutorService executorService;
    private final Map<Callable<?>, Future<?>> submittedFutures = new HashMap<>();

    /**
     * Constructs a cancelable submitter.
     * @param executorService The executor service to submit the tasks to.
     */
    public CancelableSubmitter(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * Submits a task into the executor service.
     * @param task The task to submit.
     * @param <V> The type of the task's result.
     * @return A Future representing pending completion of the task.
     */
    public <V> Future<V> submit(Callable<V> task) {
        Objects.requireNonNull(task, "Task is null.");
        Latch futureLatch = new Latch();
        // Submit a wrapping task
        var future = executorService.submit(() -> {
            try {
                // First make sure future registered
                futureLatch.await();
                // Execute
                return task.call();
            } finally {
                // Unregister the future
                synchronized (submittedFutures) {
                    submittedFutures.remove(task);
                }
            }
        });
        // Register the future for potential cancellation
        synchronized (submittedFutures) {
            submittedFutures.put(task, future);
        }
        // Mark future registered
        futureLatch.release();
        return future;
    }

    /**
     * Attempts to cancel all futures of submitted or executing tasks. Attempts to interrupt executing tasks.
     * @return The number of tasks canceled.
     */
    public int cancelSubmitted() {
        synchronized (submittedFutures) {
            return Concurrent.cancelAll(submittedFutures.values().toArray(Future[]::new));
        }
    }
}
