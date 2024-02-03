package lab.drop.computation;

import lab.drop.data.Data;
import lab.drop.functional.Functional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Various computation utilities.
 */
public class Computation {

    private Computation() {}

    /**
     * Similar to Objects::hash or Arrays::hashCode, but returning a long hash.
     */
    public static long hash64(Object... objects) {
        if (objects == null)
            return 0;
        long result = 1;
        for (Object object : Data.flat(objects)) {
            result = 31 * result + (object == null ? 0 : object.hashCode());
            if (object != null)
                result = 31 * result + object.getClass().getName().hashCode();
        }
        return result;
    }

    /**
     * Splits the array in half, applies hash64 to each half, returns as a UUID.
     */
    public static UUID hash128(Object... objects) {
        if (objects == null)
            return new UUID(0, 0);
        objects = Data.flat(objects);
        return new UUID(hash64(Arrays.copyOfRange(objects, 0, objects.length / 2)),
                hash64(Arrays.copyOfRange(objects, objects.length / 2, objects.length)));
    }

    /**
     * Returns a compact string representation of the UUID, using radix 36 rather than the UUID string standard radix
     * 16. The resulting string will be up to 25 characters long, and contain digits and alphabetic characters only. Use
     * in cases when a compact yet simple unique string identifier is needed, like a URL.<br>
     * To restore the UUID from the string, use the <code>fromCompactId</code> method.
     * @param uuid A UUID.
     * @return A compact string representation of the UUID.
     */
    public static String toCompactId(UUID uuid) {
        Objects.requireNonNull(uuid, "UUID is null.");
        return new BigInteger(1, ByteBuffer.allocate(16).putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits()).array()).toString(Character.MAX_RADIX);
    }

    /**
     * Creates a UUID from a compact string representation of the UUID created by the <code>toCompactId</code> method.
     * @param string A compact string representation of the UUID.
     * @return The UUID represented by the string.
     */
    public static UUID fromCompactId(String string) {
        if (Objects.requireNonNull(string, "String is null.").length() > 25)
            throw new IllegalArgumentException("Invalid compact ID string: " + string);
        try {
            var bytes = new BigInteger(string, Character.MAX_RADIX).toByteArray();
            var length = Math.min(bytes.length, 16);
            var buffer = ByteBuffer.allocate(16).put(16 - length, bytes, Math.max(bytes.length, 16) - 16, length);
            var uuid = new UUID(buffer.rewind().getLong(), buffer.getLong());
            if (string.length() == 25 && !string.equalsIgnoreCase(toCompactId(uuid)))
                throw new NumberFormatException("Value is out of range.");
            return uuid;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid compact ID string: " + string, e);
        }
    }

    /**
     * Compresses bytes using a GZIP stream.
     * @param bytes A bytes array.
     * @return A zipped bytes array.
     */
    public static byte[] zip(byte[] bytes) {
        return Functional.sneaky(() -> {
            try (var out = new ByteArrayOutputStream()) {
                try (var gzip = new GZIPOutputStream(out)) {
                    gzip.write(bytes);
                }
                return out.toByteArray();
            }
        });
    }

    /**
     * Decompresses bytes using a GZIP stream.
     * @param bytes A bytes array.
     * @return An unzipped bytes array.
     */
    public static byte[] unzip(byte[] bytes) {
        return Functional.sneaky(() -> {
            try (var gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
                return gzip.readAllBytes();
            }
        });
    }
}
