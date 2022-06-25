package org.rickosborne.romance.db.json;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.JsonStore;
import org.rickosborne.romance.db.model.AuthorModel;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.model.NarratorModel;
import org.rickosborne.romance.db.model.SeriesModel;
import org.rickosborne.romance.db.model.TagModel;
import org.rickosborne.romance.db.model.WatchModel;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

@AllArgsConstructor
public class JsonStoreFactory {
    private final Path dbPath;
    private final NamingConvention namingConvention;
    private final Map<Class<?>, JsonStore<?>> stores = new HashMap<>();

    public <M, S extends JsonStore<M>> S buildJsonStore(@NonNull final Class<M> modelType) {
        @SuppressWarnings("unchecked") final S store = (S) stores.computeIfAbsent(modelType, m -> {
            for (final StoreModel storeModel : StoreModel.values()) {
                if (m == storeModel.getModelType()) {
                    return storeModel.<M, S>buildStore(namingConvention, dbPath);
                }
            }
            throw new IllegalArgumentException("No JsonStore for type: " + m.getSimpleName());
        });
        return store;
    }

    public enum StoreModel {
        Author(AuthorModel.class, AuthorJsonStore::new),
        Book(BookModel.class, BookJsonStore::new),
        Narrator(NarratorModel.class, NarratorJsonStore::new),
        Series(SeriesModel.class, SeriesJsonStore::new),
        Tag(TagModel.class, TagJsonStore::new),
        Watch(WatchModel.class, WatchJsonStore::new),
        ;
        private final Class<?> modelType;
        private final JsonStoreSupplier<?, ? extends JsonStore<?>> storeSupplier;

        <M, S extends JsonStore<M>> StoreModel(
            @NonNull final Class<M> modelType,
            @NonNull final JsonStoreSupplier<M, S> storeSupplier
        ) {
            this.modelType = modelType;
            this.storeSupplier = storeSupplier;
        }

        <M, S extends JsonStore<M>> S buildStore(
            final NamingConvention namingConvention,
            final Path typePath
        ) {
            @SuppressWarnings("unchecked") final S store = (S) storeSupplier.apply(namingConvention, typePath);
            return store;
        }

        <M> Class<M> getModelType() {
            @SuppressWarnings("unchecked") final Class<M> typed = (Class<M>) modelType;
            return typed;
        }
    }

    interface JsonStoreSupplier<M, S extends JsonStore<M>> extends BiFunction<NamingConvention, Path, S> {
    }
}
