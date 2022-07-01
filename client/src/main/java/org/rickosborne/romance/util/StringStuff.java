package org.rickosborne.romance.util;

import lombok.NonNull;
import org.apache.commons.codec.digest.DigestUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class StringStuff {
    public static final String CRLF = "\n";
    public static final String[] FRACTIONS = new String[]{"", "¼", "½", "¾"};
    public static final Pattern BOOLEAN = Pattern.compile("^(?:true|false)$", Pattern.CASE_INSENSITIVE);
    public static final Pattern NUMERIC = Pattern.compile("^[\\d,]+(?:\\.\\d*)?$");

    public static String cacheName(@NonNull final URL url) {
        return String.join("-",
            DigestUtils.sha256Hex(url.toString()).substring(0, 8),
            url.getHost().replace("www.", ""),
            url.getPath().replaceAll("\\W+", "-")
        );
    }

    public static boolean isBoolean(final String s) {
        return s != null && BOOLEAN.matcher(s).matches();
    }

    public static boolean isNumeric(final String s) {
        return s != null && NUMERIC.matcher(s).matches();
    }

    public static boolean nonBlank(final String t) {
        return t != null && !t.isBlank();
    }

    public static String starsFromNumber(final Double num) {
        if (num == null) {
            return null;
        }
        final int whole = num.intValue();
        final double frac = num - whole;
        final String fracPart = FRACTIONS[(int) (Math.round(frac * 4) / 4)];
        return String.valueOf(whole) + fracPart + "⭐️/5";
    }

    public static String stringify(final Object o) {
        if (o == null) {
            return null;
        } else if (o instanceof String) {
            return (String) o;
        } else {
            throw new IllegalArgumentException("Cannot stringify " + o.getClass().getSimpleName() + ": " + o);
        }
    }

    public static LocalDate toLocalDate(final String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return LocalDate.parse(s);
    }

    public static LocalDate toLocalDateFromMDY(final String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return LocalDate.parse(s, DateTimeFormatter.ofPattern("M/d/y"));
    }

    public static String ucFirst(final String text) {
        if (text == null) {
            return null;
        } else if (text.isBlank() || text.length() < 2) {
            return text.toUpperCase();
        } else {
            return text.substring(0, 1).toUpperCase() + text.substring(1);
        }
    }

    public static URL urlFromString(final String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
