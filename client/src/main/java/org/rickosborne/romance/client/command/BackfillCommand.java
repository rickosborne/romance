package org.rickosborne.romance.client.command;

import com.google.api.services.sheets.v4.model.Request;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.json.JsonStore;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.sheet.SheetStore;
import org.rickosborne.romance.util.BookBot;
import org.rickosborne.romance.util.BookRating;
import org.rickosborne.romance.util.SheetStuff;
import picocli.CommandLine;

import java.net.URL;
import java.util.List;
import java.util.Map;

@Slf4j
@CommandLine.Command(
    name = "backfill",
    description = "Try to fill in gaps in the spreadsheet"
)
public class BackfillCommand extends ASheetCommand {
    @Override
    protected Integer doWithSheets() {
        final SheetStore<BookModel> sheetStore = getSheetStoreFactory().buildSheetStore(BookModel.class);
        final JsonStore<BookModel> jsonStore = getJsonStoreFactory().buildJsonStore(BookModel.class);
        final BookBot bookBot = getBookBot();
        final List<Request> changes = getChangeRequests();
        final DataSet<BookModel> bookData = new DataSet<>(DbModel.Book);
        for (final SheetStuff.Indexed<BookModel> indexedBook : sheetStore.getRecords()) {
            final BookModel sheetBook = indexedBook.getModel();
            final int bookRowNum = indexedBook.getRowNum();
            final BookUrls sheetUrls = new BookUrls(sheetBook);
            if (sheetUrls.anyNull() || sheetBook.getDatePurchase() == null || sheetBook.getRatings().get(BookRating.Overall) != null) {
                log.info("Trying to backfill: {}", sheetBook);
                final BookModel extended = bookBot.extendAll(sheetBook);
                final Map<String, String> bookChanges = bookData.getModelSheetAdapter().findChangesToSheet(sheetBook, extended);
                if (!bookChanges.isEmpty()) {
                    System.out.println("~~~ " + bookRowNum + ":" + bookData.getJsonStore().idFromModel(extended));
                    System.out.println(bookChanges);
                    changes.addAll(changeRequestsFromModelChanges(bookData.getSheet(), bookData.getColNums(), bookRowNum, bookChanges));
                }
                jsonStore.save(extended);
            }
        }
        return null;
    }

    @Getter
    private static class BookUrls {
        private final URL audiobookStoreUrl;
        private final URL goodreadsUrl;
        private final URL imageUrl;
        private final URL storygraphUrl;

        public BookUrls(final BookModel book) {
            this.audiobookStoreUrl = book.getAudiobookStoreUrl();
            this.imageUrl = book.getImageUrl();
            this.goodreadsUrl = book.getGoodreadsUrl();
            this.storygraphUrl = book.getStorygraphUrl();
        }

        public boolean anyNull() {
            return (audiobookStoreUrl == null)
                || (goodreadsUrl == null)
                || (imageUrl == null)
                || (storygraphUrl == null);
        }
    }
}
