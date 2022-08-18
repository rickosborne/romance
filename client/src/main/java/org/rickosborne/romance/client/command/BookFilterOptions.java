package org.rickosborne.romance.client.command;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.db.model.BookModel;
import picocli.CommandLine;

import static org.rickosborne.romance.util.StringStuff.fuzzyListMatch;
import static org.rickosborne.romance.util.StringStuff.fuzzyMatch;

@Slf4j
@Getter
public class BookFilterOptions {
    @CommandLine.Option(names = {"--author", "-ba"}, description = "Book author")
    private String bookAuthor;
    @CommandLine.Option(names = {"--title", "-bt"}, description = "Book title")
    private String bookTitle;

    public boolean bookMatches(final BookModel book) {
        if (book == null) {
            return false;
        }
        if (bookTitle != null && !fuzzyMatch(bookTitle, book.getTitle())) {
            return false;
        }
        //noinspection RedundantIfStatement
        if (bookAuthor != null && !fuzzyListMatch(bookAuthor, book.getAuthorName())) {
            return false;
        }
        return true;
    }

    public boolean isEmpty() {
        return bookAuthor == null && bookTitle == null;
    }
}
