package org.rickosborne.romance.util;

import org.junit.jupiter.api.Test;

import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.rickosborne.romance.util.DateStuff.fromMonthDayYear;

public class DateStuffTest {
    @Test
    public void fromMonthDayYearTest() {
        assertEquals("2003-03-15", fromMonthDayYear("March 15, 2003").format(DateTimeFormatter.ISO_LOCAL_DATE));
        assertEquals("1999-12-31", fromMonthDayYear("December 31 1999").format(DateTimeFormatter.ISO_LOCAL_DATE));
    }
}
