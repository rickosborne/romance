package org.rickosborne.romance.db.model;

import lombok.NonNull;

public interface SchemaAttribute<M, A> {
    default A chooseAttributeValue(final A left, final A right) {
        if (right == null) {
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
