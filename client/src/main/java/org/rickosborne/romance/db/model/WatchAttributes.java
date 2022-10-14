package org.rickosborne.romance.db.model;

import lombok.Getter;

import java.net.URL;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Getter
public enum WatchAttributes implements EnumSchemaAttribute<WatchModel> {
    authorName(WatchModel::getAuthorName, WatchModel::setAuthorName, String.class),
    bookTitle(WatchModel::getBookTitle, WatchModel::setBookTitle, String.class),
    goodreadsUrl(WatchModel::getGoodreadsUrl, WatchModel::setGoodreadsUrl, URL.class),
    ;
    private final Function<WatchModel, ?> accessor;
    private final Class<Object> attributeType;
    private final Class<WatchModel> modelType = WatchModel.class;
    private final BiConsumer<WatchModel, Object> mutator;

    <T> WatchAttributes(final Function<WatchModel, T> accessor, final BiConsumer<WatchModel, T> mutator, final Class<T> attributeType) {
        this.accessor = accessor;
        @SuppressWarnings("unchecked") final BiConsumer<WatchModel, Object> typedMutator = (BiConsumer<WatchModel, Object>) mutator;
        this.mutator = typedMutator;
        @SuppressWarnings("unchecked") final Class<Object> objectAttributeType = (Class<Object>) attributeType;
        this.attributeType = objectAttributeType;
    }
}
