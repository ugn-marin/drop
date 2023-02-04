package lab.drop.data;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A range of integers.
 */
public final class Range extends Couple<Integer> {

    private Range(int from, int to) {
        super(from, to);
    }

    /**
     * Constructs a range.
     */
    public static Range of(int from, int to) {
        return new Range(from, to);
    }

    /**
     * Returns the <code>from</code> of the range.
     */
    public int getFrom() {
        return getFirst();
    }

    /**
     * Returns the <code>to</code> of the range.
     */
    public int getTo() {
        return getSecond();
    }

    /**
     * Returns <code>to - from</code> (might be negative).
     */
    public int size() {
        return getTo() - getFrom();
    }

    /**
     * Returns true if the range size is zero.
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns the signum function of the range size.
     */
    public int signum() {
        return Integer.signum(size());
    }

    /**
     * Returns true if the value is between <code>from</code> (inclusive) and <code>to</code> (exclusive).
     */
    public boolean contains(int value) {
        return (signum() == 1 && value >= getFrom() && value < getTo()) ||
                (signum() == -1 && value <= getFrom() && value > getTo());
    }

    /**
     * Returns a stream of the range values, from <code>from</code> (inclusive) to <code>to</code> (exclusive). If the
     * range is empty, returns an empty stream.
     */
    public Stream<Integer> stream() {
        return Stream.iterate(getFrom(), i -> i != getTo(), i -> i + signum());
    }

    /**
     * Performs an action for each value in this range, from <code>from</code> (inclusive) to <code>to</code>
     * (exclusive). If the range is empty, does nothing. Equivalent to:
     * <pre>
     * stream().forEach(action)
     * </pre>
     */
    public void forEach(Consumer<Integer> action) {
        Objects.requireNonNull(action, "Action is null.");
        stream().forEach(action);
    }
}
