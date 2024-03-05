package org.rickosborne.romance.util;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.rickosborne.romance.util.StringStuff.normalizeNames;

class StringStuffTest {
    @Test
    public void alphaOnlyShouldRemoveStopWords() {
        assertEquals("teapot anchor fish", StringStuff.alphaOnly("A Teapot, An Anchor, & The Fish"));
    }

    @Test
    void asPattern_case_insensitive() {
        final Pattern aliceBrown = StringStuff.asPattern("Alice Brown");
        assertEquals("(?i)\\Qalice\\E\\s+\\Qbrown\\E", aliceBrown.toString());
        assertEquals("By the author", aliceBrown.matcher("By Alice Brown").replaceAll("the author"));
    }

    @Test
    void normalizeNames_double_ampersand() {
        assertEquals("Abby Craden, Lori Prince", normalizeNames("Lori Prince & Abby Craden"));
    }

    @Test
    void normalizeNames_single() {
        assertEquals("Lori Prince", normalizeNames("Lori Prince"));
    }

    @Test
    void titleCase() {
        assertEquals("Apple Pie", StringStuff.titleCase("apple pie"));
        assertEquals("Apple Pie", StringStuff.titleCase("APPLE PIE"));
        assertEquals("Car of the Year", StringStuff.titleCase("Car of the Year"));
    }

    @Test
    void unescape_html() {
        assertEquals("don't", StringStuff.unescape("don&#x27;t"));
        assertEquals("don't", StringStuff.unescape("don&#39;t"));
        assertEquals("don©t", StringStuff.unescape("don&copy;t"));
    }
}
