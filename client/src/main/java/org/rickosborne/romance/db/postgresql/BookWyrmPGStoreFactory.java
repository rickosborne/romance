package org.rickosborne.romance.db.postgresql;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.client.bookwyrm.BookWyrmConfig;
import org.rickosborne.romance.db.model.AuthorModel;
import org.rickosborne.romance.db.model.BookModel;

import java.util.function.Function;

@Slf4j
@Getter
public enum BookWyrmPGStoreFactory {
    Author(AuthorModel.class, BookWyrmPGAuthorStore::new),
    Book(BookModel.class, BookWyrmPGBookStore::new),
    ;

    @SuppressWarnings("unchecked")
    static <M, S extends BookWyrmPGStore<M>> S buildStore(
        @NonNull final Class<M> modelType,
        @NonNull final BookWyrmConfig bookWyrmConfig
    ) {
        for (final BookWyrmPGStoreFactory factory : BookWyrmPGStoreFactory.values()) {
            if (factory.modelType == modelType) {
                return (S) factory.builder.apply(bookWyrmConfig);
            }
        }
        throw new IllegalArgumentException("No store for model: " + modelType.getSimpleName());
    }

    private final Function<BookWyrmConfig, BookWyrmPGStore<?>> builder;
    private final Class<?> modelType;

    @SuppressWarnings("unchecked")
    <M> BookWyrmPGStoreFactory(
        @NonNull final Class<M> modelType,
        @NonNull final Function<BookWyrmConfig, BookWyrmPGStore<M>> builder
    ) {
        this.modelType = modelType;
        this.builder = (Function<BookWyrmConfig, BookWyrmPGStore<?>>) (Object) builder;
    }
}
