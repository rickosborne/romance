package org.rickosborne.romance.util;

import java.util.Set;

public enum YesNoUnknown {
    Yes("Y", "YES"),
    No("N", "NO"),
    Unknown("?", "N?"),
    ;

    public static YesNoUnknown fromString(final String s) {
        if (s == null) {
            return null;
        }
        for (final YesNoUnknown value : values()) {
            if (value.abbreviations.contains(s.toUpperCase())) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown Yes/No/Unknown value: " + s);
    }

    private final Set<String> abbreviations;
    private final String out;

    YesNoUnknown(final String... abbreviations) {
        this.abbreviations = Set.of(abbreviations);
        this.out = abbreviations[0];
    }

    @Override
    public String toString() {
        return out;
    }
}
