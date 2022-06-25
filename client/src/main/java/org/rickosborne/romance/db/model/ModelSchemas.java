package org.rickosborne.romance.db.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

@RequiredArgsConstructor
public enum ModelSchemas {
    AuthorModelSchema(AuthorModel.class, AuthorSchema::new, AuthorAttributes.class),
    BookModelSchema(BookModel.class, BookSchema::new, BookAttributes.class),
    NarratorModelSchema(NarratorModel.class, NarratorSchema::new, NarratorAttributes.class),
    SeriesModelSchema(SeriesModel.class, SeriesSchema::new, SeriesAttributes.class),
    TagModelSchema(TagModel.class, TagSchema::new, TagAttributes.class),
    WatchModelSchema(WatchModel.class, WatchSchema::new, WatchAttributes.class),
    ;
    @Getter
    private final Class<?> modelClass;
    private final Supplier<ModelSchema<?>> schemaSupplier;

    <M, S extends ModelSchema<M>, A> ModelSchemas(
        final Class<M> modelClass,
        final Supplier<S> schemaSupplier,
        final Class<A> attributesClass
    ) {
        this.modelClass = modelClass;
        @SuppressWarnings("unchecked") final Supplier<ModelSchema<?>> typedSchemaSupplier = (Supplier<ModelSchema<?>>) schemaSupplier;
        this.schemaSupplier = typedSchemaSupplier;
    }

    public <M, S extends ModelSchema<M>> S getModelSchema() {
        @SuppressWarnings("unchecked") final S modelSchema = (S) schemaSupplier.get();
        return modelSchema;
    }
}
