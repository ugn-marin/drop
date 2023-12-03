package lab.drop.text;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TextTest {

    @Test
    void orElse() {
        Assertions.assertEquals("a", Text.orElse("a", "b"));
        Assertions.assertEquals("b", Text.orElse("", "b"));
        Assertions.assertEquals("b", Text.orElse(null, "b"));
    }

    @Test
    void tail() {
        Assertions.assertEquals("bc", Text.tail("abc", 2));
        Assertions.assertEquals("abc", Text.tail("abc", 3));
        Assertions.assertEquals("abc", Text.tail("abc", 4));
        Assertions.assertEquals("", Text.tail("abc", 0));
        Assertions.assertEquals("", Text.tail("abc", -1));
        Assertions.assertEquals("", Text.tail("", 0));
        Assertions.assertEquals("", Text.tail("", 1));
    }
}
