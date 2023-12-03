package lab.drop.functional;

import lab.drop.data.Data;
import lab.drop.flow.Flow;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Various functional utilities.
 */
public class Functional {

    private Functional() {}

    /**
     * Returns a stream of all the enumeration elements.
     */
    public static <T> Stream<T> stream(Enumeration<T> enumeration) {
        return stream(Objects.requireNonNull(enumeration, "Enumeration is null.").asIterator());
    }

    /**
     * Returns a stream of all the iterator elements.
     */
    public static <T> Stream<T> stream(Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(Objects.requireNonNull(iterator,
                "Iterator is null."), Spliterator.ORDERED), false);
    }

    /**
     * Returns a stream of characters of the text.
     */
    public static Stream<Character> stream(String text) {
        return Objects.requireNonNull(text, "Text is null.").chars().mapToObj(c -> (char) c);
    }

    /**
     * Returns a union stream of elements from the streams passed.
     * @param streams An array of streams.
     * @param <T> The type of the streams' elements.
     * @return A union stream of elements from the streams passed.
     */
    @SafeVarargs
    public static <T> Stream<T> union(Stream<? extends T>... streams) {
        return Stream.of(Data.requireFull(streams)).flatMap(Function.identity());
    }

    /**
     * Wraps a callable implementation in a Supplier throwing sneaky. To define an <i>on exception</i> value computation
     * use the <code>orElse</code> method. To convert to a monadic value, use the <code>toMonadicSupplier</code> method.
     */
    public static <T> Supplier<T> toSneakySupplier(Callable<T> callable) {
        Objects.requireNonNull(callable, "Callable is null.");
        return () -> {
            try {
                return callable.call();
            } catch (Exception e) {
                throw Flow.sneaky(e);
            }
        };
    }

    /**
     * Wraps a callable implementation in a Supplier returning a monadic wrapper of the result.
     */
    public static <T> Supplier<Unsafe<T>> toMonadicSupplier(Callable<T> callable) {
        Objects.requireNonNull(callable, "Callable is null.");
        return Flow.orElse(() -> Unsafe.success(callable.call()), Unsafe::failure);
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
     * Functional.toSneakySupplier(callable).get()
     * </pre>
     */
    public static <T> T sneaky(Callable<T> callable) {
        return toSneakySupplier(callable).get();
    }

    /**
     * Returns a composed unsafe function applying the first function to the input, then the second one to the result of
     * the first one.
     * @param first The first function.
     * @param second The second function.
     * @param <I> The first function input type.
     * @param <T> The first function output and second function input type.
     * @param <O> The second function output type.
     * @return The composed function.
     */
    public static <I, T, O> UnsafeFunction<I, O> compose(UnsafeFunction<I, T> first, UnsafeFunction<T, O> second) {
        Objects.requireNonNull(first, "First function is null.");
        Objects.requireNonNull(second, "Second function is null.");
        return t -> second.apply(first.apply(t));
    }

    /**
     * Returns an unsafe runnable running the provided unsafe runnable tasks one by one.
     */
    public static UnsafeRunnable merge(UnsafeRunnable... tasks) {
        Data.requireFull(tasks);
        return () -> Flow.forEach(Stream.of(tasks), UnsafeRunnable::run);
    }

    /**
     * Performs an unsafe cast to the required non-primitive type. To be used as a {@link java.util.function.Function}.
     */
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object object) {
        return (T) object;
    }

    /**
     * Returns the Boolean value. To be used as an identity {@link java.util.function.Predicate}.
     */
    public static boolean is(Boolean value) {
        return value != null && value;
    }
}
