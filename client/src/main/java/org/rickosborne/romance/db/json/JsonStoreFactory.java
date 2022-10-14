package org.rickosborne.romance.db.json;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.model.AuthorModel;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.model.ModelSchema;
import org.rickosborne.romance.db.model.ModelSchemas;
import org.rickosborne.romance.db.model.NarratorModel;
import org.rickosborne.romance.db.model.RedditPostModel;
import org.rickosborne.romance.db.model.SeriesModel;
import org.rickosborne.romance.db.model.TagModel;
import org.rickosborne.romance.db.model.WatchModel;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public class JsonStoreFactory {
    protected static <M> JsonStore<M> buildStore(
        final NamingConvention namingConvention,
        final Path dbPath,
        final DbModel dbModel
    ) {
        final Class<M> modelType = dbModel.getModelType();
        final ModelSchema<M> modelSchema = ModelSchemas.schemaForModelType(modelType);
        return new JsonStore<>(dbModel, modelSchema, modelType, namingConvention, dbPath.resolve(dbModel.getTypeName()));
    }

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
        Author(AuthorModel.class, DbModel.Author),
        Book(BookModel.class, DbModel.Book),
        Narrator(NarratorModel.class, DbModel.Narrator),
        Series(SeriesModel.class, DbModel.Series),
        Tag(TagModel.class, DbModel.Tag),
        Watch(WatchModel.class, DbModel.Watch),
        RedditPost(RedditPostModel.class, DbModel.RedditPost),
        ;
        private final DbModel dbModel;
        private final Class<?> modelType;

        <M> StoreModel(
            @NonNull final Class<M> modelType,
            @NonNull final DbModel dbModel
        ) {
            this.modelType = modelType;
            this.dbModel = dbModel;
        }

        @SuppressWarnings("unchecked")
        <M, S extends JsonStore<M>> S buildStore(
            final NamingConvention namingConvention,
            final Path typePath
        ) {
            return (S) JsonStoreFactory.buildStore(namingConvention, typePath, dbModel);
        }

        <M> Class<M> getModelType() {
            @SuppressWarnings("unchecked") final Class<M> typed = (Class<M>) modelType;
            return typed;
        }
    }
}
