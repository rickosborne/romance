package org.rickosborne.romance.client.command;

import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.api.services.sheets.v4.model.GridCoordinate;
import com.google.api.services.sheets.v4.model.GridProperties;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.UpdateCellsRequest;
import lombok.Getter;
import lombok.extern.java.Log;
import org.rickosborne.romance.BooksSheets;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.json.JsonStore;
import org.rickosborne.romance.db.json.JsonStoreFactory;
import org.rickosborne.romance.db.model.ModelSchema;
import org.rickosborne.romance.db.sheet.SheetStore;
import org.rickosborne.romance.db.sheet.SheetStoreFactory;
import org.rickosborne.romance.sheet.AdapterFactory;
import org.rickosborne.romance.sheet.ModelSheetAdapter;
import org.rickosborne.romance.util.SheetStuff;
import org.rickosborne.romance.util.StringStuff;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.rickosborne.romance.util.SheetStuff.getColumnKeys;

@Log
@CommandLine.Command(
    name = "data-from-sheet",
    description = "Pull and materialize the Sheet data to JSON files"
)
public class DataFromSheetCommand implements Callable<Integer> {
    private final AdapterFactory adapterFactory = new AdapterFactory();
    @SuppressWarnings("unused")
    @Getter
    @CommandLine.Option(names = {"--path", "-p"}, description = "Path to DB dir", defaultValue = "book-data")
    private Path dbPath;
    private JsonStoreFactory jsonStoreFactory;
    @Getter
    private final NamingConvention namingConvention = new NamingConvention();
    private SheetStoreFactory sheetStoreFactory;
    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--userid", "-u"}, description = "Google User ID/email", required = true)
    private String userId;

    @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
    @CommandLine.Option(names = {"--write", "-w"}, description = "Write changes back to the spreadsheet")
    private boolean write = false;

    private final Map<String, Function<String, Object>> coerceFieldFunctions = Map.of(
        "isbn", s -> s
    );
    private static final LocalDate EXCEL_EPOCH = LocalDate.of(1899, Month.DECEMBER, 30);

    @Override
    public Integer call() {
        final File dbFile = dbPath.toFile();
        if (!dbFile.isDirectory()) {
            throw new IllegalArgumentException("Ensure DB path exists and is a directory: " + dbFile);
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("UserID is required");
        }
        final Sheets.Spreadsheets spreadsheets = BooksSheets.getSpreadsheets(userId);
        final Spreadsheet spreadsheet = BooksSheets.getSpreadsheet(spreadsheets);
        jsonStoreFactory = new JsonStoreFactory(getDbPath(), getNamingConvention());
        sheetStoreFactory = new SheetStoreFactory(namingConvention, userId);
        for (final DbModel dbModel : DbModel.values()) {
            pullTab(dbModel, spreadsheet, spreadsheets);
        }
        System.out.println("data-from-sheet -p " + dbPath);
        return null;
    }

    private Object coerceCellValue(final String fieldName, final String text) {
        final Function<String, Object> coercion = coerceFieldFunctions.get(fieldName);
        if (coercion != null) {
            return coercion.apply(text);
        }
        if (StringStuff.isNumeric(text)) {
            return Double.parseDouble(text);
        } else if (StringStuff.isBoolean(text)) {
            return Boolean.parseBoolean(text);
        } else if (StringStuff.isDate(text)) {
            return LocalDate.parse(text);
        }
        return text;
    }

    private <M> void pullTab(
        final DbModel dbModel,
        final Spreadsheet spreadsheet,
        final Sheets.Spreadsheets spreadsheets
    ) {
        final String tabTitle = dbModel.getTabTitle();
        final Sheet sheet = BooksSheets.sheetTitled(tabTitle, spreadsheet);
        final List<List<Object>> rows = BooksSheets.sheetValues(tabTitle, spreadsheets);
        final GridProperties gridProperties = sheet.getProperties().getGridProperties();
        final ModelSheetAdapter<M> sheetAdapter = adapterFactory.adapterByName(dbModel.getTabTitle());
        final Class<M> modelType = sheetAdapter.getModelType();
        final JsonStore<M> jsonStore = jsonStoreFactory.buildJsonStore(modelType);
        final SheetStore<M> sheetStore = sheetStoreFactory.buildSheetStore(modelType);
        final ModelSchema<M> modelSchema = jsonStore.getModelSchema();
        final int frozenRowCount = Optional.ofNullable(gridProperties.getFrozenRowCount()).orElse(0);
        final int colCount = Optional.ofNullable(gridProperties.getColumnCount()).orElse(0);
        final NamingConvention namingConvention = new NamingConvention();
        final String[] colKeys = getColumnKeys(rows, frozenRowCount, colCount, namingConvention);
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
                    for (final Map.Entry<String, String> entry : changes.entrySet()) {
                        final String fieldName = entry.getKey();
                        final String value = entry.getValue();
                        final Integer colNum = colNums.get(fieldName);
                        if (colNum == null) {
                            throw new IllegalArgumentException("Could not find column for: " + fieldName);
                        }
                        final GridCoordinate coord = new GridCoordinate()
                            .setColumnIndex(colNum)
                            .setRowIndex(rowNum)
                            .setSheetId(sheet.getProperties().getSheetId());
                        final ExtendedValue extendedValue = new ExtendedValue();
                        final Object coerced = coerceCellValue(fieldName, value);
                        if (coerced instanceof Double) {
                            extendedValue.setNumberValue((Double) coerced);
                        } else if (coerced instanceof Boolean) {
                            extendedValue.setBoolValue((Boolean) coerced);
                        } else if (coerced instanceof LocalDate) {
                            extendedValue.setNumberValue(0d + EXCEL_EPOCH.until((LocalDate) coerced).getDays());
                        } else {
                            extendedValue.setStringValue(value);
                        }
                        final Request request = new Request().setUpdateCells(
                            new UpdateCellsRequest()
                                .setStart(coord)
                                .setFields("userEnteredValue")
                                .setRows(List.of(
                                    new RowData().setValues(List.of(
                                        new CellData().setUserEnteredValue(extendedValue)
                                    ))
                                ))
                        );
                        request.setFactory(GsonFactory.getDefaultInstance());
                        changeRequests.add(request);
                        System.out.println(request);
                    }
                }
            }
            jsonStore.saveIfChanged(updated);
        }
        if (write && !changeRequests.isEmpty()) {
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
