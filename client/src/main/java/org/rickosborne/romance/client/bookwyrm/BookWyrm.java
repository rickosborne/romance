package org.rickosborne.romance.client.bookwyrm;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.rickosborne.romance.util.StringStuff.alphaOnly;

public class BookWyrm {
    public static final Pattern BOOK_LOCATION = Pattern.compile("/book/(?<id>\\d+)");
    public static final String BOOK_SUBTITLE_DELIMITER = ":\\s+|\\s+[-—]+\\s+";

    public static Integer bookDbIdFromUrl(final String url) {
        if (url == null) {
            return null;
        }
        final Matcher matcher = BOOK_LOCATION.matcher(url);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group("id"), 10);
        }
        return null;
    }

    public static Integer bookDbIdFromUrl(final URL url) {
        return url == null ? null : bookDbIdFromUrl(url.toString());
    }

    public static String likeExpression(final String text) {
        if (text == null) {
            return null;
        }
        final String alpha = alphaOnly(text.replaceAll("'", " "));
        return "%" + alpha.replaceAll(" ", "%") + "%";
    }
}
