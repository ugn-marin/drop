package lab.drop;

import lab.drop.concurrent.InterruptedRuntimeException;
import lab.drop.concurrent.Lazy;
import lab.drop.data.Range;
import lab.drop.function.Reducer;
import lab.drop.function.Unsafe;
import lab.drop.function.UnsafeConsumer;
import lab.drop.function.UnsafeRunnable;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Various syntax sugar utilities.
 */
public abstract class Sugar {

    private Sugar() {}

    /**
     * Returns a supplier calling the callable and returning the result, or a function result if thrown an exception.
     * @param callable The callable.
     * @param onException A function returning a result on the callable exception.
     * @param <T> The callable return type.
     * @return A supplier of the callable result if returned, or else the function result.
     */
    public static <T> Supplier<T> orElse(Callable<T> callable, Function<Exception, T> onException) {
        Objects.requireNonNull(callable, "Callable is null.");
        Objects.requireNonNull(onException, "Exception function is null.");
        return () -> {
            try {
                return callable.call();
            } catch (Exception e) {
                return onException.apply(e);
            }
        };
    }

    /**
     * Returns a supplier calling the callable and testing its result for success.
     * @param callable The callable.
     * @param success A predicate testing the result for success.
     * @param <T> The callable return type.
     * @return A supplier of the success test result. False might mean a false test result, failure of the callable, or
     * failure of the predicate.
     */
    public static <T> Supplier<Boolean> success(Callable<T> callable, Predicate<T> success) {
        Objects.requireNonNull(callable, "Callable is null.");
        Objects.requireNonNull(success, "Success predicate is null.");
        return orElse(() -> success.test(callable.call()), e -> false);
    }

    /**
     * Iterates from 0 (inclusive) to <code>times</code> (exclusive).
     * @param times The iteration range.
     * @param consumer The consumer.
     */
    public static void iterate(int times, Consumer<Integer> consumer) {
        Range.of(0, times).forEach(consumer);
    }

    /**
     * Repeats a runnable call forever (or until throws a runtime exception or error).
     * @param runnable The runnable.
     */
    @SuppressWarnings("InfiniteLoopStatement")
    public static void forever(Runnable runnable) {
        Objects.requireNonNull(runnable, "Runnable is null.");
        while (true)
            runnable.run();
    }

    /**
     * Returns a stream of all the enumeration elements, or until reaches null.
     */
    public static <T> Stream<T> stream(Enumeration<T> enumeration) {
        return stream(Objects.requireNonNull(enumeration, "Enumeration is null.").asIterator());
    }

    /**
     * Returns a stream of all the iterator elements, or until reaches null.
     */
    public static <T> Stream<T> stream(Iterator<T> iterator) {
        Objects.requireNonNull(iterator, "Iterator is null.");
        return Stream.generate(() -> iterator.hasNext() ? iterator.next() : null).takeWhile(Objects::nonNull);
    }

    /**
     * Returns a stream of characters of the text.
     */
    public static Stream<Character> stream(String text) {
        return Objects.requireNonNull(text, "Text is null.").chars().mapToObj(c -> (char) c);
    }

    /**
     * Produces and accepts values into the consumer as long as they pass the predicate.
     * @param callable The callable producing the values.
     * @param consumer The values consumer.
     * @param predicate The predicate testing the values.
     * @param <T> The values type.
     * @throws Exception Any exception thrown by the implementations.
     */
    public static <T> void acceptWhile(Callable<T> callable, UnsafeConsumer<T> consumer, Predicate<T> predicate)
            throws Exception {
        Objects.requireNonNull(consumer, "Consumer is null.");
        Objects.requireNonNull(predicate, "Predicate is null.");
        T value = Objects.requireNonNull(callable, "Callable is null.").call();
        while (predicate.test(value)) {
            consumer.accept(value);
            value = callable.call();
        }
    }

    /**
     * Produces and accepts optional values into the consumer until empty.
     * @param callable The callable producing the optional values.
     * @param consumer The values consumer.
     * @param <T> The values type.
     * @throws Exception Any exception thrown by the implementations.
     */
    public static <T> void acceptWhilePresent(Callable<Optional<T>> callable, UnsafeConsumer<T> consumer)
            throws Exception {
        Objects.requireNonNull(consumer, "Consumer is null.");
        acceptWhile(callable, optional -> consumer.accept(optional.orElseThrow()), Optional::isPresent);
    }

    /**
     * Accepts the stream elements into the unsafe consumer. Converts the stream to sequential if isn't.
     * @param stream The stream.
     * @param consumer The consumer.
     * @param <T> The elements type.
     * @throws Exception Any exception thrown by the consumer.
     */
    public static <T> void forEach(Stream<T> stream, UnsafeConsumer<T> consumer) throws Exception {
        Objects.requireNonNull(consumer, "Consumer is null.");
        var iterator = Objects.requireNonNull(stream, "Stream is null.").sequential().iterator();
        while (iterator.hasNext())
            consumer.accept(iterator.next());
    }

    /**
     * Wraps a callable implementation in a Supplier throwing sneaky. To define an <i>on exception</i> value calculation
     * use the <code>orElse</code> method. To convert to a monadic value, use the <code>toMonadicSupplier</code> method.
     */
    public static <T> Supplier<T> toSneakySupplier(Callable<T> callable) {
        Objects.requireNonNull(callable, "Callable is null.");
        return () -> {
            try {
                return callable.call();
            } catch (Exception e) {
                throw sneaky(e);
            }
        };
    }

    /**
     * Wraps a callable implementation in a Supplier returning a monadic wrapper of the result.
     */
    public static <T> Supplier<Unsafe<T>> toMonadicSupplier(Callable<T> callable) {
        Objects.requireNonNull(callable, "Callable is null.");
        return () -> {
            try {
                return Unsafe.success(callable.call());
            } catch (Exception e) {
                return Unsafe.failure(e);
            }
        };
    }

    /**
     * Runs an unsafe runnable as a sneaky runnable. Equivalent to:
     * <pre>
     * runnable.toSneakyRunnable().run()
     * </pre>
     */
    public static void sneaky(UnsafeRunnable runnable) {
        Objects.requireNonNull(runnable, "Runnable is null.").toSneakyRunnable().run();
    }

    /**
     * Runs a callable as a sneaky supplier. Equivalent to:
     * <pre>
     * Sugar.toSneakySupplier(callable).get()
     * </pre>
     */
    public static <T> T sneaky(Callable<T> callable) {
        return toSneakySupplier(callable).get();
    }

    /**
     * Returns the exception as is if runtime exception, or wrapped in a new UndeclaredThrowableException otherwise.
     */
    public static RuntimeException sneaky(Exception e) {
        Objects.requireNonNull(e, "Exception is null.");
        if (e instanceof RuntimeException re)
            return re;
        else if (e instanceof InterruptedException ie)
            return new InterruptedRuntimeException(ie);
        return new UndeclaredThrowableException(e);
    }

    /**
     * Returns the throwable as an exception.
     * @param throwable A throwable.
     * @return The throwable if an exception or null, or wrapped in a new UndeclaredThrowableException otherwise.
     */
    public static Exception toException(Throwable throwable) {
        if (throwable instanceof Exception e)
            return e;
        return throwable == null ? null : new UndeclaredThrowableException(throwable);
    }

    /**
     * Throws the throwable as an exception, or as Error if is an Error.
     * @param throwable A throwable.
     * @throws Exception The throwable if not null, thrown as is if instance of Exception or Error, or wrapped in a new
     * UndeclaredThrowableException otherwise.
     */
    public static void throwIfNonNull(Throwable throwable) throws Exception {
        if (throwable == null)
            return;
        if (throwable instanceof Error e)
            throw e;
        throw toException(throwable);
    }

    /**
     * Throws the exception reduced from the list, if any.
     * @param exceptionsReducer The exceptions reducer.
     * @param exceptions A lazy list of exceptions.
     * @throws Exception The reduced exception.
     */
    public static void throwIfAny(Reducer<Exception> exceptionsReducer, Lazy<List<Exception>> exceptions)
            throws Exception {
        Objects.requireNonNull(exceptionsReducer, "Reducer is null.");
        maybe(exceptions, list -> throwIfNonNull(exceptionsReducer.apply(list)));
    }

    /**
     * Performs an unsafe operation on the lazy value if and only if calculated.
     * @param lazy The lazy value supplier.
     * @param consumer The value consumer.
     * @param <T> The value type.
     * @throws Exception The consumer exception.
     */
    public static <T> void maybe(Lazy<T> lazy, UnsafeConsumer<T> consumer) throws Exception {
        Objects.requireNonNull(consumer, "Consumer is null.");
        if (Objects.requireNonNull(lazy, "Lazy is null.").isCalculated())
            consumer.accept(lazy.get());
    }

    /**
     * Returns an unsafe runnable running the provided unsafe runnable steps one by one.
     */
    public static UnsafeRunnable glue(UnsafeRunnable... steps) {
        requireFull(steps);
        return () -> forEach(Stream.of(steps), UnsafeRunnable::run);
    }

    /**
     * Runs the provided unsafe runnable steps with guaranteed execution: For each step, subsequent steps are executed
     * in the <code>finally</code> block. Throwables are accepted by the throwable consumer.
     * @param steps The steps.
     * @param throwableConsumer The consumer of the steps' throwables.
     */
    public static void runSteps(Iterator<UnsafeRunnable> steps, Consumer<Throwable> throwableConsumer) {
        Objects.requireNonNull(steps, "Steps iterator is null.");
        Objects.requireNonNull(throwableConsumer, "Throwable consumer is null.");
        if (!steps.hasNext()) {
            return;
        }
        try {
            Objects.requireNonNull(steps.next(), "Step runnable is null.").run();
        } catch (Throwable t) {
            throwableConsumer.accept(t);
        } finally {
            runSteps(steps, throwableConsumer);
        }
    }

    /**
     * Runs the provided unsafe runnable steps with guaranteed execution: For each step, subsequent steps are executed
     * in the <code>finally</code> block. Throwables are thrown if Error, otherwise reduced by the provided reducer.
     * @param steps The steps.
     * @param exceptionsReducer The exceptions reducer for all throwables except Errors.
     * @throws Exception The reduced exception if any step(s) failed.
     */
    public static void runSteps(Iterator<UnsafeRunnable> steps, Reducer<Exception> exceptionsReducer) throws Exception {
        Lazy<List<Exception>> exceptions = new Lazy<>(ArrayList::new);
        Objects.requireNonNull(exceptionsReducer, "Reducer is null.");
        runSteps(steps, throwable -> {
            if (throwable instanceof Error e)
                throw e;
            exceptions.get().add(toException(throwable));
        });
        throwIfAny(exceptionsReducer, exceptions);
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
        iterate(size, i -> list.add(supplier.get()));
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
     * Returns the object if instance of type.
     * @param object An object.
     * @param type A class.
     * @param orElse The default return value if the object is not an instance of the type.
     * @param <T> The type to cast the object to, if is instance of the type. Must be assignable from <code>type</code>
     *           - not validated. Must be non-primitive.
     * @return The object if instance of type, or the default.
     */
    public static <T> T as(Object object, Class<?> type, T orElse) {
        Objects.requireNonNull(type, "Type is null.");
        return type.isInstance(object) ? cast(object) : orElse;
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
                .map(Sugar::<T>cast).collect(Collectors.toSet());
    }

    /**
     * Returns true if the object is an instance of one or more of the passed classes.
     * @param object The object.
     * @param types The classes.
     * @return True if instance of any, else false.
     */
    public static boolean instanceOfAny(Object object, Class<?>... types) {
        return object != null && Stream.of(requireFull(types)).anyMatch(t -> t.isAssignableFrom(object.getClass()));
    }

    /**
     * Performs an unsafe cast to the required non-primitive type. To be used as a {@link java.util.function.Function}.
     */
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object object) {
        return (T) object;
    }

    /**
     * Returns the boolean value. To be used as an identity {@link java.util.function.Predicate}.
     */
    public static boolean is(boolean value) {
        return value;
    }

    /**
     * Returns a union stream of elements from the streams passed.
     * @param streams An array of streams.
     * @param <T> The type of the streams' elements.
     * @return A union stream of elements from the streams passed.
     */
    @SafeVarargs
    public static <T> Stream<T> union(Stream<? extends T>... streams) {
        return Stream.of(requireFull(streams)).flatMap(Function.identity());
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

    /**
     * Constructs a strings array containing the result of <code>toString</code> for each non-null array member.
     * @param array The array.
     * @param <T> The members type.
     * @return The strings array.
     */
    public static <T> String[] toStrings(T[] array) {
        return Stream.of(Objects.requireNonNull(array, "Array is null.")).filter(Objects::nonNull)
                .map(Objects::toString).toArray(String[]::new);
    }

    /**
     * Removes all instances of all substrings listed from the text.
     * @param text The text.
     * @param substrings The substrings to remove.
     * @return The resulting string.
     */
    public static String remove(String text, String... substrings) {
        return replace(text, toStrings(Stream.of(requireNoneNull(substrings)).flatMap(s -> Stream.of(s, ""))
                .toArray()));
    }

    /**
     * Performs an ordered set of replacements on the text, replacing the first string with the second, the third with
     * the forth and so on. Each replacement is repeated as long as instances of the target string still exist in the
     * text, in order to support repetitive patterns.
     * @param text The text.
     * @param replacements The target and replacement substrings (must be even).
     * @return The resulting string.
     */
    public static String replace(String text, String... replacements) {
        if (requireNoneNull(replacements).length % 2 != 0)
            throw new IllegalArgumentException("The replacements array length must be even.");
        for (int i = 0; i < replacements.length; i += 2) {
            String target = replacements[i];
            String replacement = replacements[i + 1];
            if (target.equals(replacement))
                continue;
            while (text.contains(target))
                text = text.replace(target, replacement);
        }
        return text;
    }
}
