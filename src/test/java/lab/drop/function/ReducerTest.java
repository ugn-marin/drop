package lab.drop.function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

class ReducerTest {

    @Test
    void max() {
        Assertions.assertEquals(100, Reducer.<Integer>max().apply(List.of(22, 3, 56, 82, -94, 100, 30)));
        Assertions.assertEquals("000", Reducer.max(Comparator.comparingInt(String::length)).apply(List.of(
                "22", "3", "56", "82", "000", "30")));
    }

    @Test
    void min() {
        Assertions.assertEquals(-94, Reducer.<Integer>min().apply(List.of(22, 3, 56, 82, -94, 100, 30)));
        Assertions.assertEquals("3", Reducer.min(Comparator.comparingInt(String::length)).apply(List.of(
                "22", "3", "56", "82", "000", "30")));
    }

    @Test
    void andThen() {
        Assertions.assertEquals("3", Reducer.<Integer>max().andThen(Object::toString).apply(List.of(1, 2, 3)));
        Assertions.assertEquals(10, Reducer.<Integer>min().andThen(i -> i * 10).apply(List.of(1, 2, 3)));
    }

    @Test
    void orElse() {
        Assertions.assertEquals(8, Reducer.<Integer>max().orElse(() -> 8).apply(List.of()));
        Assertions.assertEquals(8, Reducer.<Integer>min().orElse(() -> 8).apply(null));
    }

    @Test
    void orElseNull() {
        Assertions.assertNull(Reducer.first().apply(List.of()));
        Assertions.assertNull(Reducer.last().apply(null));
        Assertions.assertNull(Reducer.max().orElseNull().apply(List.of()));
        Assertions.assertNull(Reducer.min().orElseNull().apply(null));
    }

    @Test
    void converter() {
        var dateStringifier = new Converter<java.util.Date, String>(Map.of(
                java.util.Date.class, d -> "utilDate: " + d.toInstant().toString(),
                java.sql.Date.class, d -> "sqlDate: " + Instant.ofEpochMilli(d.getTime()).toString(),
                java.sql.Timestamp.class, t -> "ts: " + t.toInstant().toString()
        )).andThen(s -> s.replace("1970-01-01T", ""));
        var stringifier = new Converter<Object, String>(Map.of(
                String.class, Function.identity(),
                Integer.class, String::valueOf,
                java.util.Date.class, dateStringifier
        ), Objects::toString);
        Assertions.assertEquals("self", stringifier.apply("self"));
        Assertions.assertEquals("35", stringifier.apply(35));
        Assertions.assertEquals("utilDate: 00:00:05Z", stringifier.apply(new java.util.Date(5000)));
        Assertions.assertEquals("sqlDate: 00:00:05Z", stringifier.apply(new java.sql.Date(5000)));
        Assertions.assertEquals("ts: 00:00:05Z", stringifier.apply(new java.sql.Timestamp(5000)));
        Assertions.assertEquals("35.6", stringifier.apply(35.6));
    }
}
