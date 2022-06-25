package org.rickosborne.romance.db;

import lombok.NonNull;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.model.BookSchema;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class BookJsonStore extends JsonStore<BookModel> {
    public BookJsonStore(
        @NonNull final NamingConvention namingConvention,
        @NonNull final Path dbPath
    ) {
        super(
            DbModel.Book,
            new BookSchema(),
            BookModel.class,
            namingConvention,
            dbPath.resolve(DbModel.Book.getTypeName())
        );
    }

    @Override
    public Diff<BookModel> diffModels(final BookModel before, final BookModel after) {
        final Diff<BookModel> diff = new Diff<>(after, before, List.of());
        if (before == null && after == null) {
            return diff;
        } else if (before == null) {
            throw new IllegalStateException("Not implemented: diffModels(null, ...)");
        } else if (after == null) {
            throw new IllegalStateException("Not implemented: diffModels(..., null)");
        } else {
            throw new IllegalStateException("Not implemented: diffModels(..., ...)");
        }
//        return diff;
    }

    @Override
    public String idFromModel(final BookModel model) {
        return getNamingConvention().fileNameFromTexts(
            model.getAuthorName(),
            Optional.ofNullable(model.getDatePublish()).map(LocalDate::getYear).map(String::valueOf).orElse(""),
            model.getTitle()
        );
    }
}
