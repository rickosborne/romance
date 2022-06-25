package org.rickosborne.romance.db.model;

import lombok.NonNull;

import java.net.URL;
import java.util.function.BiConsumer;
import java.util.function.Function;

public enum WatchAttributes implements SchemaAttribute<WatchModel, Object> {
    authorName(WatchModel::getAuthorName, WatchModel::setAuthorName, String.class),
    bookTitle(WatchModel::getBookTitle, WatchModel::setBookTitle, String.class),
    goodreadsUrl(WatchModel::getGoodreadsUrl, WatchModel::setGoodreadsUrl, URL.class),
    ;
    private final Function<WatchModel, ?> accessor;
    private final Class<?> attributeType;
    private final BiConsumer<WatchModel, Object> mutator;

    <T> WatchAttributes(final Function<WatchModel, T> accessor, final BiConsumer<WatchModel, T> mutator, final Class<T> attributeType) {
        this.accessor = accessor;
        @SuppressWarnings("unchecked") final BiConsumer<WatchModel, Object> typedMutator = (BiConsumer<WatchModel, Object>) mutator;
        this.mutator = typedMutator;
        this.attributeType = attributeType;
    }

    public Object getAttribute(@NonNull final WatchModel model) {
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
    public Class<WatchModel> getModelType() {
        return WatchModel.class;
    }

    public void setAttribute(@NonNull final WatchModel model, final Object value) {
        if (mutator == null) {
            throw new NullPointerException("WatchModel cannot set " + name());
        }
        if ((value != null) && !attributeType.isInstance(value)) {
            throw new IllegalArgumentException(String.format("Expected %s but found %s", attributeType.getSimpleName(), value.getClass().getSimpleName()));
        }
        mutator.accept(model, value);
    }
}
