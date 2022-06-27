package org.rickosborne.romance.util;

import lombok.Value;

@Value
public class Pair<L, R> {
    public static <L, R> Pair<L, R> build(final L left, final R right) {
        return new Pair<>(left, right);
    }

    L left;
    R right;

    public boolean hasLeft() {
        return left != null;
    }

    public boolean hasRight() {
        return right != null;
    }
}
