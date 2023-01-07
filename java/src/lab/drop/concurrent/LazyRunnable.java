package lab.drop.concurrent;

/**
 * A lazy runnable, calculating if and only if it hasn't been already.
 */
public class LazyRunnable implements Runnable {
    private final Lazy<Void> lazy;

    /**
     * Constructs a lazy runnable.
     * @param runnable The runnable. Will be calculated at the first execution.
     */
    public LazyRunnable(Runnable runnable) {
        lazy = new Lazy<>(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Calculates the runnable if called for the first time, else does nothing. Upon calculation marks this instance as
     * <i>calculated</i>, unless the calculation fails, in which case the method will continue to fail on subsequent
     * calls until the runnable is successfully calculated.
     */
    @Override
    public void run() {
        lazy.get();
    }

    /**
     * Returns true if the runnable has been calculated, else false.
     */
    public boolean isCalculated() {
        return lazy.isCalculated();
    }
}
