package org.rickosborne.romance.util;

public record Triple<L, M, R>(L left, M middle, R right) {
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
        return new Triple<>(left, pair == null ? null : pair.left(), pair == null ? null : pair.right());
    }
}
