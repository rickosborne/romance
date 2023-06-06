package org.rickosborne.romance.db.model;

import lombok.Getter;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Getter
public enum TagAttributes implements EnumSchemaAttribute<TagModel> {
    description(TagModel::getDescription, TagModel::setDescription, String.class),
    name(TagModel::getName, TagModel::setName, String.class),
    ownedCount(TagModel::getOwnedCount, TagModel::setOwnedCount, Integer.class),
    positiveDurationHours(TagModel::getPositiveDurationHours, TagModel::setPositiveDurationHours, Double.class),
    positiveLikelihood(TagModel::getPositiveLikelihood, TagModel::setPositiveLikelihood, Double.class),
    positiveRate(TagModel::getPositiveRate, TagModel::setPositiveRate, Double.class),
    ratedCount(TagModel::getRatedCount, TagModel::setRatedCount, Integer.class),
    ratedDurationHours(TagModel::getRatedDurationHours, TagModel::setRatedDurationHours, Double.class),
    ratings(TagModel::getRatings, null, Map.class),
    fiveStarCount(TagModel::getFiveStarCount, TagModel::setFiveStarCount, Integer.class),
    ;
    private final Function<TagModel, ?> accessor;
    private final Class<Object> attributeType;
    private final Class<TagModel> modelType = TagModel.class;
    private final BiConsumer<TagModel, Object> mutator;

    <T> TagAttributes(final Function<TagModel, T> accessor, final BiConsumer<TagModel, T> mutator, final Class<T> attributeType) {
        this.accessor = accessor;
        @SuppressWarnings("unchecked") final BiConsumer<TagModel, Object> typedMutator = (BiConsumer<TagModel, Object>) mutator;
        this.mutator = typedMutator;
        @SuppressWarnings("unchecked") final Class<Object> objectAttributeType = (Class<Object>) attributeType;
        this.attributeType = objectAttributeType;
    }
}
