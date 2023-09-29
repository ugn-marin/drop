package lab.drop;

import lab.drop.concurrent.InterruptedRuntimeException;
import lab.drop.data.Data;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Various syntax sugar utilities.
 */
public class Sugar {

    private Sugar() {}

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
        return type.isInstance(object) ? cast(object) : orElse;
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
                .map(Sugar::<T>cast).collect(Collectors.toSet());
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
        return replace(text, toStrings(Stream.of(Data.requireNoneNull(substrings)).flatMap(s -> Stream.of(s, ""))
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
        Objects.requireNonNull(text, "Text is null.");
        if (Data.requireNoneNull(replacements).length % 2 != 0)
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

    /**
     * Returns the last <code>length</code> characters of the text.
     * @param text The text.
     * @param length The length.
     * @return The resulting string.
     */
    public static String tail(String text, int length) {
        return Objects.requireNonNull(text, "Text is null.").substring(Math.max(0, text.length() - length));
    }
}
