package lab.drop.computation;

import lab.drop.data.Data;
import lab.drop.functional.Functional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
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
