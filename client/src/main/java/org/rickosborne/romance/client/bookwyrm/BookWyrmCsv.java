package org.rickosborne.romance.client.bookwyrm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.util.StringStuff;

import java.util.function.BiConsumer;

@Getter
@RequiredArgsConstructor
public enum BookWyrmCsv {
    title(BookModel::setTitle),
    author_text(BookModel::setAuthorName),
    remote_id,
    openlibrary_key,
    inventaire_id,
    librarything_key,
    goodreads_key((b, s) -> b.setGoodreadsUrl(StringStuff.urlFromString("https://www.goodreads.com/book/show/" + s))),
    bnf_id,
    viaf,
    wikidata,
    asin,
    isbn_10((b, s) -> {
        if (!StringStuff.nonBlank(b.getIsbn())) {
            b.setIsbn(s);
        }
    }),
    isbn_13((b, s) -> {
        final String isbn = b.getIsbn();
        if (!StringStuff.nonBlank(isbn) || isbn.length() < 13) {
            b.setIsbn(s);
        }
    }),
    oclc_number,
    rating,
    review_name,
    review_cw,
    review_content,
    ;
    private final BiConsumer<BookModel, String> bookModelSetter;

    BookWyrmCsv() {
        this(null);
    }

    public void setBook(final BookModel book, final String text) {
        if (book != null && bookModelSetter != null) {
            bookModelSetter.accept(book, text);
        }
    }
}
