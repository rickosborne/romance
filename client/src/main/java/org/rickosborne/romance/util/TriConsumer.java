package org.rickosborne.romance.util;

public interface TriConsumer<T, U, V> {
    void accept(T t, U u, V v);
}
