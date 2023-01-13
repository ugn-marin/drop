package lab.drop.flow;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A flow aid for ensuring a single use of a certain code: Calling {@link #check} is allowed only once per instance.
 */
public class OneShot {
    private final AtomicBoolean checked = new AtomicBoolean();

    /**
     * Validates that the method hasn't previously been called on this instance.
     * @param error The exception message in case this is not the first call.
     * @throws IllegalStateException If this is not the first call.
     */
    public void check(String error) {
        if (checked.getAndSet(true))
            throw new IllegalStateException(error);
    }
}
