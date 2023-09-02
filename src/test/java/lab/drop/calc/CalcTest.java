package lab.drop.calc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

class CalcTest {

    @Test
    void numericDescription() {
        Assertions.assertEquals("0", Units.Numeric.describe(0));
        Assertions.assertEquals("1", Units.Numeric.describe(1));
        Assertions.assertEquals("-2", Units.Numeric.describe(-2));
        Assertions.assertEquals("500", Units.Numeric.describe(500));
        Assertions.assertEquals("1000", Units.Numeric.describe(1000));
        Assertions.assertEquals("1.0K", Units.Numeric.describe(1001));
        Assertions.assertEquals("90.0K", Units.Numeric.describe(90000));
        Assertions.assertEquals("90.03K", Units.Numeric.describe(90030));
        Assertions.assertEquals("1000.0K", Units.Numeric.describe(1000000));
        Assertions.assertEquals("1.0M", Units.Numeric.describe(1000001));
        Assertions.assertEquals("-900.0M", Units.Numeric.describe(-900000000));
        Assertions.assertEquals("909.09B", Units.Numeric.describe(909090000000L));
        Assertions.assertEquals("-9090.9B", Units.Numeric.describe(-9090900000000L));
    }

    @Test
    void timeUnitsDescription() {
        Assertions.assertEquals("1000 milliseconds", Units.Time.describe(Units.Time.SECOND));
        Assertions.assertEquals("60.0 seconds", Units.Time.describe(Units.Time.MINUTE));
        Assertions.assertEquals("60.0 minutes", Units.Time.describe(Units.Time.HOUR));
        Assertions.assertEquals("24.0 hours", Units.Time.describe(Units.Time.DAY));
        Assertions.assertEquals("7.0 days", Units.Time.describe(Units.Time.WEEK));

        Assertions.assertEquals("0 milliseconds", Units.Time.describe(0));
        Assertions.assertEquals("400 milliseconds", Units.Time.describe(400));
        Assertions.assertEquals("-25 milliseconds", Units.Time.describe(-25));
        Assertions.assertEquals("1999 milliseconds", Units.Time.describe(1999));
        Assertions.assertEquals("4.0 seconds", Units.Time.describe(4000));
        Assertions.assertEquals("2.12 seconds", Units.Time.describe(2123));
        Assertions.assertEquals("2.13 seconds", Units.Time.describe(2125));
        Assertions.assertEquals("80.5 seconds", Units.Time.describe(80500));
        Assertions.assertEquals("-90.5 seconds", Units.Time.describe(-90500));
        Assertions.assertEquals("120.0 seconds", Units.Time.describe(120000));
        Assertions.assertEquals("2.1 minutes", Units.Time.describe(126000));
        Assertions.assertEquals("16.32 minutes", Units.Time.describe(978990));
        Assertions.assertEquals("120.0 minutes", Units.Time.describe(60000 * 120));
        Assertions.assertEquals("2.1 hours", Units.Time.describe(60000 * 126));
        Assertions.assertEquals("72.0 hours", Units.Time.describe(60000 * 60 * 72));
        Assertions.assertEquals("3.0 days", Units.Time.describe(60001 * 60 * 72));
        Assertions.assertEquals("21.0 days", Units.Time.describe(60000 * 60 * 72 * 7));
        Assertions.assertEquals("3.0 weeks", Units.Time.describe(60001 * 60 * 72 * 7));
        Assertions.assertEquals("300.0 weeks", Units.Time.describe(60000L * 60 * 72 * 7 * 100));
        Assertions.assertEquals("-1001.0 weeks", Units.Time.describe(-60000L * 60 * 24 * 7 * 1001));
    }

    @Test
    void timeUnitsSince() throws InterruptedException {
        long startMillis = System.currentTimeMillis();
        Thread.sleep(200);
        System.out.println(Units.Time.describeSince(startMillis));

        long startNano = System.nanoTime();
        Thread.sleep(200);
        System.out.println(Units.Time.describeSinceNano(startNano));
    }

    @Test
    void sizeUnitsDescription() {
        Assertions.assertEquals("1024 bytes", Units.Size.describe(Units.Size.KB));
        Assertions.assertEquals("1024.0 KB", Units.Size.describe(Units.Size.MB));
        Assertions.assertEquals("1024.0 MB", Units.Size.describe(Units.Size.GB));
        Assertions.assertEquals("1024.0 GB", Units.Size.describe(Units.Size.TB));
        Assertions.assertEquals("1024.0 TB", Units.Size.describe(Units.Size.PB));

        Assertions.assertEquals("0 bytes", Units.Size.describe(0));
        Assertions.assertEquals("500 bytes", Units.Size.describe(500));
        Assertions.assertEquals("-100 bytes", Units.Size.describe(-100));
        Assertions.assertEquals("1.0 KB", Units.Size.describe(1025));
        Assertions.assertEquals("7.81 KB", Units.Size.describe(8000));
        Assertions.assertEquals("-7.81 KB", Units.Size.describe(-8000));
        Assertions.assertEquals("200.0 MB", Units.Size.describe(Units.Size.MB * 200));
        Assertions.assertEquals("300.0 GB", Units.Size.describe(Units.Size.GB * 300));
        Assertions.assertEquals("400.0 TB", Units.Size.describe(Units.Size.TB * 400));
        Assertions.assertEquals("500.0 PB", Units.Size.describe(Units.Size.PB * 500));
        Assertions.assertEquals("-5000.0 PB", Units.Size.describe(Units.Size.PB * -5000));
    }

    @Test
    void zip() {
        String text = "There's nothing special about this text, but repeating it will make it easy to zip.".repeat(999);
        byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);
        byte[] zipped = Calc.zip(utf8);
        System.out.printf("Zipped %s into %s%n", Units.Size.describe(utf8.length), Units.Size.describe(zipped.length));
        Assertions.assertEquals(389, zipped.length);
        byte[] unzipped = Calc.unzip(zipped);
        Assertions.assertEquals(text, new String(unzipped, StandardCharsets.UTF_8));
    }

    @Test
    void u25() {
        u25("00000000-0000-0000-0000-000000000000", "0", U25.hash((Object[]) null));
        u25("10000000-1000-1000-1000-100000000000", "y3mdbdz8auw3bt6lc7yass8w", null);
        u25("ffffffff-ffff-ffff-ffff-ffffffffffff", "f5lxx1zz5pnorynqglhzmsp33", null);
        u25("123e4567-e89b-12d3-a456-426614174000", "12vqjrnxk8whv3i8qi6qgrlz4", null);
        u25("0e5ab0bf-bf70-4ab4-b9c7-689be859581a", "ulcl9o7s5x5un8t2k4g4zkt6", null);
        u25("fd14a171-ebfc-492e-a6e7-9638b123d328", "ezdy2k5hz2lyj44f9w7rbsc2w", null);
        u25("00000000-0000-0000-0000-000000000001", "1", U25.fromString("1"));
        u25("00000000-0000-0000-0000-000000000024", "10", U25.fromString("10"));
        u25("00000000-0000-0000-0000-0000001b1d45", "12345", U25.fromString("12345"));
        u25("004f0a98-ff57-8acc-6ec4-61f0228115db", "notthelongestexample123", U25.fromString("NotTheLongestExample123"));
        u25("00000000-0000-0001-0000-000000000001", "3w5e11264sgsh", U25.hash());
        u25("00000000-0000-0001-0000-00000000001f", "3w5e11264sgtb", U25.hash((Object) null));
        u25("00000000-0000-0001-ffff-ffff8567792b", "7sas223e907xn", U25.hash(0));
        u25("ffffffff-8567-792b-ffff-ffff8567792b", "f5lxx1waqdrvu0ehr72ewiywb", U25.hash(0, 0));
        u25("ffffffff-8567-7a9f-ffff-fe334edb3a5a", "f5lxx1waqdt0283gdg9r205lm", U25.hash(12, 13, 14));
        u25("00000000-0000-0001-ffff-ffff85382bea", "7sas223e75rze", U25.hash(-99999));
        u25("00000000-0000-0001-ffff-fff6707eafbb", "7sas21lh64uyj", U25.hash("Some object"));
        u25("00000000-4c03-f392-ffff-ffff856856c5", "2a3z97tr1xn2nw8tbd1", U25.hash("Some", null, 900));
        u25("5d21302f-d1cc-b999-eef2-21f86b13a645", "5ihhv3tmia5eqck4ru1qt32v9", U25.hash("A", "little", "longer",
                "array", "of", "objects", 1, 2, 3, 4, 5, 6, 7, 8, 9, 0.1, true, false, -222.333, U25.hash("not", "random")));
        println(Assertions.assertThrows(IllegalArgumentException.class, () -> U25.fromString("Longer than 25 but not a UUID")));
        println(Assertions.assertThrows(IllegalArgumentException.class, () -> U25.fromString("invalid!")));
        println(Assertions.assertThrows(IllegalArgumentException.class, () -> U25.fromString("not valid")));
        println(Assertions.assertThrows(IllegalArgumentException.class, () -> U25.fromString("non-valid")));
        println(Assertions.assertThrows(IllegalArgumentException.class, () -> U25.fromString("zzzzzzzzzzzzzzzzzzzzzzzzz")));
        println(Assertions.assertThrows(IllegalArgumentException.class, () -> U25.fromString("f5lxx1zz5pnorynqglhzmsp34")));
    }

    private void u25(String name, String name25, U25 generated) {
        var u25 = new U25(UUID.fromString(name));
        if (generated != null)
            Assertions.assertEquals(u25, generated);
        Assertions.assertEquals(name25, u25.toString());
        Assertions.assertEquals(u25, U25.fromString(name25));
        Assertions.assertEquals(u25, U25.fromString(name25.toUpperCase()));
        Assertions.assertEquals(u25, U25.fromString(name));
        Assertions.assertEquals(u25, U25.fromString(name.toUpperCase()));
        Assertions.assertEquals(u25, U25.fromString(u25.toString()));
        Assertions.assertEquals(name, u25.uuid().toString());
        println(u25);
    }

    private void println(U25 u25) {
        System.out.println(u25.uuid() + " : " + u25 + "  (" + u25.uuid().getMostSignificantBits() + ", " +
                u25.uuid().getLeastSignificantBits() + ")");
    }

    private void println(IllegalArgumentException e) {
        System.out.println(e.getMessage());
        if (e.getCause() != null)
            System.out.println("  " + e.getCause().getMessage());
    }
}
