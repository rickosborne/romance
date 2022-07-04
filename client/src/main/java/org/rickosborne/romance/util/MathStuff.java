package org.rickosborne.romance.util;

import java.time.Duration;

public class MathStuff {
    public static Double doubleFromDuration(final String duration) {
        final long minutes = Duration.parse(duration).toMinutes();
        return round(minutes / 60d, 0.01d);
    }

    public static double round(final double unrounded, final double places) {
        return Math.round(unrounded / places) * places;
    }
}
