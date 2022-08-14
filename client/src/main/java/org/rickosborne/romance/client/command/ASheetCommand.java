package org.rickosborne.romance.client.command;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.api.services.sheets.v4.model.GridCoordinate;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.UpdateCellsRequest;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import org.rickosborne.romance.BooksSheets;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.client.JsonCookieStore;
import org.rickosborne.romance.client.html.AudiobookStoreHtml;
import org.rickosborne.romance.client.html.StoryGraphHtml;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.json.JsonStore;
import org.rickosborne.romance.db.json.JsonStoreFactory;
import org.rickosborne.romance.db.model.ModelSchema;
import org.rickosborne.romance.db.sheet.SheetStore;
import org.rickosborne.romance.db.sheet.SheetStoreFactory;
import org.rickosborne.romance.sheet.AdapterFactory;
import org.rickosborne.romance.sheet.ModelSheetAdapter;
import org.rickosborne.romance.util.BookBot;
import org.rickosborne.romance.util.Once;
import org.rickosborne.romance.util.StringStuff;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class ASheetCommand implements Callable<Integer> {
    @Getter(value = AccessLevel.PROTECTED)
    private static final LocalDate excelEpoch = LocalDate.of(1899, Month.DECEMBER, 30);
    @Getter(lazy = true, value = AccessLevel.PROTECTED)
    private final AdapterFactory adapterFactory = new AdapterFactory();
    @Getter(value = AccessLevel.PROTECTED)
    @CommandLine.Mixin
    private AudiobookStoreAuthOptions auth;
    @Getter(value = AccessLevel.PROTECTED)
    @CommandLine.Option(names = {"--cache", "-c"}, description = "Path to cache dir", defaultValue = ".cache/html")
    private Path cachePath;
    @Getter(lazy = true)
    private final AudiobookStoreHtml audiobookStoreHtml = new AudiobookStoreHtml(getCachePath(), null);
    @Getter(value = AccessLevel.PROTECTED)
    private final Map<String, Function<String, Object>> coerceFieldFunctions = Map.of(
        "isbn", s -> s
    );
    @SuppressWarnings("unused")
    @Getter(value = AccessLevel.PROTECTED)
    @CommandLine.Option(names = {"--cookies", "-s"}, description = "Path to cookie store", defaultValue = "./.credentials/abs-cookies.json")
    private Path cookieStorePath;
    @Getter(value = AccessLevel.PROTECTED)
    @CommandLine.Option(names = {"--path", "-p"}, description = "Path to DB dir", defaultValue = "book-data")
    private Path dbPath;
    @Getter(value = AccessLevel.PROTECTED, lazy = true)
    private final JsonCookieStore jsonCookieStore = Once.supply(() -> {
        final Path cookieStorePath = getCookieStorePath();
        if (cookieStorePath == null || !cookieStorePath.toFile().isFile()) {
            throw new IllegalArgumentException("Invalid cookie store path");
        }
        return JsonCookieStore.fromPath(cookieStorePath);
    });
    @Getter(value = AccessLevel.PROTECTED, lazy = true)
    private final NamingConvention namingConvention = new NamingConvention();
    @Getter(lazy = true)
    private final JsonStoreFactory jsonStoreFactory = buildJsonStoreFactory();
    @Getter(lazy = true)
    private final StoryGraphHtml storyGraphHtml = new StoryGraphHtml(getCachePath(), null);
    @Getter(value = AccessLevel.PROTECTED)
    @CommandLine.Option(names = {"--userid", "-u"}, description = "Google User ID/email", required = true)
    private String userId;
    @Getter(lazy = true)
    private final SheetStoreFactory sheetStoreFactory = buildSheetStoreFactory();
    @Getter(lazy = true)
    private final Sheets.Spreadsheets spreadsheets = BooksSheets.getSpreadsheets(getUserId());
    @Getter(lazy = true)
    private final Spreadsheet spreadsheet = BooksSheets.getSpreadsheet(getSpreadsheets());
    @Getter(lazy = true)
    private final BookBot bookBot = buildBookBot();
    @Getter(value = AccessLevel.PROTECTED)
    @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
    @CommandLine.Option(names = {"--write", "-w"}, description = "Write changes back to the spreadsheet")
    private boolean write = false;

    private BookBot buildBookBot() {
        return new BookBot(getAuth(), getCachePath(), getCookieStorePath(), getDbPath(), getUserId());
    }

    private JsonStoreFactory buildJsonStoreFactory() {
        return new JsonStoreFactory(getDbPath(), getNamingConvention());
    }

    private SheetStoreFactory buildSheetStoreFactory() {
        return new SheetStoreFactory(getNamingConvention(), getUserId());
    }

    @Override
    public Integer call() throws Exception {
        final File dbFile = getDbPath().toFile();
        if (!dbFile.isDirectory()) {
            throw new IllegalArgumentException("Ensure DB path exists and is a directory: " + dbFile);
        }
        final String userId = getUserId();
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("UserID is required");
        }
        return doWithSheets();
    }

    protected List<Request> changeRequestsFromModelChanges(final Sheet sheet, final Map<String, Integer> colNums, final int rowNum, final Map<String, String> changes) {
        final List<Request> changeRequests = new LinkedList<>();
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
                final double excelDays = 0d + getExcelEpoch().until((LocalDate) coerced, ChronoUnit.DAYS);
                extendedValue.setNumberValue(excelDays);
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
        return changeRequests;
    }

    protected Object coerceCellValue(final String fieldName, final String text) {
        final Function<String, Object> coercion = getCoerceFieldFunctions().get(fieldName);
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

    abstract protected Integer doWithSheets();


    @Value
    protected class DataSet<T> {
        String[] colKeys;
        Map<String, Integer> colNums;
        DbModel dbModel;
        JsonStore<T> jsonStore;
        ModelSchema<T> modelSchema;
        ModelSheetAdapter<T> modelSheetAdapter;
        Class<T> modelType;
        Sheet sheet;
        SheetStore<T> sheetStore;

        public DataSet(@NonNull final DbModel dbModel) {
            this.dbModel = dbModel;
            modelType = dbModel.getModelType();
            jsonStore = getJsonStoreFactory().buildJsonStore(modelType);
            modelSchema = jsonStore.getModelSchema();
            sheetStore = getSheetStoreFactory().buildSheetStore(modelType);
            colKeys = sheetStore.getColumnKeys();
            colNums = IntStream.range(0, colKeys.length).boxed().collect(Collectors.toMap(i -> colKeys[i], i -> i));
            modelSheetAdapter = getAdapterFactory().adapterForType(modelType);
            sheet = BooksSheets.sheetTitled(dbModel.getTabTitle(), getSpreadsheet());
        }
    }
}
