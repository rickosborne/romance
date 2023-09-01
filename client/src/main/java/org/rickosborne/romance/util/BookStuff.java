package org.rickosborne.romance.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.rickosborne.romance.util.StringStuff.titleCase;

public class BookStuff {

    public static final Pattern LEADING_INITIALS_PATTERN = Pattern.compile("^(\\w\\s+(?:\\w\\s+)+)");

    public static String cleanAuthor(final String author) {
        if (author == null) {
            return null;
        }
        String name = author
            .replaceAll("\\.", " ")
            .replaceAll("\\s+", " ");
        final Matcher matcher = LEADING_INITIALS_PATTERN.matcher(name);
        if (matcher.find()) {
            final String start = matcher.group(1);
            final String fixed = start.replaceAll("\\s+", "").toUpperCase();
            name = name.replace(start.trim(), fixed);
        }
        return titleCase(name);
    }

    public static String cleanTitle(final String title) {
        if (title == null) {
            return null;
        }
        return titleCase(title
            .replace(" (Unabridged)", "")
            .replaceAll("[.,:/?]+", " ")
            .replaceAll("\\s+-+\\s+", " ")
            .replaceAll("\\s+", " ")
            .replaceAll("(?i)\\s+\\([^)]*(?:unabridged|book\\s+\\d)[^)]*\\)", "")
            .replaceAll("(?i)\\s+audiobook$", "")
            .trim());
    }
}
