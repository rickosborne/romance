package org.rickosborne.romance.db.model;

import lombok.NonNull;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public enum TagAttributes implements SchemaAttribute<TagModel, Object> {
    description(TagModel::getDescription, TagModel::setDescription, String.class),
    name(TagModel::getName, TagModel::setName, String.class),
    ownedCount(TagModel::getOwnedCount, TagModel::setOwnedCount, Integer.class),
    positiveDurationHours(TagModel::getPositiveDurationHours, TagModel::setPositiveDurationHours, Double.class),
    positiveLikelihood(TagModel::getPositiveLikelihood, TagModel::setPositiveLikelihood, Double.class),
    positiveRate(TagModel::getPositiveRate, TagModel::setPositiveRate, Double.class),
    ratedCount(TagModel::getRatedCount, TagModel::setRatedCount, Integer.class),
    ratedDurationHours(TagModel::getRatedDurationHours, TagModel::setRatedDurationHours, Double.class),
    ratings(TagModel::getRatings, null, Map.class),
    ;
    private final Function<TagModel, ?> accessor;
    private final Class<?> attributeType;
    private final BiConsumer<TagModel, Object> mutator;

    <T> TagAttributes(final Function<TagModel, T> accessor, final BiConsumer<TagModel, T> mutator, final Class<T> attributeType) {
        this.accessor = accessor;
        @SuppressWarnings("unchecked") final BiConsumer<TagModel, Object> typedMutator = (BiConsumer<TagModel, Object>) mutator;
        this.mutator = typedMutator;
        this.attributeType = attributeType;
    }

    public Object getAttribute(@NonNull final TagModel model) {
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
    public Class<TagModel> getModelType() {
        return TagModel.class;
    }

    public void setAttribute(@NonNull final TagModel model, final Object value) {
        if (mutator == null) {
            throw new NullPointerException("TagModel cannot set " + name());
        }
        if ((value != null) && !attributeType.isInstance(value)) {
            throw new IllegalArgumentException(String.format("Expected %s but found %s", attributeType.getSimpleName(), value.getClass().getSimpleName()));
        }
        mutator.accept(model, value);
    }
}
