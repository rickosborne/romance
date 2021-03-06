package org.rickosborne.romance.util;

import lombok.NonNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.function.BiConsumer;

public interface ModelSetter<M> {
    static <M> void setNothing(final M model, final Object unused) {
    }

    default BiConsumer<M, Object> doubleSetter(@NonNull final BiConsumer<M, Double> setter) {
        return (m, v) -> {
            if (v instanceof Double) {
                setter.accept(m, (Double) v);
            } else if (v instanceof Integer) {
                setter.accept(m, ((Integer) v).doubleValue());
            } else if (v instanceof Long) {
                setter.accept(m, ((Long) v).doubleValue());
            } else if (v instanceof String) {
                final String s = (String) v;
                if (s.isBlank()) {
                    setter.accept(m, null);
                } else if (s.endsWith("%")) {
                    setter.accept(m, Double.parseDouble(s.replace("%", "")) * 0.01);
                } else if (StringStuff.isNumeric(s)) {
                    setter.accept(m, Double.parseDouble(s));
                }
            } else {
                throw new IllegalArgumentException("Cannot convert to Double " + v.getClass().getSimpleName() + ": " + v);
            }
        };
    }

    default <T> BiConsumer<M, T> ifNotNull(@NonNull final BiConsumer<M, T> setter) {
        return (m, v) -> {
            if (v != null) {
                setter.accept(m, v);
            }
        };
    }

    default BiConsumer<M, Object> intSetter(@NonNull final BiConsumer<M, Integer> setter) {
        return (m, v) -> {
            if (v instanceof Integer) {
                setter.accept(m, (Integer) v);
            } else if (v instanceof Long) {
                setter.accept(m, ((Long) v).intValue());
            } else if (v instanceof String) {
                final String s = (String) v;
                if (s.isBlank() || "-".equals(s)) {
                    setter.accept(m, null);
                } else {
                    setter.accept(m, Integer.parseInt(s.replaceAll(",", ""), 10));
                }
            } else {
                throw new IllegalArgumentException("Cannot convert to Integer " + v.getClass().getSimpleName() + ": " + v);
            }
        };
    }

    default BiConsumer<M, Object> localDateSetter(@NonNull final BiConsumer<M, LocalDate> setter) {
        return (m, v) -> {
            if (v instanceof LocalDate) {
                setter.accept(m, (LocalDate) v);
            } else if (v instanceof Instant) {
                setter.accept(m, LocalDate.ofInstant((Instant) v, ZoneOffset.UTC));
            } else if (v instanceof String) {
                final String s = (String) v;
                if (s.isBlank()) {
                    setter.accept(m, null);
                } else {
                    setter.accept(m, LocalDate.parse(s));
                }
            } else {
                throw new IllegalArgumentException("Cannot convert to LocalDate " + v.getClass().getSimpleName() + ": " + v);
            }
        };
    }

    default BiConsumer<M, Object> stringSetter(@NonNull final BiConsumer<M, String> setter) {
        return (m, v) -> {
            if (v instanceof String) {
                final String s = (String) v;
                if (s.isBlank()) {
                    setter.accept(m, null);
                } else {
                    setter.accept(m, s);
                }
            } else {
                throw new IllegalArgumentException("Cannot convert to String " + v.getClass().getSimpleName() + ": " + v);
            }
        };
    }

    default BiConsumer<M, Object> urlSetter(@NonNull final BiConsumer<M, URL> setter) {
        return (m, v) -> {
            if (v instanceof URL) {
                setter.accept(m, (URL) v);
            } else if (v instanceof String) {
                final String s = (String) v;
                if (s.isBlank()) {
                    setter.accept(m, null);
                } else {
                    try {
                        setter.accept(m, new URL(s));
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException("Not a URL: " + s, e);
                    }
                }
            }
        };
    }

}
