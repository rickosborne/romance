package org.rickosborne.romance.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BookStuffTest {
    @Test
    void initialsGetFixed() {
        assertEquals("ABCD Example", BookStuff.cleanAuthor("A B C D Example"));
        assertEquals("ABCD Example", BookStuff.cleanAuthor("ABCD Example"));
        assertEquals("Abcd Example", BookStuff.cleanAuthor("abcd example"));
        assertEquals("ABCD Example", BookStuff.cleanAuthor("a b c d  Example"));
    }
}
