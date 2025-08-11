package org.rickosborne.romance.util;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class AMerger {
    /**
     * Returns the first non-null value.
     */
    @SafeVarargs
    public static <M, T> T coalesce(final Function<M, T> accessor, final M... models) {
        for (final M model : models) {
            if (model != null) {
                final T value = accessor.apply(model);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    @SafeVarargs
    public static <M, T> T coalesce(final Function<M, T> accessor, final Predicate<T> predicate, final M... models) {
        for (final M model : models) {
            if (model != null) {
                final T value = accessor.apply(model);
                if (value != null && predicate.test(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    @SafeVarargs
    public static <M, T> List<T> coalesceList(final Function<M, List<T>> accessor, final M... books) {
        List<T> result = null;
        for (final M book : books) {
            if (book != null) {
                final List<T> list = accessor.apply(book);
                if (list != null) {
                    if (!list.isEmpty()) {
                        return list;
                    }
                    result = list;
                }
            }
        }
        return result;
    }

    @SafeVarargs
    public static <M> String coalesceText(final Function<M, String> accessor, final M... models) {
        for (final M model : models) {
            if (model != null) {
                final String value = accessor.apply(model);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    public static boolean filterNonDate(final String date) {
        return date != null && !("0001-01-01T00:00:00".equals(date));
    }

    public static boolean nonEmpty(final String value) {
        return value != null && !value.isBlank();
    }

}
