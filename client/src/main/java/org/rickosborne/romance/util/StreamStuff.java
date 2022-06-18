package org.rickosborne.romance.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class StreamStuff {
    public static <T> Function<T, Indexed<T>> indexed() {
        final AtomicInteger index = new AtomicInteger(0);
        return v -> new Indexed<>(index.getAndIncrement(), v);
    }

    public static <T> Function<T, T> firstOrElse(
        @NonNull final Function<T, T> onFirst,
        @NonNull final Function<T, T> onElse
    ) {
        @SuppressWarnings("unchecked") final Function<T, T>[] fn = (Function<T, T>[]) new Function[1];
        fn[0] = v -> {
            fn[0] = onElse;
            return onFirst.apply(v);
        };
        return v -> fn[0].apply(v);
    }

    @Getter
    @AllArgsConstructor
    public static class Indexed<T> {
        private final int index;
        private final T value;
    }
}
