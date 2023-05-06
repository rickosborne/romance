package org.rickosborne.romance.util;

import static org.rickosborne.romance.util.StringStuff.titleCase;

public class BookStuff {
    public static String cleanTitle(final String title) {
        if (title == null) {
            return null;
        }
        return titleCase(title
            .replace(" (Unabridged)", "")
            .replaceAll("(?i)\\s+\\([^)]*(?:unabridged|book\\s+\\d)[^)]*\\)", "")
            .replaceAll("(?i)\\s+audiobook$", "")
            .trim());
    }
}
