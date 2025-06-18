package org.rickosborne.romance.util;

import java.time.Duration;

public class MathStuff {
    public static boolean closeEnough(final Double a, final Double b) {
        return (a == null && b == null)
            || (a != null && b != null && Math.abs(a - b) < 0.000001);
    }

    public static Double doubleFromDuration(final String duration) {
        String text = duration;
        if (!text.startsWith("PT")) {
            if (text.startsWith("P")) {
                text = "PT" + text.substring(1);
            } else {
                text = "PT" + text;
            }
        }
        final long minutes = Duration.parse(text).toMinutes();
        return round(minutes / 60d, 0.01d);
    }

    public static Double fourPlaces(final Double value) {
        return value == null ? null : round(value, 0.0001);
    }

    public static double fourPlaces(final double value) {
        return round(value, 0.0001);
    }

    public static boolean intIsh(final double value) {
        return closeEnough(value, (double) Math.round(value));
    }

    public static double round(final double unrounded, final double places) {
        return Math.round(unrounded / places) * places;
    }

    public static Double twoPlaces(final Double value) {
        return value == null ? null : round(value, 0.01);
    }

    public static double twoPlaces(final double value) {
        return round(value, 0.01);
    }
}
