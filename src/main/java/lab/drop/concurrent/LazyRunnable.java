package lab.drop.concurrent;

/**
 * A lazy runnable, computing if and only if it hasn't been already.
 */
public class LazyRunnable implements Runnable {
    private final Lazy<Void> lazy;

    /**
     * Constructs a lazy runnable.
     * @param runnable The runnable. Will be computed at the first execution.
     */
    public LazyRunnable(Runnable runnable) {
        lazy = new Lazy<>(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Computes the runnable if called for the first time, else does nothing. Upon computation marks this instance as
     * <i>computed</i>, unless the computation fails, in which case the method will continue to fail on subsequent calls
     * until the runnable is successfully computed.
     */
    @Override
    public void run() {
        lazy.get();
    }

    /**
     * Returns true if the runnable has been computed, else false.
     */
    public boolean isComputed() {
        return lazy.isComputed();
    }
}
