package lab.drop.calc;

import lab.drop.Sugar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Various calculation utilities.
 */
public abstract class Calc {

    private Calc() {}

    /**
     * Similar to Objects::hash or Arrays::hashCode, but returning a long hash.
     */
    public static long hash64(Object... objects) {
        if (objects == null)
            return 0;
        long result = 1;
        for (Object object : Sugar.flat(objects)) {
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
        objects = Sugar.flat(objects);
        return new UUID(hash64(Arrays.copyOfRange(objects, 0, objects.length / 2)),
                hash64(Arrays.copyOfRange(objects, objects.length / 2, objects.length)));
    }

    /**
     * Compresses bytes using a GZIP stream.
     * @param bytes A bytes array.
     * @return A zipped bytes array.
     */
    public static byte[] zip(byte[] bytes) {
        return Sugar.sneaky(() -> {
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
        return Sugar.sneaky(() -> {
            try (var in = new ByteArrayInputStream(bytes)) {
                try (var gzip = new GZIPInputStream(in)) {
                    return gzip.readAllBytes();
                }
            }
        });
    }
}
