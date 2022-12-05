package org.rickosborne.romance.util;

public interface ThrowingFunction<T, U, E extends Throwable> {
    U apply(final T item) throws E;
}
