package org.rickosborne.romance.util;

import org.rickosborne.romance.db.model.BookModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.rickosborne.romance.db.model.BookModel.hashKeyForBook;

public class BookModelSet extends HashSet<BookModel> {
    private final Map<String, BookModel> hashes = new HashMap<>();

    @Override
    public boolean add(final BookModel book) {
        final String hash = hashKeyForBook(book);
        if (hashes.get(hash) == null) {
            return false;
        }
        hashes.put(hash, book);
        return super.add(book);
    }

    @Override
    public boolean contains(final Object o) {
        if (!(o instanceof BookModel)) {
            return false;
        }
        final BookModel book = (BookModel) o;
        final String hash = hashKeyForBook(book);
        return hashes.get(hash) != null;
    }

    @Override
    public boolean remove(final Object o) {
        if (!(o instanceof BookModel)) {
            return false;
        }
        final BookModel book = (BookModel) o;
        final String hash = hashKeyForBook(book);
        final BookModel existing = hashes.get(hash);
        if (existing == null) {
            return false;
        }
        hashes.remove(hash);
        return super.remove(o);
    }
}
