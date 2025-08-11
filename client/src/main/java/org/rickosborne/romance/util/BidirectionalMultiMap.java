package org.rickosborne.romance.util;

import lombok.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BidirectionalMultiMap<LeftT, RightT, KeyT> {
    private final Function<LeftT, KeyT> leftIdentifier;
    private final Map<KeyT, LeftT> leftsByKey = new HashMap<>();
    private final Map<KeyT, List<KeyT>> leftsFromRightKey = new HashMap<>();
    private final Function<RightT, KeyT> rightIdentifier;
    private final Map<KeyT, RightT> rightsByKey = new HashMap<>();
    private final Map<KeyT, List<KeyT>> rightsFromLeftKey = new HashMap<>();
    private int size = 0;

    public BidirectionalMultiMap(
        @NonNull final Function<LeftT, KeyT> leftIdentifier,
        @NonNull final Function<RightT, KeyT> rightIdentifier
    ) {
        this.leftIdentifier = leftIdentifier;
        this.rightIdentifier = rightIdentifier;
    }

    public boolean add(final LeftT left, final RightT right) {
        if (left == null || right == null) {
            return false;
        }
        final Pair<KeyT, LeftT> leftPair = originalLeft(left);
        final Pair<KeyT, RightT> rightPair = originalRight(right);
        final KeyT leftKey = leftPair == null ? this.leftKey(left) : leftPair.getLeft();
        final KeyT rightKey = rightPair == null ? this.rightKey(right) : rightPair.getLeft();
        if (leftKey == null || rightKey == null) {
            return false;
        }
        final LeftT originalLeft = leftPair == null ? left : leftPair.getRight();
        final RightT originalRight = rightPair == null ? right : rightPair.getRight();
        if (originalLeft == null || originalRight == null) {
            return false;
        }
        leftsByKey.putIfAbsent(leftKey, originalLeft);
        rightsByKey.putIfAbsent(rightKey, originalRight);
        final List<KeyT> leftKeys = leftsFromRightKey.computeIfAbsent(rightKey, k -> new LinkedList<>());
        final List<KeyT> rightKeys = rightsFromLeftKey.computeIfAbsent(leftKey, k -> new LinkedList<>());
        boolean added = false;
        if (!leftKeys.contains(leftKey)) {
            leftKeys.add(leftKey);
            added = true;
        }
        if (!rightKeys.contains(rightKey)) {
            rightKeys.add(rightKey);
            added = true;
        }
        if (added) {
            size++;
        }
        return added;
    }

    public boolean contains(final LeftT left, final RightT right) {
        final Pair<KeyT, LeftT> leftPair = originalLeft(left);
        final Pair<KeyT, RightT> rightPair = originalRight(right);
        if (leftPair == null || rightPair == null) {
            return false;
        }
        final KeyT leftKey = leftPair.getLeft();
        final KeyT rightKey = rightPair.getLeft();
        final List<KeyT> rights = rightsFromLeftKey.get(leftKey);
        final List<KeyT> lefts = leftsFromRightKey.get(rightKey);
        if (rights == null || lefts == null) {
            return false;
        }
        return lefts.contains(rightKey) && rights.contains(leftKey);
    }

    public boolean containsLeft(final LeftT left) {
        final KeyT key = leftKey(left);
        if (key == null) {
            return false;
        }
        final LeftT original = leftsByKey.get(key);
        return original != null;
    }

    public boolean containsLeftKey(final KeyT key) {
        if (key == null) {
            return false;
        }
        final LeftT original = leftsByKey.get(key);
        return original != null;
    }

    public boolean containsRight(final RightT right) {
        final KeyT key = rightKey(right);
        if (key == null) {
            return false;
        }
        final RightT original = rightsByKey.get(key);
        return original != null;
    }

    public boolean containsRightKey(final KeyT key) {
        if (key == null) {
            return false;
        }
        final RightT original = rightsByKey.get(key);
        return original != null;
    }

    public Collection<LeftT> getLefts() {
        return leftsByKey.values();
    }

    public Collection<RightT> getRights() {
        return rightsByKey.values();
    }

    public KeyT leftKey(final LeftT left) {
        if (left == null) {
            return null;
        }
        return this.leftIdentifier.apply(left);
    }

    public int leftSize() {
        return this.leftsByKey.size();
    }

    public List<LeftT> leftsFor(final RightT right) {
        return sideFor(right, this::originalRight, rightsFromLeftKey, leftsByKey);
    }

    public Pair<KeyT, LeftT> originalLeft(final LeftT left) {
        if (left == null) {
            return null;
        }
        final KeyT key = leftKey(left);
        if (key == null) {
            return null;
        }
        final LeftT original = leftsByKey.get(key);
        if (original == null) {
            return null;
        }
        return new Pair<>(key, left);
    }

    public Pair<KeyT, RightT> originalRight(final RightT right) {
        if (right == null) {
            return null;
        }
        final KeyT key = rightKey(right);
        if (key == null) {
            return null;
        }
        final RightT original = rightsByKey.get(key);
        if (original == null) {
            return null;
        }
        return new Pair<>(key, right);
    }

    public KeyT rightKey(final RightT right) {
        if (right == null) {
            return null;
        }
        return this.rightIdentifier.apply(right);
    }

    public int rightSize() {
        return this.rightsByKey.size();
    }

    public List<RightT> rightsFor(final LeftT left) {
        return sideFor(left, this::originalLeft, rightsFromLeftKey, rightsByKey);
    }

    private <T, U> List<U> sideFor(
        final T t,
        final Function<T, Pair<KeyT, T>> originalSide,
        final Map<KeyT, List<KeyT>> othersFromKey,
        final Map<KeyT, U> otherByKey
    ) {
        if (t == null) {
            return Collections.emptyList();
        }
        final Pair<KeyT, T> pair = originalSide.apply(t);
        if (pair == null) {
            return Collections.emptyList();
        }
        final KeyT tKey = pair.getLeft();
        final List<KeyT> uKeys = othersFromKey.get(tKey);
        if (uKeys == null) {
            return Collections.emptyList();
        }
        return uKeys.stream()
            .map(otherByKey::get)
            .collect(Collectors.toList());
    }

    public int size() {
        return size;
    }
}
