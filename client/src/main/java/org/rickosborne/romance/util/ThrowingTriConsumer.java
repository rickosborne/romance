package org.rickosborne.romance.util;

public interface ThrowingTriConsumer<T, U, V, E extends Throwable> {
    void accept(final T t, final U u, final V v) throws E;
}
