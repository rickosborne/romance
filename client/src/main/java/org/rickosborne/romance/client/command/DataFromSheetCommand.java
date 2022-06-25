package org.rickosborne.romance.client.command;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.GridProperties;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import lombok.Getter;
import lombok.extern.java.Log;
import org.rickosborne.romance.BooksSheets;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.JsonStore;
import org.rickosborne.romance.db.json.JsonStoreFactory;
import org.rickosborne.romance.sheet.AdapterFactory;
import org.rickosborne.romance.sheet.ModelSheetAdapter;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.rickosborne.romance.util.StringStuff.stringify;

@Log
@CommandLine.Command(
    name = "data-from-sheet",
    description = "Pull and materialize the Sheet data to JSON files"
)
public class DataFromSheetCommand implements Callable<Integer> {
    public static final List<String> DATA_TAB_TITLES = Stream.of(DbModel.values())
        .map(DbModel::getTabTitle)
        .collect(Collectors.toList());

    private final AdapterFactory adapterFactory = new AdapterFactory();

    @SuppressWarnings("unused")
    @Getter
    @CommandLine.Option(names = {"--path", "-p"}, description = "Path to DB dir", defaultValue = "book-data")
    private Path dbPath;
    private JsonStoreFactory jsonStoreFactory;
    @Getter
    private final NamingConvention namingConvention = new NamingConvention();
    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--userid", "-u"}, description = "Google User ID/email", required = true)
    private String userId;

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
        //noinspection CodeBlock2Expr
        DATA_TAB_TITLES.forEach(tabTitle -> {
            pullTab(tabTitle, spreadsheet, spreadsheets, adapterFactory.adapterByName(tabTitle));
        });
        System.out.println("data-from-sheet -p " + dbPath);
        return null;
    }

    private String[] getColumnKeys(
        final List<List<Object>> rows,
        final int frozenRowCount,
        final int colCount,
        final NamingConvention namingConvention
    ) {
        final String[] colKeys = new String[colCount];
        final String[] lastInRow = new String[frozenRowCount];
        for (int colNum = 0; colNum < colCount; colNum++) {
            for (int rowNum = 0; rowNum < frozenRowCount; rowNum++) {
                final List<Object> row = rows.get(rowNum);
                if (row.size() <= colNum) {
                    continue;
                }
                final String cellValue = stringify(row.get(colNum));
                if (cellValue == null || cellValue.isBlank()) {
                    //noinspection UnnecessaryContinue
                    continue;
                } else {
                    lastInRow[rowNum] = cellValue;
                }
            }
            colKeys[colNum] = namingConvention.fieldNameFromTexts(lastInRow);
        }
        return colKeys;
    }

    private <M> void pullTab(
        final String tabTitle,
        final Spreadsheet spreadsheet,
        final Sheets.Spreadsheets spreadsheets,
        final ModelSheetAdapter<M> sheetAdapter
    ) {
        final Sheet sheet = BooksSheets.sheetTitled(tabTitle, spreadsheet);
        final List<List<Object>> rows = BooksSheets.sheetValues(tabTitle, spreadsheets);
        final GridProperties gridProperties = sheet.getProperties().getGridProperties();
        final Class<M> modelType = sheetAdapter.getModelType();
        final JsonStore<M> jsonStore = jsonStoreFactory.buildJsonStore(modelType);
        final int frozenRowCount = Optional.ofNullable(gridProperties.getFrozenRowCount()).orElse(0);
        final int colCount = Optional.ofNullable(gridProperties.getColumnCount()).orElse(0);
        final NamingConvention namingConvention = new NamingConvention();
        if (frozenRowCount == 0 || colCount == 0) {
            return;
        }
        final String[] colKeys = getColumnKeys(rows, frozenRowCount, colCount, namingConvention);
        System.out.println(tabTitle + ": " + String.join(", ", colKeys));
        for (int rowNum = frozenRowCount; rowNum < rows.size(); rowNum++) {
            final List<Object> row = rows.get(rowNum);
            if (row == null || row.isEmpty()) {
                continue;
            }
            final M record = sheetAdapter.withNewModel((ma, model) -> {
                for (int colNum = 0; colNum < row.size(); colNum++) {
                    final String colKey = colKeys[colNum];
                    final Object cellValue = row.get(colNum);
                    ma.setKeyValue(model, colKey, cellValue);
                }
            });
            jsonStore.saveIfChanged(record);
        }
    }
}
