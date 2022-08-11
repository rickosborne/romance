package org.rickosborne.romance.client.command;

import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import lombok.extern.java.Log;
import org.rickosborne.romance.BooksSheets;
import org.rickosborne.romance.client.html.AudiobookStoreHtml;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.json.JsonStore;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.model.ModelSchema;
import org.rickosborne.romance.db.sheet.SheetStore;
import org.rickosborne.romance.sheet.ModelSheetAdapter;
import org.rickosborne.romance.util.SheetStuff;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Log
@CommandLine.Command(
    name = "tabs-data-for-sheet",
    description = "Pull and materialize the Sheet data to JSON files"
)
public class FillFromTABSCommand extends ASheetCommand {

    @Override
    protected Integer doWithSheets() {
        final Class<BookModel> bookModelType = DbModel.Book.getModelType();
        final JsonStore<BookModel> jsonStore = getJsonStoreFactory().buildJsonStore(bookModelType);
        final ModelSchema<BookModel> modelSchema = jsonStore.getModelSchema();
        final SheetStore<BookModel> bookSheetStore = getSheetStoreFactory().buildSheetStore(bookModelType);
        final AudiobookStoreHtml audiobookStoreHtml = getAudiobookStoreHtml();
        final ModelSheetAdapter<BookModel> sheetAdapter = getAdapterFactory().adapterByName(DbModel.Book.getTabTitle());
        final Sheet sheet = BooksSheets.sheetTitled(DbModel.Book.getTabTitle(), getSpreadsheet());
        final String[] colKeys = bookSheetStore.getSheetDescriptor().getColumnKeys();
        final Map<String, Integer> colNums = IntStream.range(0, colKeys.length).boxed().collect(Collectors.toMap(i -> colKeys[i], i -> i));
        final List<Request> changeRequests = new LinkedList<>();
        for (final SheetStuff.Indexed<BookModel> bookRecord : bookSheetStore.getRecords()) {
            final BookModel sheetBook = bookRecord.getModel();
            final int rowNum = bookRecord.getRowNum();
            final URL tabsUrl = sheetBook.getAudiobookStoreUrl();
            if (tabsUrl == null) {
                continue;
            }
            final BookModel tabsBook = audiobookStoreHtml.getBookModel(tabsUrl);
            final BookModel sheetPlusTabs = modelSchema.mergeModels(tabsBook, sheetBook);
            final BookModel jsonBook = jsonStore.findLikeFromCache(sheetPlusTabs);
            final BookModel all = modelSchema.mergeModels(jsonBook, sheetPlusTabs);
            final Map<String, String> changes = sheetAdapter.findChangesToSheet(sheetBook, all);
            if (!changes.isEmpty()) {
                System.out.println("~~~ " + rowNum + ":" + jsonStore.idFromModel(all));
                System.out.println(changes);
                changeRequests.addAll(changeRequestsFromModelChanges(sheet, colNums, rowNum, changes));
            }
            jsonStore.saveIfChanged(all);
        }
        if (isWrite() && !changeRequests.isEmpty()) {
            final BatchUpdateSpreadsheetRequest updateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest()
                .setRequests(changeRequests);
            try {
                getSpreadsheets().batchUpdate(getSpreadsheet().getSpreadsheetId(), updateSpreadsheetRequest).execute();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
