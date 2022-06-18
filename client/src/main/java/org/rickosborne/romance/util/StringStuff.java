package org.rickosborne.romance.util;

public class StringStuff {
    public static final String[] FRACTIONS = new String[]{"", "¼", "½", "¾"};

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

    public static String ucFirst(final String text) {
        if (text == null) {
            return null;
        } else if (text.isBlank() || text.length() < 2) {
            return text.toUpperCase();
        } else {
            return text.substring(0, 1).toUpperCase() + text.substring(1);
        }
    }
}
