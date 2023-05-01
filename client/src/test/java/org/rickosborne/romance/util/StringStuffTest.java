package org.rickosborne.romance.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.rickosborne.romance.util.StringStuff.normalizeNames;

class StringStuffTest {
    @Test
    public void alphaOnlyShouldRemoveStopWords() {
        assertEquals("teapot anchor fish", StringStuff.alphaOnly("A Teapot, An Anchor, & The Fish"));
    }

    @Test
    void normalizeNames_double_ampersand() {
        assertEquals("Abby Craden, Lori Prince", normalizeNames("Lori Prince & Abby Craden"));
    }

    @Test
    void normalizeNames_single() {
        assertEquals("Lori Prince", normalizeNames("Lori Prince"));
    }
}
