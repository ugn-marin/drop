package lab.drop.concurrent;

/**
 * A runtime exception wrapping an <code>InterruptedException</code>, to be used in interruptible flows where it's
 * not desirable or possible to modify the function signature.
 */
public class InterruptedRuntimeException extends RuntimeException {

    public InterruptedRuntimeException(InterruptedException e) {
        super(e);
    }
}
