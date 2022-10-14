package org.rickosborne.romance.db.model;

import lombok.Getter;
import lombok.NonNull;

import java.net.URL;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Getter
public enum RedditPostAttributes implements EnumSchemaAttribute<RedditPostModel> {
    authorName(RedditPostModel::getAuthorName, RedditPostModel::setAuthorName, String.class),
    body(RedditPostModel::getBody, RedditPostModel::setBody, String.class),
    id(RedditPostModel::getId, RedditPostModel::setId, String.class),
    url(RedditPostModel::getUrl, RedditPostModel::setUrl, URL.class),
    ;
    private final Function<RedditPostModel, ?> accessor;
    private final Class<Object> attributeType;
    private final Class<RedditPostModel> modelType = RedditPostModel.class;
    private final BiConsumer<RedditPostModel, Object> mutator;

    <T> RedditPostAttributes(
        @NonNull final Function<RedditPostModel, T> accessor,
        @NonNull final BiConsumer<RedditPostModel, T> mutator,
        @NonNull final Class<T> attributeType
    ) {
        this.accessor = accessor;
        @SuppressWarnings("unchecked") final BiConsumer<RedditPostModel, Object> typedMutator = (BiConsumer<RedditPostModel, Object>) mutator;
        this.mutator = typedMutator;
        @SuppressWarnings("unchecked") final Class<Object> objectAttributeType = (Class<Object>) attributeType;
        this.attributeType = objectAttributeType;
    }
}
