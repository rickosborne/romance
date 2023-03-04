package org.rickosborne.romance.client.audiobookstore;

import lombok.NonNull;
import org.rickosborne.romance.db.model.AuthorModel;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.model.NarratorModel;
import org.rickosborne.romance.db.model.SeriesModel;

public interface AbsPaginatedVisitor {
    default void onAuthor(@NonNull final AuthorModel author) {
    }

    default void onBook(@NonNull final BookModel book) {
    }

    default void onNarrator(@NonNull final NarratorModel narrator) {
    }

    default boolean onPageComplete() {
        return false;
    }

    default boolean onPreviewAuthor(@NonNull final AuthorModel author) {
        return false;
    }

    default boolean onPreviewBook(@NonNull final BookModel book) {
        return false;
    }

    default boolean onPreviewNarrator(@NonNull final NarratorModel narrator) {
        return false;
    }

    default boolean onPreviewSeries(@NonNull final SeriesModel series) {
        return false;
    }

    default void onSeries(@NonNull final SeriesModel series) {
    }

}
