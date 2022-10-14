package org.rickosborne.romance.db.model;

import lombok.NonNull;

import java.util.function.BiConsumer;
import java.util.function.Function;

public interface EnumSchemaAttribute<M> extends SchemaAttribute<M, Object> {
    Function<M, ?> getAccessor();

    @Override
    default Object getAttribute(@NonNull final M model) {
        return getAccessor().apply(model);
    }

    @Override
    default String getAttributeName() {
        return name();
    }

    BiConsumer<M, Object> getMutator();

    String name();

    @Override
    default void setAttribute(@NonNull final M model, final Object value) {
        final BiConsumer<M, Object> mutator = getMutator();
        if (mutator == null) {
            throw new NullPointerException(getModelType().getSimpleName() + " cannot set " + name());
        }
        final Class<Object> attributeType = getAttributeType();
        if ((value != null) && !attributeType.isInstance(value)) {
            throw new IllegalArgumentException(String.format("Expected %s but found %s", attributeType.getSimpleName(), value.getClass().getSimpleName()));
        }
        mutator.accept(model, value);
    }
}
