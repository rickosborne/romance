package org.rickosborne.romance.client.command;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import lombok.extern.java.Log;
import org.rickosborne.romance.BooksSheets;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.json.JsonStore;
import org.rickosborne.romance.db.model.ModelSchema;
import org.rickosborne.romance.db.sheet.SheetStore;
import org.rickosborne.romance.sheet.ModelSheetAdapter;
import org.rickosborne.romance.util.SheetStuff;
import picocli.CommandLine;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Log
@CommandLine.Command(
    name = "data-from-sheet",
    description = "Pull and materialize the Sheet data to JSON files"
)
public class DataFromSheetCommand extends ASheetCommand {
    @Override
    protected Integer doWithSheets() {
        for (final DbModel dbModel : DbModel.values()) {
            pullTab(dbModel, getSpreadsheet(), getSpreadsheets());
        }
        System.out.println("data-from-sheet -p " + getDbPath());
        return null;
    }

    private <M> void pullTab(
        final DbModel dbModel,
        final Spreadsheet spreadsheet,
        final Sheets.Spreadsheets spreadsheets
    ) {
        final String tabTitle = dbModel.getTabTitle();
        final Sheet sheet = BooksSheets.sheetTitled(tabTitle, spreadsheet);
        final ModelSheetAdapter<M> sheetAdapter = getAdapterFactory().adapterByName(dbModel.getTabTitle());
        final Class<M> modelType = sheetAdapter.getModelType();
        final JsonStore<M> jsonStore = getJsonStoreFactory().buildJsonStore(modelType);
        final SheetStore<M> sheetStore = getSheetStoreFactory().buildSheetStore(modelType);
        final SheetStuff.SheetDescriptor sheetDescriptor = sheetStore.getSheetDescriptor();
        final ModelSchema<M> modelSchema = jsonStore.getModelSchema();
        final String[] colKeys = sheetDescriptor.getColumnKeys();
        final Map<String, Integer> colNums = IntStream.range(0, colKeys.length).boxed().collect(Collectors.toMap(i -> colKeys[i], i -> i));
        System.out.println(tabTitle + ": " + String.join(", ", colKeys));
        final List<Request> changeRequests = new LinkedList<>();
        for (final SheetStuff.Indexed<M> indexed : sheetStore.getRecords()) {
            final int rowNum = indexed.getRowNum();
            final M record = indexed.getModel();
            final M existing = jsonStore.findLikeFromCache(record);
            final M updated = modelSchema.mergeModels(existing, record);
            if (existing != null) {
                final Map<String, String> changes = sheetAdapter.findChangesToSheet(record, updated);
                if (!changes.isEmpty()) {
                    System.out.println("~~~ " + jsonStore.idFromModel(existing));
                    System.out.println(changes);
                    changeRequests.addAll(changeRequestsFromModelChanges(sheet, colNums, rowNum, changes));
                }
            }
            jsonStore.saveIfChanged(updated);
        }
        if (isWrite() && !changeRequests.isEmpty()) {
            final BatchUpdateSpreadsheetRequest updateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest()
                .setRequests(changeRequests);
            try {
                spreadsheets.batchUpdate(spreadsheet.getSpreadsheetId(), updateSpreadsheetRequest).execute();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
