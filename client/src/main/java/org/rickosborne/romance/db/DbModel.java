package org.rickosborne.romance.db;

import lombok.Getter;
import lombok.NonNull;
import org.rickosborne.romance.db.model.AuthorModel;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.model.NarratorModel;
import org.rickosborne.romance.db.model.RedditPostModel;
import org.rickosborne.romance.db.model.SeriesModel;
import org.rickosborne.romance.db.model.TagModel;
import org.rickosborne.romance.db.model.WatchModel;
import org.rickosborne.romance.sheet.AuthorSheetAdapter;
import org.rickosborne.romance.sheet.BookSheetAdapter;
import org.rickosborne.romance.sheet.ModelSheetAdapter;
import org.rickosborne.romance.sheet.NarratorSheetAdapter;
import org.rickosborne.romance.sheet.SeriesSheetAdapter;
import org.rickosborne.romance.sheet.TagSheetAdapter;
import org.rickosborne.romance.sheet.WatchSheetAdapter;

import java.util.function.Supplier;

@Getter
public enum DbModel {
    Book("book", "Books", BookModel.class, BookModel::build, BookSheetAdapter.class),
    Author("author", "Authors", AuthorModel.class, AuthorModel::build, AuthorSheetAdapter.class),
    Narrator("narrator", "Narrators", NarratorModel.class, NarratorModel::new, NarratorSheetAdapter.class),
    Series("series", "Series", SeriesModel.class, SeriesModel::new, SeriesSheetAdapter.class),
    Tag("tag", "Tags", TagModel.class, TagModel::new, TagSheetAdapter.class),
    Watch("watch", "Watchlist", WatchModel.class, WatchModel::new, WatchSheetAdapter.class),
    RedditPost("redditPost", RedditPostModel.class, RedditPostModel::new),
    ;
    private final Class<? extends ModelSheetAdapter<?>> adapterType;
    private final DbModelType dbModelType;
    private final Class<?> modelType;
    private final Supplier<?> supplier;
    private final String tabTitle;
    private final String typeName;

    <M> DbModel(
        @NonNull final String typeName,
        @NonNull final Class<M> modelType,
        @NonNull final Supplier<M> supplier
    ) {
        this.typeName = typeName;
        this.modelType = modelType;
        this.supplier = supplier;
        this.adapterType = null;
        this.tabTitle = null;
        this.dbModelType = DbModelType.Reddit;
    }

    <M, A extends ModelSheetAdapter<M>> DbModel(
        @NonNull final String typeName,
        @NonNull final String tabTitle,
        @NonNull final Class<M> modelType,
        @NonNull final Supplier<M> supplier,
        @NonNull final Class<A> adapterType
    ) {
        this.typeName = typeName;
        this.modelType = modelType;
        this.supplier = supplier;
        this.adapterType = adapterType;
        this.tabTitle = tabTitle;
        this.dbModelType = DbModelType.DocSheet;
    }

    public <M> M buildModel() {
        @SuppressWarnings("unchecked") final M model = (M) this.supplier.get();
        return model;
    }

    public <M> Class<M> getModelType() {
        @SuppressWarnings("unchecked") final Class<M> t = (Class<M>) modelType;
        return t;
    }

    public enum DbModelType {
        DocSheet,
        Reddit,
    }

    public static DbModel forModelType(@NonNull final Class<?> modelType) {
        for (final DbModel dbModel : DbModel.values()) {
            if (modelType == dbModel.modelType) {
                return dbModel;
            }
        }
        throw new IllegalArgumentException("Not a DbModel type: " + modelType.getSimpleName());
    }
}
