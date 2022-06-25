package org.rickosborne.romance.db.json;

import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.JsonStore;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.model.BookSchema;

import java.nio.file.Path;

public class BookJsonStore extends JsonStore<BookModel> {
    public BookJsonStore(
        final NamingConvention namingConvention,
        final Path dbPath
    ) {
        super(DbModel.Book, new BookSchema(), BookModel.class, namingConvention, dbPath.resolve(DbModel.Book.getTypeName()));
    }
}
