package org.rickosborne.romance.db.model;

import lombok.Getter;

import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Getter
public enum NarratorAttributes implements EnumSchemaAttribute<NarratorModel> {
    accent(NarratorModel::getAccent, NarratorModel::setAccent, String.class),
    goodreadsUrl(NarratorModel::getGoodreadsUrl, NarratorModel::setGoodreadsUrl, URL.class),
    name(NarratorModel::getName, NarratorModel::setName, String.class),
    negatives(NarratorModel::getNegatives, null, Set.class),
    ownedCount(NarratorModel::getOwnedCount, NarratorModel::setOwnedCount, Integer.class),
    ratings(NarratorModel::getRatings, null, Map.class),
    siteUrl(NarratorModel::getSiteUrl, NarratorModel::setSiteUrl, URL.class),
    totalDurationHours(NarratorModel::getTotalDurationHours, NarratorModel::setTotalDurationHours, Double.class),
    totalPages(NarratorModel::getTotalPages, NarratorModel::setTotalPages, Integer.class),
    twitterUrl(NarratorModel::getTwitterUrl, NarratorModel::setTwitterUrl, URL.class),
    ;
    private final Function<NarratorModel, ?> accessor;
    private final Class<Object> attributeType;
    private final Class<NarratorModel> modelType = NarratorModel.class;
    private final BiConsumer<NarratorModel, Object> mutator;

    <T> NarratorAttributes(final Function<NarratorModel, T> accessor, final BiConsumer<NarratorModel, T> mutator, final Class<T> attributeType) {
        this.accessor = accessor;
        @SuppressWarnings("unchecked") final BiConsumer<NarratorModel, Object> typedMutator = (BiConsumer<NarratorModel, Object>) mutator;
        this.mutator = typedMutator;
        @SuppressWarnings("unchecked") final Class<Object> objectAttributeType = (Class<Object>) attributeType;
        this.attributeType = objectAttributeType;
    }
}
