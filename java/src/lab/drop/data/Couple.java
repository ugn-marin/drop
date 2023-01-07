package lab.drop.data;

import java.util.Objects;

/**
 * An ordered pair of objects of the same type.
 * @param <T> The objects type.
 */
public abstract class Couple<T> {
    private final T first;
    private final T second;

    /**
     * Constructs a couple.
     * @param first The first object.
     * @param second The second object.
     */
    protected Couple(T first, T second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Returns the first object.
     */
    protected T getFirst() {
        return first;
    }

    /**
     * Returns the second object.
     */
    protected T getSecond() {
        return second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Couple<?> couple = (Couple<?>) o;
        return Objects.equals(first, couple.first) && Objects.equals(second, couple.second);
    }

    /**
     * Indicates whether the provided data equals to the data of the couple.
     */
    public final boolean equals(T first, T second) {
        return Objects.equals(this.first, first) && Objects.equals(this.second, second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public String toString() {
        return "[" + first + ", " + second + "]";
    }
}
