package org.rickosborne.romance.util;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.text.StringEscapeUtils.unescapeHtml4;


@Slf4j
public class StringStuff {
    public static final Set<String> BLOCKED_NAMES = Set.of(
        "Narrator Info Added Soon",
        "Narrator",
        "MS",
        "PhD"
    );
    public static final List<Pattern> BLOCKED_NAME_PATTERNS = List.of(
        Pattern.compile("^More by "),
        Pattern.compile("^(Dr|Mr|Ms|Mrs)\\.?\\s*"),
        Pattern.compile(",(\\s*(MS|PhD))+$")
    );
    public static final Pattern BOOLEAN = Pattern.compile("^(?:true|false)$", Pattern.CASE_INSENSITIVE);
    public static final String CRLF = "\n";
    public static final int FILE_NAME_MAX_LENGTH = 150;
    public static final String[] FRACTIONS = new String[]{"", "¼", "½", "¾"};
    public static final Pattern ISO_DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    public static final Pattern NUMERIC = Pattern.compile("^[\\d,]+(?:\\.\\d*)?$");
    public static final String QUOTE_CHAR_CLASS = "['’`\"]";
    public static final List<String> WHITESPACE = List.of(" ", "\t", "\r", "\n");

    public static String alphaOnly(final String s) {
        if (s == null) {
            return null;
        }
        return s
            .toLowerCase()
            .replaceAll(QUOTE_CHAR_CLASS, "")
            .replaceAll("\\b(a|an|the)\\b", "")
            .replaceAll("[^a-z\\d]+", " ")
            .trim();
    }

    public static Pattern asPattern(final String text) {
        return Pattern.compile(Stream.of(text.split("\\s+"))
            .map((String word) -> Pattern.quote(word.toLowerCase()))
            .collect(Collectors.joining("\\s+", "(?i)", "")), Pattern.CASE_INSENSITIVE);
    }

    public static String cacheName(@NonNull final URL url) {
        return String.join("-",
            DigestUtils.sha256Hex(url.toString()).substring(0, 8),
            url.getHost().replace("www.", ""),
            url.getPath().replaceAll("\\W+", "-")
        );
    }

    public static String coalesceNonBlank(final String... items) {
        for (final String item : items) {
            if (item != null && !item.isBlank()) {
                return item;
            }
        }
        return null;
    }

    public static String ellipsize(final String s, final int maxLen) {
        return s == null ? null : s.length() > maxLen ? s.substring(0, maxLen) : s;
    }

    public static String ensureToken(final String needle, final String haystack) {
        if (needle == null) {
            return haystack;
        } else if (haystack == null || haystack.isBlank()) {
            return needle;
        } else if (haystack.contains(needle)) {
            return haystack;
        } else {
            return (haystack + needle).trim();
        }
    }

    public static int firstWhitespace(final String s) {
        if (s == null) {
            return -1;
        }
        int ws = -1;
        for (final String w : WHITESPACE) {
            final int at = s.indexOf(w);
            if (at > -1 && (ws == -1 || ws > at)) {
                ws = at;
            }
        }
        return ws;
    }

    public static String firstWordOf(final String s) {
        if (s == null || s.isBlank()) {
            return s;
        }
        final String t = s.trim();
        final int ws = firstWhitespace(t);
        if (ws > 0) {
            return t.substring(0, ws);
        }
        return t;
    }

    public static String formatMS(final Integer ms) {
        if (ms == null) {
            return "";
        }
        int rem = ms;
        final int msec = rem % 1000;
        rem /= 1000;
        final int sec = rem % 60;
        rem /= 60;
        final int min = rem % 60;
        final int hour = rem / 60;
        String f = "";
        if (hour > 0) {
            return zeroPad(hour, 2) + ":" + zeroPad(min, 2) + ":" + zeroPad(sec, 2);
        } else if (min > 0) {
            return zeroPad(min, 2) + ":" + zeroPad(sec, 2);
        } else {
            return sec + "." + zeroPad(msec, 3);
        }
    }

    public static boolean fuzzyListMatch(final String a, final String b) {
        if (a == null || b == null) {
            return false;
        }
        for (final String aItem : a.split(",")) {
            for (final String bItem : b.split(",")) {
                if (nonBlank(aItem) && nonBlank(bItem) && fuzzyMatch(aItem, bItem)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean fuzzyMatch(final String a, final String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.equals(b)) {
            return true;
        }
        final String aa = alphaOnly(a);
        final String bb = alphaOnly(b);
        return Objects.equals(aa, bb)
            || ((a.contains(":") || a.contains(",")) && aa.startsWith(bb))
            || ((b.contains(":") || b.contains(",")) && bb.startsWith(aa));
    }

    public static boolean isBoolean(final String s) {
        return s != null && BOOLEAN.matcher(s).matches();
    }

    public static boolean isDate(final String s) {
        return s != null && ISO_DATE.matcher(s).matches();
    }

    public static boolean isNumeric(final String s) {
        return s != null && NUMERIC.matcher(s).matches();
    }

    public static boolean isSorted(@NonNull final List<String> list) {
        if (list.isEmpty()) {
            return true;
        }
        String last = null;
        for (final String next : list) {
            if (next == null) {
                continue;
            }
            if (last != null && last.compareTo(next) > 0) {
                return false;
            }
            last = next;
        }
        return true;
    }

    public static String joinSorted(final String delim, final Collection<String> set) {
        if (set == null || set.isEmpty()) {
            return null;
        }
        return set.stream().sorted().collect(Collectors.joining(delim));
    }

    public static String noLongerThan(final int maxLength, final String s) {
        return s == null || s.length() <= maxLength ? s : s.substring(0, maxLength);
    }

    public static boolean nonBlank(final String t) {
        return t != null && !t.isBlank();
    }

    public static String normalizeNames(final String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return splitNames(name)
            .collect(Collectors.joining(", "));
    }

    public static String nullIfBlank(final String s) {
        return s == null || s.isBlank() ? null : s;
    }

    public static Map<String, String> pairMap(final String... items) {
        final Map<String, String> map = new HashMap<>();
        for (int i = 1; i < items.length; i += 2) {
            map.put(items[i - 1], items[i]);
        }
        return map;
    }

    public static String removeToken(final String needle, final String haystack) {
        if (needle == null || haystack == null || haystack.isBlank()) {
            return haystack;
        } else if (haystack.contains(needle)) {
            return haystack.replace(needle, "").trim();
        } else {
            return haystack;
        }
    }

    @SafeVarargs
    public static <T, V> BiConsumer<T, V> setButNot(final BiConsumer<T, V> downstream, @NonNull final V... avoid) {
        return (t, v) -> {
            if (v == null) {
                return;
            }
            for (final V a : avoid) {
                if (Objects.equals(v, a)) {
                    return;
                }
            }
            downstream.accept(t, v);
        };
    }

    public static List<String> sortIfNeeded(final List<String> list) {
        if (list != null && !isSorted(list)) {
            return list.stream().sorted().collect(Collectors.toList());
        }
        return list;
    }

    public static Stream<String> splitNames(final String name) {
        if (name == null || name.isBlank()) {
            return Stream.empty();
        }
        return Stream.of(name.split("\\s*(?:\\s+and|&|;|,)\\s+"))
            .distinct()
            .sorted()
            .map((o) -> {
                String n = o.trim();
                for (final Pattern pattern : BLOCKED_NAME_PATTERNS) {
                    n = pattern.matcher(n).replaceAll("").trim();
                }
                return n;
            })
            .filter(n -> !BLOCKED_NAMES.contains(n) && !n.isBlank());
    }

    public static String starsFromNumber(final Double num) {
        if (num == null) {
            return null;
        }
        final int whole = num.intValue();
        final double frac = num - whole;
        final String fracPart = FRACTIONS[(int) (Math.round(frac * 4) / 4)];
        return whole + fracPart + "⭐️/5";
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

    public static String titleCase(final String s) {
        if (s == null || s.isBlank()) {
            return s;
        }
        final String t = s.trim();
        final boolean needsFix = (t.toLowerCase().equals(t) || t.toUpperCase().equals(t));
        if (!needsFix) {
            return t;
        }
        return Arrays.stream(t.split("\\s+"))
            .map(word -> {
                if (word.length() < 2) {
                    return word.toUpperCase();
                }
                return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
            })
            .collect(Collectors.joining(" "));
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

    public static String unescape(final String text) {
        return unescapeHtml4(text);
    }

    public static URI uriFromString(final String uri) {
        if (uri == null || uri.isBlank()) {
            return null;
        }
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
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

    public static URL urlFromString(final String url, final String base) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            if (url.startsWith("http")) {
                return new URL(url);
            }
            if (url.startsWith("/")) {
                return new URL(base + url);
            }
            log.warn("Unknown URL format: '{}' + base '{}'", url, base);
            return null;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String zeroPad(final int num, final int width) {
        final String s = String.valueOf(num);
        final int zeros = width - s.length();
        if (zeros < 1) {
            return s;
        }
        return "0".repeat(zeros) + s;
    }
}
