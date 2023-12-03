package lab.drop.data;

import lab.drop.flow.Flow;
import lab.drop.functional.Functional;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Various data utilities.
 */
public class Data {

    private Data() {}

    /**
     * Returns the cast object if instance of type.
     * @param object An object.
     * @param type A class.
     * @param orElse The default return value if the object is not an instance of the type.
     * @param <T> The type to cast the object to, if is instance of the type. Must be assignable from <code>type</code>
     *           - not validated. Must be non-primitive.
     * @return The cast object if instance of type, or the default.
     */
    public static <T> T as(Object object, Class<?> type, T orElse) {
        Objects.requireNonNull(type, "Type is null.");
        return type.isInstance(object) ? Functional.cast(object) : orElse;
    }

    /**
     * Returns an Optional of the cast object if instance of type.
     * @param object An object.
     * @param type A class.
     * @param <T> The type to cast the object to, if is instance of the type. Must be assignable from <code>type</code>
     *           - not validated. Must be non-primitive.
     * @return An Optional of the cast object if instance of type, or empty otherwise.
     */
    public static <T> Optional<T> as(Object object, Class<?> type) {
        return Optional.ofNullable(as(object, type, null));
    }

    /**
     * Returns a set of all members of the collection that are instances of a certain type.
     * @param objects An objects collection.
     * @param type A class.
     * @param <T> The type to cast the found members to. Must be assignable from <code>type</code> - not validated.
     * @return A new set of the matching members.
     */
    public static <T> Set<T> instancesOf(Collection<?> objects, Class<?> type) {
        Objects.requireNonNull(type, "Type is null.");
        return Objects.requireNonNull(objects, "Collection is null.").stream().filter(type::isInstance)
                .map(Functional::<T>cast).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns true if the object is an instance of one or more of the passed classes.
     * @param object The object.
     * @param types The classes.
     * @return True if instance of any, else false.
     */
    public static boolean instanceOfAny(Object object, Class<?>... types) {
        return object != null && Stream.of(Data.requireFull(types))
                .anyMatch(t -> t.isAssignableFrom(object.getClass()));
    }

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
     * Data.requireNonEmpty(Data.requireNoneNull(objects))
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
     * Data.requireNonEmpty(Data.requireNoneNull(objects))
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
     * Returns a union set of elements from the sets passed.
     * @param sets An array of sets.
     * @param <T> The sets elements type.
     * @return A union set of elements from the sets.
     */
    @SafeVarargs
    public static <T> Set<T> union(Set<T>... sets) {
        return Stream.of(requireFull(sets)).flatMap(Set::stream).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns a sorted set of the elements in the set passed.
     * @param set The set.
     * @param <T> The set elements type.
     * @return A sorted set of the elements in the set passed.
     */
    public static <T extends Comparable<T>> Set<T> sorted(Set<T> set) {
        return sorted(set, Comparator.naturalOrder());
    }

    /**
     * Returns a sorted set of the elements in the set passed.
     * @param set The set.
     * @param comparator The comparator to sort by.
     * @param <T> The set elements type.
     * @return A sorted set of the elements in the set passed.
     */
    public static <T> Set<T> sorted(Set<T> set, Comparator<T> comparator) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(Objects.requireNonNull(set, "Set is null.").stream()
                .sorted(Objects.requireNonNull(comparator, "Comparator is null.")).toList()));
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
