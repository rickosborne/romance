package org.rickosborne.romance.client.command;

import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.client.AudiobookStoreService;
import org.rickosborne.romance.client.response.Login;
import org.rickosborne.romance.client.response.UserInformation2;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.Diff;
import org.rickosborne.romance.db.SchemaDiff;
import org.rickosborne.romance.db.model.BookAttributes;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.model.SchemaAttribute;
import org.rickosborne.romance.db.sheet.SheetStore;
import org.rickosborne.romance.sheet.ModelSheetAdapter;
import org.rickosborne.romance.util.BookBot;
import org.rickosborne.romance.util.BookMerger;
import org.rickosborne.romance.util.ConsoleStuff;
import org.rickosborne.romance.util.ModelSetter;
import org.rickosborne.romance.util.SheetStuff;
import picocli.CommandLine;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static org.rickosborne.romance.util.BookMerger.bookLikeFilter;

@Slf4j
@CommandLine.Command(
    name = "read",
    description = "Fetch and display recently finished reads"
)
public class ReadCommand extends ASheetCommand {
    @CommandLine.Option(names = {"--highlight"}, negatable = true)
    protected boolean highlight = true;
    @CommandLine.Option(names = {"--limit"})
    protected int limit = 20;
    @CommandLine.Option(names = {"--max-days-old"})
    protected long maxDaysOld = 20;

    @Override
    protected Integer doWithSheets() {
        final BookBot bot = getBookBot();
        final AudiobookStoreAuthOptions tabsAuth = getTabsAuth();
        final AudiobookStoreService tabsService = bot.getAudiobookStoreCache().getService();
        final Login login = tabsAuth.ensureAuthGuid(tabsService);
        final LocalDate onOrAfter = LocalDate.now().minusDays(maxDaysOld);
        final UpdateContext updateContext = (highlight || isWrite()) ? new UpdateContext(bot) : null;
        try {
            final UserInformation2 userInfo = tabsService.userInformation2(login.getUserGuid().toString())
                .execute().body();
            Objects.requireNonNull(userInfo).getAudiobooks().stream()
                .map(BookMerger::modelFromBookInformation)
                .filter(b -> b.getDateRead() != null && onOrAfter.isBefore(b.getDateRead()))
                .sorted(Comparator.comparing(BookModel::getDateRead).reversed())
                .limit(this.limit)
                .sorted(Comparator.comparing(BookModel::getDateRead))
                .forEachOrdered(book -> {
                    String message = book.toString();
                    if (updateContext != null) {
                        message = updateIfNecessary(book, updateContext);
                    }
                    System.out.printf("%s\t%s%n", book.getDateRead(), message);
                });
            writeChangesIfRequested();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    protected String updateIfNecessary(final BookModel book, final UpdateContext context) {
        Function<String, String> formatter = (s) -> s;
        final BookModel fullBook = context.sheetStore.findLikeOrMatch(book, bookLikeFilter(book));
        if (fullBook == null) {
            formatter = (s) -> ConsoleStuff.warn(s + "\t⚠️ Not in sheet");
        } else {
            final String bookId = context.sheetStore.idFromModel(fullBook);
            if (bookId == null) {
                formatter = (s) -> ConsoleStuff.warn(s + "\t⚠️ No ID");
            } else {
                final SheetStuff.Indexed<BookModel> indexedBook = context.bookRecords.get(bookId);
                if (indexedBook == null) {
                    formatter = (s) -> ConsoleStuff.warn(s + "\t⚠️ Not indexed");
                } else {
                    final BookModel sheetBook = indexedBook.getModel();
                    final int rowNum = indexedBook.getRowNum();
                    if (sheetBook != null && (sheetBook.getDnf() == null || sheetBook.getDnf() == Boolean.FALSE) && sheetBook.getDateRead() == null) {
                        final Diff<BookModel> bookDiff = new SchemaDiff().diffModels(sheetBook, book, List.of(BookAttributes.dateRead));
                        final Map<String, String> changes = context.bookAdapter.changesForDiff(bookDiff, (attr) -> attr.equals(BookAttributes.dateRead), context.bookSheetFields);
                        formatter = (s) -> ConsoleStuff.updated(s + "\t🆕");
                        final List<Request> requests = changeRequestsFromModelChanges(context.sheet, context.colNums, rowNum, changes);
                        getChangeRequests().addAll(requests);
                    }
                }
            }
        }
        return formatter.apply(book.toString());
    }

    protected class UpdateContext {
        final ModelSheetAdapter<BookModel> bookAdapter = getAdapterFactory().adapterForType(BookModel.class);
        final Map<String, SheetStuff.Indexed<BookModel>> bookRecords;
        final Map<SchemaAttribute<BookModel, ?>, ModelSetter<BookModel>> bookSheetFields = bookAdapter.getSheetFields();
        final Map<String, Integer> colNums;
        final Sheet sheet;
        final SheetStore<BookModel> sheetStore;

        public UpdateContext(final BookBot bot) {
            sheetStore = bot.getSheetStoreFactory().buildSheetStore(BookModel.class);
            sheet = sheetStore.getSheet();
            bookRecords = sheetStore.getRecordsById();
            colNums = new DataSet<>(DbModel.Book).getColNums();
        }
    }
}
