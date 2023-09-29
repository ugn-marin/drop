package lab.drop.data;

import lab.drop.flow.Flow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Various data utilities.
 */
public class Data {

    private Data() {}

    /**
     * Validates that the value is within the range. Only validated against the non-null range arguments.
     * @param value The number value.
     * @param min The range minimum allowed (optional).
     * @param max The range maximum allowed (optional).
     * @param <N> The number type.
     * @return The value if in range.
     * @throws NullPointerException If the value is null.
     * @throws IllegalArgumentException If the value is not in range.
     */
    public static <N extends Number> N requireRange(N value, N min, N max) {
        Objects.requireNonNull(value, "Value is null.");
        if (min != null && value.doubleValue() < min.doubleValue())
            throw new IllegalArgumentException(value + " is smaller than the minimum " + min + ".");
        if (max != null && value.doubleValue() > max.doubleValue())
            throw new IllegalArgumentException(value + " is greater than the maximum " + max + ".");
        return value;
    }

    /**
     * Validates that the value is a positive integer.
     */
    public static int requirePositive(int value) {
        return requireRange(value, 1, null);
    }

    /**
     * Validates that the array is not null or empty.
     * @param objects The array.
     * @param <T> The members type.
     * @return The array.
     * @throws NullPointerException If the array is null.
     * @throws IllegalArgumentException If the array is empty.
     */
    public static <T> T[] requireNonEmpty(T[] objects) {
        if (Objects.requireNonNull(objects, "Array is null.").length == 0)
            throw new IllegalArgumentException("Array is empty.");
        return objects;
    }

    /**
     * Validates that the list is not null or empty.
     * @param objects The list.
     * @param <T> The members type.
     * @return The list.
     * @throws NullPointerException If the list is null.
     * @throws IllegalArgumentException If the list is empty.
     */
    public static <T> List<T> requireNonEmpty(List<T> objects) {
        if (Objects.requireNonNull(objects, "List is null.").isEmpty())
            throw new IllegalArgumentException("List is empty.");
        return objects;
    }

    /**
     * Validates that the array and every one of its members is not null.
     * @param objects The array.
     * @param <T> The members type.
     * @return The array.
     * @throws NullPointerException If the array or any of its members is null.
     */
    public static <T> T[] requireNoneNull(T[] objects) {
        Stream.of(Objects.requireNonNull(objects, "Array is null.")).forEach(
                o -> Objects.requireNonNull(o, "Array contains a null reference."));
        return objects;
    }

    /**
     * Validates that the list and every one of its members is not null.
     * @param objects The list.
     * @param <T> The members type.
     * @return The list.
     * @throws NullPointerException If the list or any of its members is null.
     */
    public static <T> List<T> requireNoneNull(List<T> objects) {
        Objects.requireNonNull(objects, "List is null.").forEach(
                o -> Objects.requireNonNull(o, "List contains a null reference."));
        return objects;
    }

    /**
     * Validates that the array is not null or empty, and none of its members is null. Equivalent to:
     * <pre>
     * Sugar.requireNonEmpty(Sugar.requireNoneNull(objects))
     * </pre>
     * @param objects The array.
     * @param <T> The members type.
     * @return The array.
     */
    public static <T> T[] requireFull(T[] objects) {
        return requireNonEmpty(requireNoneNull(objects));
    }

    /**
     * Validates that the list is not null or empty, and none of its members is null. Equivalent to:
     * <pre>
     * Sugar.requireNonEmpty(Sugar.requireNoneNull(objects))
     * </pre>
     * @param objects The list.
     * @param <T> The members type.
     * @return The list.
     */
    public static <T> List<T> requireFull(List<T> objects) {
        return requireNonEmpty(requireNoneNull(objects));
    }

    /**
     * Returns the first member of the array. Throws appropriate exceptions if the array is null or empty.
     */
    public static <T> T first(T[] objects) {
        return requireNonEmpty(objects)[0];
    }

    /**
     * Returns the last member of the array. Throws appropriate exceptions if the array is null or empty.
     */
    public static <T> T last(T[] objects) {
        return requireNonEmpty(objects)[objects.length - 1];
    }

    /**
     * Returns the first member of the list. Throws appropriate exceptions if the list is null or empty.
     */
    public static <T> T first(List<T> objects) {
        return requireNonEmpty(objects).get(0);
    }

    /**
     * Returns the last member of the list. Throws appropriate exceptions if the list is null or empty.
     */
    public static <T> T last(List<T> objects) {
        return requireNonEmpty(objects).get(objects.size() - 1);
    }

    /**
     * Removes and returns the first member of the list. Throws appropriate exceptions if the list is null or empty.
     */
    public static <T> T removeFirst(List<T> objects) {
        return requireNonEmpty(objects).remove(0);
    }

    /**
     * Removes and returns the last member of the list. Throws appropriate exceptions if the list is null or empty.
     */
    public static <T> T removeLast(List<T> objects) {
        return requireNonEmpty(objects).remove(objects.size() - 1);
    }

    /**
     * Fills a modifiable list with the supplier results.
     * @param size The list size.
     * @param supplier The supplier.
     * @param <T> The list items type.
     * @return The filled list.
     */
    public static <T> List<T> fill(int size, Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "Supplier is null.");
        List<T> list = new ArrayList<>(size);
        Flow.iterate(size, i -> list.add(supplier.get()));
        return list;
    }

    /**
     * Fills a list of nulls.
     * @param size The list size.
     * @param <T> The list items type.
     * @return The filled list.
     */
    public static <T> List<T> fill(int size) {
        return fill(size, () -> null);
    }

    /**
     * Returns an unmodifiable copy of the list. Unlike <code>List.copyOf</code>, this function allows null elements in
     * the list.
     * @param list The list.
     * @param <T> The list elements type.
     * @return The unmodifiable copy of the list.
     */
    public static <T> List<T> unmodifiableCopy(List<T> list) {
        return Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(list, "List is null.")));
    }

    /**
     * Returns a flat union array of the objects passed. That is, for any member being an array, an iterable or a stream
     * itself, the inner members are added to the flat union. The order of the items is preserved as in the members.
     * @param objects An array of objects.
     * @return A flat union array of the objects passed.
     */
    public static Object[] flat(Object... objects) {
        return Stream.of(Objects.requireNonNull(objects, "Array is null.")).flatMap(o -> {
            if (o instanceof Object[] array)
                return Stream.of(array);
            else if (o instanceof Iterable<?> iterable)
                return StreamSupport.stream(iterable.spliterator(), false);
            else if (o instanceof Stream<?> stream)
                return stream;
            else
                return Stream.of(o);
        }).toArray();
    }
}
