package org.rickosborne.romance.util;

public interface ThrowingBiConsumer<T, U, E extends Throwable> {
    void apply(final T t, final U u) throws E;
}
