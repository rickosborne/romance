package org.rickosborne.romance.client.command;

import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import org.apache.commons.csv.CSVRecord;
import org.rickosborne.romance.client.BookWyrmService;
import org.rickosborne.romance.client.bookwyrm.BookWyrmCsv;
import org.rickosborne.romance.client.bookwyrm.BookWyrmCsvStore;
import org.rickosborne.romance.client.response.BookWyrmBook;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.sheet.SheetStore;
import org.rickosborne.romance.util.Mutable;
import picocli.CommandLine;
import retrofit2.Response;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import static org.rickosborne.romance.client.BookWyrmService.tryToUpdateCsrf;
import static org.rickosborne.romance.util.BookMerger.bookLikeFilter;

@Slf4j
@CommandLine.Command(
    name = "bw-covers",
    description = "Add cover art to BookWyrm"
)
public class BookWyrmCoversCommand extends ASheetCommand {
    @CommandLine.Option(names = "--bw-csv", description = "BookWyrm Export CSV", required = true)
    private String bwCsv;
    @CommandLine.Option(names = "--csrf-cookie", description = "CSRF Cookie", required = true)
    private String startingCsrfCookie;
    @CommandLine.Option(names = "--session-id", description = "Session ID", required = true)
    private String sessionId;

    private HttpUrl apiBase;

    @Override
    protected Integer doWithSheets() {
        final Path bwCsvPath = Path.of(bwCsv);
        if (!bwCsvPath.toFile().isFile()) {
            throw new IllegalArgumentException("Not a file: " + bwCsv);
        }
        if (startingCsrfCookie == null || startingCsrfCookie.isBlank()) {
            throw new IllegalArgumentException("Need --csrf tokens");
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Need a --session-id for BookWyrm Django");
        }
        final SheetStore<BookModel> bookStore = getSheetStoreFactory().buildSheetStore(BookModel.class);
        BookWyrmService bookWyrmService = null;
        final Mutable<String> csrfCookie = Mutable.of(startingCsrfCookie);
        final Mutable<String> csrfApp = Mutable.empty();
        final BookWyrmCsvStore csvStore = new BookWyrmCsvStore(bwCsvPath);
        try {
            for (final CSVRecord record : csvStore.getCsvParser()) {
                final String title = record.get(BookWyrmCsv.title);
                final String remoteId = record.get(BookWyrmCsv.remote_id);
                final String authorName = record.get(BookWyrmCsv.author_text);
                if (title == null || title.isBlank() || remoteId == null || remoteId.isBlank() || authorName == null || authorName.isBlank()) {
                    continue;
                }
                if (bookWyrmService == null) {
                    final String apiBase = remoteId.replaceFirst("/book/[0-9]+$", "");
                    this.apiBase = HttpUrl.parse(apiBase);
                    bookWyrmService = BookWyrmService.build(apiBase);
                }
                final Integer bookId = bookWyrmService.bookIdFromRemoteId(remoteId);
                if (bookId == null) {
                    log.error("Could not parse bookId: {}", remoteId);
                    continue;
                }
                final Response<BookWyrmBook> getResponse = bookWyrmService.getBook(bookId).execute();
                tryToUpdateCsrf(csrfCookie, apiBase, getResponse);
                bookWyrmService.getBookCsrf(bookId, sessionId, apiBase, csrfApp, csrfCookie);
                final BookWyrmBook book = getResponse.body();
                if (book == null) {
                    log.warn("Not on BookWyrm: \"{}\" by {}", title, authorName);
                    continue;
                }
                if (book.getCover() != null && book.getCover().getUrl() != null) {
                    log.info("Already has a cover: \"{}\" by {}", title, authorName);
                    continue;
                }
                final BookModel likeBook = BookModel.builder()
                    .title(title)
                    .authorName(authorName)
                    .build();
                final BookModel storedBook = bookStore.findLikeOrMatch(likeBook, bookLikeFilter(likeBook));
                if (storedBook == null) {
                    log.warn("Could not find: \"{}\" by {}", title, authorName);
                    continue;
                }
                final URL imageUrl = storedBook.getImageUrl();
                if (imageUrl == null) {
                    log.warn("No image: \"{}\" by {}", title, authorName);
                    continue;
                }
                final Response<Void> response = bookWyrmService.updateCoverFromUrl(
                    bookId,
                    imageUrl,
                    csrfCookie.getItem(),
                    csrfApp.getItem(),
                    sessionId,
                    remoteId
                ).execute();
                if (!response.isSuccessful()) {
                    log.error(response.errorBody().string());
                    throw new RuntimeException("Failed: " + response.code() + " " + response.message());
                }
                tryToUpdateCsrf(csrfCookie, apiBase, response);
                log.info("Fixed: \"{}\" by {}", title, authorName);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }


}
