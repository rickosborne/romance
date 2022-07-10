package org.rickosborne.romance.sheet;

import lombok.NonNull;
import org.rickosborne.romance.db.model.AuthorModel;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.model.NarratorModel;
import org.rickosborne.romance.db.model.SeriesModel;
import org.rickosborne.romance.db.model.TagModel;
import org.rickosborne.romance.db.model.WatchModel;

import java.util.Map;

public class AdapterFactory {
    public static final Map<Class<?>, ModelSheetAdapter<?>> ADAPTER_BY_TYPE = Map.of(
        BookModel.class, new BookSheetAdapter(),
        AuthorModel.class, new AuthorSheetAdapter(),
        NarratorModel.class, new NarratorSheetAdapter(),
        SeriesModel.class, new SeriesSheetAdapter(),
        TagModel.class, new TagSheetAdapter(),
        WatchModel.class, new WatchSheetAdapter()
    );
    public static final Map<String, Class<?>> MODEL_BY_TITLE = Map.of(
        "Books", BookModel.class,
        "Authors", AuthorModel.class,
        "Narrators", NarratorModel.class,
        "Series", SeriesModel.class,
        "Tags", TagModel.class,
        "Watchlist", WatchModel.class
    );

    public <M> ModelSheetAdapter<M> adapterByName(@NonNull final String tabTitle) {
        final Class<?> modelType = MODEL_BY_TITLE.get(tabTitle);
        if (modelType == null) {
            throw new IllegalArgumentException("Unknown tab/model: " + tabTitle);
        }
        @SuppressWarnings("unchecked") final ModelSheetAdapter<M> typed = (ModelSheetAdapter<M>) adapterForType(modelType);
        return typed;
    }

    public <M> ModelSheetAdapter<M> adapterForType(@NonNull final Class<M> type) {
        final ModelSheetAdapter<?> adapter = ADAPTER_BY_TYPE.get(type);
        if (adapter == null) {
            throw new IllegalArgumentException("No adapter for type: " + type.getSimpleName());
        }
        @SuppressWarnings("unchecked") final ModelSheetAdapter<M> typedAdapter = (ModelSheetAdapter<M>) adapter;
        return typedAdapter;
    }
}
