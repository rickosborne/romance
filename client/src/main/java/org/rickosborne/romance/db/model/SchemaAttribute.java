package org.rickosborne.romance.db.model;

import lombok.NonNull;

import java.time.LocalDate;

public interface SchemaAttribute<M, A> {
    static LocalDate earlier(final LocalDate a, final LocalDate b) {
        return a == null ? b : b == null ? a : b.isBefore(a) ? b : a;
    }

    static LocalDate earlierSameYear(final LocalDate a, final LocalDate b) {
        return a == null ? b : b == null ? a : b.isBefore(a) && b.getYear() == a.getYear() ? b : a;
    }

    static <T> T keepIfNotNull(final T a, final T b) {
        return a == null ? b : a;
    }

    default A chooseAttributeValue(final A left, final A right) {
        if (right == null || (right instanceof String && ((String) right).isBlank())) {
            return left;
        } else {
            return right;
        }
    }

    A getAttribute(@NonNull final M model);

    String getAttributeName();

    Class<A> getAttributeType();

    Class<M> getModelType();

    void setAttribute(@NonNull final M model, final A value);
}
