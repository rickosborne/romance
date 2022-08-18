package org.rickosborne.romance.db;

import lombok.NonNull;

import java.util.function.Consumer;

public interface Importable<M> {
    static <T> void setIf(
        final T value,
        @NonNull final Consumer<T> mutator
    ) {
        if (value != null) {
            mutator.accept(value);
        }
    }

    static <T> void setIfNotNull(
        final T oldValue,
        final T newValue,
        @NonNull final Consumer<T> mutator
    ) {
        if (newValue != null && oldValue == null) {
            mutator.accept(newValue);
        }
    }

    void importFrom(final M other);

    void importFromIfNotNull(final M other);
}
