package lab.drop.concurrent;

import lab.drop.Sugar;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A synchronization aid that allows one or more threads to wait until a set of operations being performed in other
 * threads completes, given that the number of registered operations has reached the defined limit. Operations are
 * registered using the {@link #begin} method, and unregistered using the {@link #end} method.
 */
public class Limiter {
    private final int limit;
    private final AtomicInteger blocked = new AtomicInteger();
    private final AtomicInteger executing = new AtomicInteger();

    /**
     * Constructs a limiter.
     * @param limit The limit.
     */
    public Limiter(int limit) {
        this.limit = Sugar.requireRange(limit, 1, null);
    }

    /**
     * Returns the limit.
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Returns the approximate number of threads blocked on the limit.
     */
    public int getBlocked() {
        return blocked.get();
    }

    /**
     * Returns the current registered operations count.
     */
    public int getExecuting() {
        return executing.get();
    }

    /**
     * Increases the count of registered operations if not reached the limit, else causes the current thread to wait for
     * the count to decrease.
     * @throws InterruptedException If interrupted.
     */
    public void begin() throws InterruptedException {
        Interruptible.validateInterrupted();
        blocked.incrementAndGet();
        synchronized (executing) {
            try {
                while (executing.get() == limit)
                    executing.wait();
            } finally {
                blocked.decrementAndGet();
            }
            executing.incrementAndGet();
        }
    }

    /**
     * Decreases the count of registered operations.
     */
    public void end() {
        synchronized (executing) {
            if (executing.decrementAndGet() < 0)
                throw new IllegalStateException("No registered operations to end.");
            executing.notifyAll();
        }
    }
}
