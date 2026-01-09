package org.rickosborne.romance.util;

public record Pair<L, R>(L left, R right) {
    public static <L, R> Pair<L, R> build(final L left, final R right) {
        return new Pair<>(left, right);
    }

    public boolean hasLeft() {
        return left != null;
    }

    public boolean hasRight() {
        return right != null;
    }
}
