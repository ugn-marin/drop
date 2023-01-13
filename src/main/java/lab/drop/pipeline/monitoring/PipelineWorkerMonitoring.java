package lab.drop.pipeline.monitoring;

/**
 * Monitoring data of a pipeline worker.
 */
public interface PipelineWorkerMonitoring extends PipelineComponentMonitoring {

    /**
     * Returns true if the worker is an internal pipeline worker, else false.
     */
    boolean isInternal();

    /**
     * Returns the worker state.
     */
    PipelineWorkerState getState();

    /**
     * Returns the defined concurrency level of the worker.
     */
    int getConcurrency();

    /**
     * Returns the threads utilization at the moment.
     */
    double getCurrentUtilization();

    /**
     * Returns the average threads utilization over time up to this point, or while work was being done.
     */
    double getAverageUtilization();
}
