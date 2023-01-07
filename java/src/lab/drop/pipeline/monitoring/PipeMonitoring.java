package lab.drop.pipeline.monitoring;

/**
 * Monitoring data of a pipe.
 */
public interface PipeMonitoring extends PipelineComponentMonitoring {

    /**
     * Returns the base capacity.
     */
    int getBaseCapacity();

    /**
     * Returns the estimated number of drops currently in this pipe, which is the sum of in-order drops, out-of-order
     * drops and in-push drops.
     */
    default int getTotalDrops() {
        return getInOrderDrops() + getOutOfOrderDrops() + getInPushDrops();
    }

    /**
     * Returns the number of drops currently in queue.
     */
    int getInOrderDrops();

    /**
     * Returns the number of drops waiting to be arranged in the queue.
     */
    int getOutOfOrderDrops();

    /**
     * Returns the number of drops currently in <code>push</code>.
     */
    int getInPushDrops();

    /**
     * Returns the number of drops pushed into the queue so far (that passed the in-push and arrangement phases).
     */
    long getDropsPushed();

    /**
     * Returns the load of the pipe at the moment (size out of base capacity), between 0 and 1.
     */
    default double getCurrentLoad() {
        return Math.min((double) getTotalDrops() / getBaseCapacity(), 1);
    }

    /**
     * Returns the average load of the pipe up to this point (average size out of base capacity), between 0 and 1.
     */
    double getAverageLoad();
}
