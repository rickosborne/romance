package org.rickosborne.romance.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringStuffTest {
    @Test
    public void alphaOnlyShouldRemoveStopWords() {
        assertEquals("teapot anchor fish", StringStuff.alphaOnly("A Teapot, An Anchor, & The Fish"));
    }
}
