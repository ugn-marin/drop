package lab.drop.pipeline.monitoring;

/**
 * Represents the runtime state of the worker.
 */
public enum PipelineWorkerState {
    /**
     * Not started yet.
     */
    Ready,
    /**
     * Started: Doing work, waiting for input, waiting for internal work etc.<br>
     * Note: When a worker is done or canceled in any way, it will still appear as <code>Running</code> during
     * <code>close</code>.
     */
    Running,
    /**
     * All internal work is done, and the worker is still closing.
     */
    Closing,
    /**
     * The worker failed or was canceled, and is still closing.
     */
    Canceling,
    /**
     * All internal work is done, and the worker finished closing.
     */
    Done,
    /**
     * The worker failed or was canceled, and finished closing (or closing failed).
     */
    Canceled
}
