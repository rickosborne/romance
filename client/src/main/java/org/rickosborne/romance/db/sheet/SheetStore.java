package org.rickosborne.romance.db.sheet;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.rickosborne.romance.BooksSheets;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.ModelStore;
import org.rickosborne.romance.db.model.ModelSchema;
import org.rickosborne.romance.sheet.ModelSheetAdapter;
import org.rickosborne.romance.util.SheetStuff;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Getter(value = AccessLevel.PROTECTED)
@RequiredArgsConstructor
public class SheetStore<M> implements ModelStore<M>, Iterable<M> {

    @Getter
    private final DbModel dbModel;
    private final ModelSchema<M> modelSchema;
    private final ModelSheetAdapter<M> modelSheetAdapter;
    private final NamingConvention namingConvention;
    private final String userId;
    @Getter(lazy = true)
    private final Sheets.Spreadsheets spreadsheets = BooksSheets.getSpreadsheets(userId);
    @Getter(lazy = true)
    private final Spreadsheet spreadsheet = BooksSheets.getSpreadsheet(getSpreadsheets());
    @Getter(lazy = true)
    private final List<SheetStuff.Indexed<M>> records = SheetStuff.readModels(getDbModel(), getSpreadsheet(), getSpreadsheets(), getModelSheetAdapter());
    @Getter(lazy = true)
    private final Map<String, SheetStuff.Indexed<M>> recordsById = getRecords().stream()
        .collect(Collectors.toMap(i -> idFromModel(i.getModel()), i -> i));
    @Getter(lazy = true)
    private final Sheet sheet = BooksSheets.sheetTitled(getDbModel().getTabTitle(), getSpreadsheet());
    @Getter(lazy = true)
    private final SheetStuff.SheetDescriptor sheetDescriptor = SheetStuff.getSheetDescriptor(getDbModel(), getSpreadsheet(), getSpreadsheets());
    @Getter(lazy = true)
    private final int firstEmptyRowNum = getRecords().size() + getSheetDescriptor().getFrozenRowCount();
    @Getter(lazy = true)
    private final String[] columnKeys = getSheetDescriptor().getColumnKeys();

    @Override
    public M findById(final String id) {
        return findByIdFromCache(id);
    }

    @Override
    public M findByIdFromCache(final String id) {
        final SheetStuff.Indexed<M> maybe = getRecordsById().get(id);
        return maybe == null ? null : maybe.getModel();
    }

    private SheetStuff.Indexed<M> findIndexed(final M model) {
        return getRecordsById().get(idFromModel(model));
    }

    @Override
    public Class<M> getModelType() {
        return dbModel.getModelType();
    }

    public Integer getRowNum(final M model) {
        return Optional.ofNullable(findIndexed(model)).map(SheetStuff.Indexed::getRowNum).orElse(null);
    }

    public boolean hasMatch(@NonNull final Predicate<M> predicate) {
        return stream().anyMatch(predicate);
    }

    @Override
    public String idFromModel(final M model) {
        final List<String> parts = modelSchema.idValuesFromModel(model);
        if (parts.isEmpty()) {
            return null;
        }
        return getNamingConvention().fileNameFromTexts(parts.stream());
    }

    @NotNull
    @Override
    public Iterator<M> iterator() {
        return getRecords().stream()
            .map(SheetStuff.Indexed::getModel)
            .iterator();
    }

    @Override
    public M save(final M model) {
        final SheetStuff.Indexed<M> indexed = findIndexed(model);
        if (indexed == null) {
            throw new IllegalArgumentException("Could not find " + dbModel.getTypeName() + " in sheet: " + model);
        }
        throw new RuntimeException("Not implemented: SheetStore<" + dbModel.getTypeName() + ">.save()");
    }

    @Override
    public Stream<M> stream() {
        return getRecords().stream().map(SheetStuff.Indexed::getModel);
    }

}
