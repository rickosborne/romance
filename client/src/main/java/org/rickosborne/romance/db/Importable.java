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

    void importFrom(final M other);
}
