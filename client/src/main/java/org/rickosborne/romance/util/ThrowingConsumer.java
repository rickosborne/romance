package org.rickosborne.romance.util;

public interface ThrowingConsumer<T, E extends Throwable> {
    void accept(final T item) throws E;
}
