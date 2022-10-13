package org.rickosborne.romance.client.command;

import com.google.api.services.sheets.v4.model.Request;
import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.client.html.AudiobookStoreHtml;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.model.AuthorModel;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.sheet.SheetStore;
import org.rickosborne.romance.util.BookBot;
import org.rickosborne.romance.util.BookRating;
import org.rickosborne.romance.util.IgnoredBooks;
import org.rickosborne.romance.util.SheetStuff;
import picocli.CommandLine;

import java.net.URL;
import java.util.List;
import java.util.Objects;

import static org.rickosborne.romance.util.BookMerger.bookLikeFilter;
import static org.rickosborne.romance.util.StringStuff.CRLF;

@CommandLine.Command(
    name = "more-from-authors",
    description = "Find more unknown books from known authors"
)
@Slf4j
public class MoreFromAuthorsCommand extends ASheetCommand {
    public static final String MAX_RATING = "--max-rating";
    public static final String MIN_RATING = "--min-rating";

    @SuppressWarnings("unused")
    @CommandLine.Option(names = MIN_RATING, required = true)
    private Double minRating;

    @SuppressWarnings({"unused", "FieldMayBeFinal", "FieldCanBeLocal"})
    @CommandLine.Option(names = MAX_RATING)
    private Double maxRating;

    @Override
    protected Integer doWithSheets() {
        Objects.requireNonNull(minRating, "Required: " + MIN_RATING);
        final DataSet<AuthorModel> authorData = new DataSet<>(DbModel.Author);
        final DataSet<BookModel> bookData = new DataSet<>(DbModel.Book);
        final SheetStore<AuthorModel> authorStore = authorData.getSheetStore();
        final SheetStore<BookModel> bookStore = bookData.getSheetStore();
        final List<Request> changeRequests = getChangeRequests();
        final AudiobookStoreHtml tabsHtml = getAudiobookStoreHtml();
        final BookBot bookBot = getBookBot();
        final StringBuilder sb = new StringBuilder();
        for (final SheetStuff.Indexed<AuthorModel> indexedAuthor : authorStore.getRecords()) {
            final int rowNum = indexedAuthor.getRowNum();
            final AuthorModel author = indexedAuthor.getModel();
            final Double authorOverall = author.getRatings().get(BookRating.Overall);
            final URL authorTabsUrl = author.getAudiobookStoreUrl();
            if (authorOverall == null || authorOverall < minRating || authorTabsUrl == null || (maxRating != null && authorOverall >= maxRating)) {
                continue;
            }
            log.info(author.toString());
            for (final BookModel foundBook : tabsHtml.getBooksForAuthor(author)) {
                if (IgnoredBooks.isIgnored(foundBook)) {
                    continue;
                }
                BookModel book = bookStore.findLikeOrMatch(foundBook, bookLikeFilter(foundBook));
                if (book != null) {
                    continue;
                }
                book = bookBot.extendAll(foundBook);
                if (book == null) {
                    continue;
                }
                sb.append(DocTabbed.fromBookModel(book)).append(CRLF);
            }
        }
        System.out.println(sb);
        return 0;
    }
}
