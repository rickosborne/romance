package org.rickosborne.romance.util;

public class BookStuff {
    public static String cleanTitle(final String title) {
        if (title == null) {
            return null;
        }
        return title
            .replace(" (Unabridged)", "")
            .replaceAll("(?i)\\s+\\([^)]*(?:unabridged|book\\s+\\d)[^)]*\\)", "")
            .replaceAll("(?i)\\s+audiobook$", "")
            .trim();
    }
}
