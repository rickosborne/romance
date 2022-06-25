package org.rickosborne.romance.db.model;

import lombok.NonNull;

import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

public enum NarratorAttributes implements SchemaAttribute<NarratorModel, Object> {
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
    private final Class<?> attributeType;
    private final BiConsumer<NarratorModel, Object> mutator;

    <T> NarratorAttributes(final Function<NarratorModel, T> accessor, final BiConsumer<NarratorModel, T> mutator, final Class<T> attributeType) {
        this.accessor = accessor;
        @SuppressWarnings("unchecked") final BiConsumer<NarratorModel, Object> typedMutator = (BiConsumer<NarratorModel, Object>) mutator;
        this.mutator = typedMutator;
        this.attributeType = attributeType;
    }

    public Object getAttribute(@NonNull final NarratorModel model) {
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
    public Class<NarratorModel> getModelType() {
        return NarratorModel.class;
    }

    public void setAttribute(@NonNull final NarratorModel model, final Object value) {
        if (mutator == null) {
            throw new NullPointerException("NarratorModel cannot set " + name());
        }
        if ((value != null) && !attributeType.isInstance(value)) {
            throw new IllegalArgumentException(String.format("Expected %s but found %s", attributeType.getSimpleName(), value.getClass().getSimpleName()));
        }
        mutator.accept(model, value);
    }

}
