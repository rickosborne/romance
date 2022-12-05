package org.rickosborne.romance.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Triple<L, M, R> {
    public static <L, M, R> Triple<L, M, R> build(
        final L left,
        final M middle,
        final R right
    ) {
        return new Triple<>(left, middle, right);
    }

    public static <L, M, R> Triple<L, M, R> build(
        final L left,
        final Pair<M, R> pair
    ) {
        return new Triple<>(left, pair == null ? null : pair.getLeft(), pair == null ? null : pair.getRight());
    }

    L left;
    M middle;
    R right;
}
