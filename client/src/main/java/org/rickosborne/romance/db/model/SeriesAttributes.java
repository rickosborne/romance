package org.rickosborne.romance.db.model;

import lombok.Getter;

import java.net.URL;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Getter
public enum SeriesAttributes implements EnumSchemaAttribute<SeriesModel> {
    audiobookStoreUrl(SeriesModel::getAudiobooksStoreUrl, SeriesModel::setAudiobooksStoreUrl, URL.class),
    goodreadsUrl(SeriesModel::getGoodreadsUrl, SeriesModel::setGoodreadsUrl, URL.class),
    storyGraphUrl(SeriesModel::getStoryGraphUrl, SeriesModel::setStoryGraphUrl, URL.class),
    name(SeriesModel::getName, SeriesModel::setName, String.class),
    ownedCount(SeriesModel::getOwnedCount, SeriesModel::setOwnedCount, Integer.class),
    ratings(SeriesModel::getRatings, null, Map.class),
    ;
    private final Function<SeriesModel, ?> accessor;
    private final Class<Object> attributeType;
    private final Class<SeriesModel> modelType = SeriesModel.class;
    private final BiConsumer<SeriesModel, Object> mutator;

    <T> SeriesAttributes(final Function<SeriesModel, T> accessor, final BiConsumer<SeriesModel, T> mutator, final Class<T> attributeType) {
        this.accessor = accessor;
        @SuppressWarnings("unchecked") final BiConsumer<SeriesModel, Object> typedMutator = (BiConsumer<SeriesModel, Object>) mutator;
        this.mutator = typedMutator;
        @SuppressWarnings("unchecked") final Class<Object> objectAttributeType = (Class<Object>) attributeType;
        this.attributeType = objectAttributeType;
    }
}
