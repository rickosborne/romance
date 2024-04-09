package org.rickosborne.romance.util;

import static org.fusesource.jansi.Ansi.ansi;

public class ConsoleStuff {
    public static String updated(final String text) {
        return ansi().fgMagenta().a(text).reset().toString();
    }

    public static String warn(final String text) {
        return ansi().fgYellow().a(text).reset().toString();
    }
}
