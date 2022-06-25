package org.rickosborne.romance.db.model;

import lombok.NonNull;

public interface SchemaAttribute<M, A> {
    A getAttribute(@NonNull final M model);

    String getAttributeName();

    Class<A> getAttributeType();

    Class<M> getModelType();

    void setAttribute(@NonNull final M model, final A value);
}
