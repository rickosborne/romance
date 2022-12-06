package org.rickosborne.romance.client.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.client.bookwyrm.BookWyrmConfig;
import org.rickosborne.romance.db.DbJsonWriter;
import org.rickosborne.romance.db.json.JsonStore;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.model.BookSchema;
import org.rickosborne.romance.db.postgresql.BookWyrmPGBookStore;
import org.rickosborne.romance.db.sheet.SheetStore;
import org.rickosborne.romance.util.Pair;
import picocli.CommandLine;

import static org.rickosborne.romance.util.BookMerger.bookLikeFilter;

@Slf4j
@CommandLine.Command(
    name = "bw-sync",
    description = "Synchronize with BookWyrm PG DB"
)
public class BookWyrmPGSyncCommand extends ASheetCommand {
    private final BookWyrmConfig bookWyrmConfig = BookWyrmConfig.getInstance();

    @Override
    protected Integer doWithSheets() {
        final ObjectWriter jsonWriter = DbJsonWriter.getJsonWriter();
        final SheetStore<BookModel> sheetBookStore = getSheetStoreFactory().buildSheetStore(BookModel.class);
        final BookSchema bookSchema = new BookSchema();
        final JsonStore<BookModel> jsonStore = getJsonStoreFactory().buildJsonStore(BookModel.class);
        try (final BookWyrmPGBookStore db = new BookWyrmPGBookStore(bookWyrmConfig)) {
            final BookWyrmPGBookStore.IdCache idCache = db.getIdCache();
            log.info("Titles in ID cache: {}", idCache.getCount());
            for (final BookModel sheetBook : sheetBookStore) {
                final Pair<BookModel, Integer> pair = db.findLikeForUser(sheetBook, bookWyrmConfig.getUserId());
                final BookModel dbBook = pair == null ? null : pair.getLeft();
                if (dbBook == null) {
                    log.debug("Could not find: {}", sheetBook);
                    continue;
                }
                log.debug(jsonWriter.writeValueAsString(dbBook));
                final BookModel jsonBook = jsonStore.findLikeOrMatch(sheetBook, bookLikeFilter(sheetBook));
                BookModel merged = bookSchema.mergeModels(dbBook, jsonBook);
                merged = bookSchema.mergeModels(merged, sheetBook);
                log.debug(jsonWriter.writeValueAsString(merged));
                db.saveIfChanged(merged);
                break;
            }
            return 0;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
