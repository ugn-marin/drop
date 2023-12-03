package lab.drop.text;

import lab.drop.data.Data;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Various text utilities.
 */
public class Text {

    private Text() {}

    /**
     * Returns the text if it's not null and not blank, or the provided alternative otherwise.
     * @param text The text.
     * @param orElse The alternative.
     * @return The resulting string.
     */
    public static String orElse(String text, String orElse) {
        return text == null || text.isBlank() ? orElse : text;
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
     * Returns the last <code>length</code> characters of the text. If the text is shorter than the length, the text is
     * returned as is.
     * @param text The text.
     * @param length The length.
     * @return The resulting string.
     */
    public static String tail(String text, int length) {
        return Objects.requireNonNull(text, "Text is null.").isEmpty() ? text :
                text.substring(Math.max(0, text.length() - Math.max(0, length)));
    }
}
