package lab.drop.calc;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;

/**
 * The Unique-25 is a wrapper of a UUID, producing a 25 characters string representation, instead of the 36 characters
 * representation of the UUID. This is achieved by converting the UUID value using base 36 rather than the UUID string
 * standard base 16. The resulting string will contain digits and lower case alphabetic characters only. For use in
 * cases when a compact yet simple unique string identifier is needed, like a URL. For comparison, a base 64 encoded
 * representation of the UUID bytes takes 24 characters.<br>
 * The U25 is interchangeable with UUID, and the produced string is interchangeable with a U25 instance by using the
 * <code>fromString</code> method.
 */
public class U25 {
    private final UUID uuid;
    private final String compact;

    /**
     * Constructs a U25 instance of the provided UUID.
     */
    public U25(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid, "UUID is null.");
        var buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        compact = new BigInteger(1, buffer.array()).toString(36);
    }

    /**
     * Creates a U25 instance from the string standard representation as described in the <code>toString</code> method,
     * a compact U25 string, or an instance from the UUID created by the <code>UUID.fromString</code> of that string.
     * @param string A U25 or UUID string.
     * @return The U25 represented by the string.
     */
    public static U25 fromString(String string) {
        if (Objects.requireNonNull(string, "String is null.").length() > 25)
            return new U25(UUID.fromString(string));
        try {
            var bytes = new BigInteger(string, 36).toByteArray();
            var buffer = ByteBuffer.allocate(16);
            var length = Math.min(bytes.length, 16);
            buffer.put(16 - length, bytes, bytes.length == 17 ? 1 : 0, length);
            buffer.rewind();
            return new U25(new UUID(buffer.getLong(), buffer.getLong()));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid U25 string: " + string + ".", e);
        }
    }

    /**
     * Returns the U25 instance from a randomly generated UUID.
     */
    public static U25 random() {
        return new U25(UUID.randomUUID());
    }

    /**
     * Returns the U25 instance from the objects' hash128.
     */
    public static U25 hash(Object... objects) {
        return new U25(Calc.hash128(objects));
    }

    /**
     * Returns the UUID.
     */
    public UUID uuid() {
        return uuid;
    }

    /**
     * Returns a base 36 unsigned integer of the wrapped UUID with no zeroes padding (length 1-25).
     */
    public String compact() {
        return compact;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        U25 u25 = (U25) o;
        return uuid.equals(u25.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    /**
     * Returns the string representing this U25 object: A base 36 unsigned integer of the wrapped UUID, padded with
     * zeroes if required to be exactly 25 characters. To get the compact version (without zeroes padding) use the
     * <code>compact</code> method.
     */
    @Override
    public String toString() {
        return "0".repeat(25 - compact.length()) + compact;
    }
}
