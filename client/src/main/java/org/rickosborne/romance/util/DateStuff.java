package org.rickosborne.romance.util;

import org.rickosborne.romance.client.html.English;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateStuff {

    public static final Pattern MONTH_DAY_YEAR_PATTERN = Pattern.compile("^(?<mo>\\w+)\\s+(?<day>[0-9]+),?\\s+(?<year>[0-9]+)$");

    public static LocalDate fromMonthDayYear(final String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        final Matcher matcher = MONTH_DAY_YEAR_PATTERN.matcher(text);
        if (matcher.find()) {
            final int mo = English.MONTHS.indexOf(matcher.group("mo"));
            final int day = Integer.parseInt(matcher.group("day"), 10);
            final int year = Integer.parseInt(matcher.group("year"), 10);
            return LocalDate.of(year, mo, day);
        }
        return null;
    }
}
