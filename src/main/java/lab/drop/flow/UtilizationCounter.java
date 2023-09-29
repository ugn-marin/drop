package lab.drop.flow;

import lab.drop.data.Data;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * An accumulative counter for concurrent work utilization over time.
 */
public class UtilizationCounter {
    private final int concurrency;
    private final AtomicBoolean started = new AtomicBoolean();
    private final LongAdder flatTime = new LongAdder();
    private final LongAdder busyTime = new LongAdder();
    private final AtomicInteger currentBusy = new AtomicInteger();
    private long lastCheckpoint;

    /**
     * Constructs a utilization counter.
     * @param concurrency The concurrency level of the measured work.
     */
    public UtilizationCounter(int concurrency) {
        this.concurrency = Data.requirePositive(concurrency);
    }

    /**
     * Returns the utilization at the moment.
     */
    public double getCurrentUtilization() {
        return (double) currentBusy.get() / concurrency;
    }

    /**
     * Returns the average utilization over time up to this point, or while the counter was running.
     */
    public double getAverageUtilization() {
        return (double) busyTime.sum() / (flatTime.sum() * concurrency);
    }

    /**
     * Starts the counter: Defines this moment as the start of the measurement.
     */
    public void start() {
        synchronized (started) {
            if (started.getAndSet(true))
                throw new IllegalStateException("Utilization measurement was already started.");
            timeStep();
        }
    }

    /**
     * Increments the busy work measurement.
     */
    public void busy() {
        addTime(1);
    }

    /**
     * Decrements the busy work measurement.
     */
    public void idle() {
        addTime(-1);
    }

    /**
     * Stops the counter: Defines this moment as the end of the measurement.
     */
    public void stop() {
        synchronized (started) {
            addTime(0);
            started.set(false);
            currentBusy.set(0);
        }
    }

    private void addTime(int change) {
        if (!started.get())
            throw new IllegalStateException("Utilization measurement was stopped or not started.");
        long timeStep = timeStep();
        int wereBusy = Data.requireRange(currentBusy.addAndGet(change), 0, concurrency) - change;
        flatTime.add(timeStep);
        busyTime.add(timeStep * wereBusy);
    }

    private long timeStep() {
        synchronized (currentBusy) {
            long now = System.nanoTime();
            long timeStep = now - lastCheckpoint;
            lastCheckpoint = now;
            return timeStep;
        }
    }
}
