package org.rickosborne.romance.db.model;

import lombok.NonNull;

import java.net.URL;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public enum SeriesAttributes implements SchemaAttribute<SeriesModel, Object> {
    goodreadsUrl(SeriesModel::getGoodreadsUrl, SeriesModel::setGoodreadsUrl, URL.class),
    name(SeriesModel::getName, SeriesModel::setName, String.class),
    ownedCount(SeriesModel::getOwnedCount, SeriesModel::setOwnedCount, Integer.class),
    ratings(SeriesModel::getRatings, null, Map.class),
    ;
    private final Function<SeriesModel, ?> accessor;
    private final Class<?> attributeType;
    private final BiConsumer<SeriesModel, Object> mutator;

    <T> SeriesAttributes(final Function<SeriesModel, T> accessor, final BiConsumer<SeriesModel, T> mutator, final Class<T> attributeType) {
        this.accessor = accessor;
        @SuppressWarnings("unchecked") final BiConsumer<SeriesModel, Object> typedMutator = (BiConsumer<SeriesModel, Object>) mutator;
        this.mutator = typedMutator;
        this.attributeType = attributeType;
    }

    public Object getAttribute(@NonNull final SeriesModel model) {
        return accessor.apply(model);
    }

    @Override
    public String getAttributeName() {
        return name();
    }

    @Override
    public Class<Object> getAttributeType() {
        @SuppressWarnings("unchecked") final Class<Object> typed = (Class<Object>) attributeType;
        return typed;
    }

    @Override
    public Class<SeriesModel> getModelType() {
        return SeriesModel.class;
    }

    public void setAttribute(@NonNull final SeriesModel model, final Object value) {
        if (mutator == null) {
            throw new NullPointerException("SeriesModel cannot set " + name());
        }
        if ((value != null) && !attributeType.isInstance(value)) {
            throw new IllegalArgumentException(String.format("Expected %s but found %s", attributeType.getSimpleName(), value.getClass().getSimpleName()));
        }
        mutator.accept(model, value);
    }
}
