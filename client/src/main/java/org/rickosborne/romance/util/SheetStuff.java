package org.rickosborne.romance.util;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.GridProperties;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.rickosborne.romance.BooksSheets;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.sheet.ModelSheetAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.rickosborne.romance.util.StringStuff.stringify;

public class SheetStuff {

    public static String[] getColumnKeys(
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

    public static <M> List<Indexed<M>> readModels(
        final DbModel dbModel,
        final Spreadsheet spreadsheet,
        final Sheets.Spreadsheets spreadsheets,
        final ModelSheetAdapter<M> sheetAdapter
    ) {
        final String tabTitle = dbModel.getTabTitle();
        final Sheet sheet = BooksSheets.sheetTitled(tabTitle, spreadsheet);
        final List<List<Object>> rows = BooksSheets.sheetValues(tabTitle, spreadsheets);
        final GridProperties gridProperties = sheet.getProperties().getGridProperties();
        final int frozenRowCount = Optional.ofNullable(gridProperties.getFrozenRowCount()).orElse(0);
        final int colCount = Optional.ofNullable(gridProperties.getColumnCount()).orElse(0);
        final NamingConvention namingConvention = new NamingConvention();
        if (frozenRowCount == 0 || colCount == 0) {
            return Collections.emptyList();
        }
        final String[] colKeys = getColumnKeys(rows, frozenRowCount, colCount, namingConvention);
        final List<Indexed<M>> records = new ArrayList<>(rows.size() - frozenRowCount);
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
            records.add(new Indexed<>(record, rowNum));
        }
        return records;
    }

    @RequiredArgsConstructor
    @Getter
    public static class Indexed<M> {
        private final M model;
        private final int rowNum;
    }
}
