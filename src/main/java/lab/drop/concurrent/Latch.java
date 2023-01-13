package lab.drop.concurrent;

import java.util.concurrent.CountDownLatch;

/**
 * A synchronization aid that allows one or more threads to wait until the {@link #release} method is called. Equivalent
 * to a <code>CountDownLatch</code> with a count of 1.
 */
public class Latch {
    private final CountDownLatch countDownLatch;

    /**
     * Constructs a latch.
     */
    public Latch() {
        countDownLatch = new CountDownLatch(1);
    }

    /**
     * Causes the current thread to wait until the latch is released.
     * @throws InterruptedException If interrupted.
     */
    public void await() throws InterruptedException {
        countDownLatch.await();
    }

    /**
     * Releases all waiting threads.
     */
    public void release() {
        countDownLatch.countDown();
    }

    /**
     * Returns true if the latch has been released, else false.
     */
    public boolean isReleased() {
        return countDownLatch.getCount() == 0;
    }
}
