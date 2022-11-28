package org.rickosborne.romance.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.function.Supplier;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Setter
public final class Mutable<T> {
    public static <T> Mutable<T> empty() {
        return new Mutable<>(null);
    }

    public static <T> Mutable<T> of(final T item) {
        return new Mutable<>(item);
    }

    T item;

    public T getOrSet(@NonNull final Supplier<T> itemSupplier) {
        if (item == null) {
            item = itemSupplier.get();
        }
        return item;
    }

    public boolean isEmpty() {
        return item == null;
    }
}
