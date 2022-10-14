package org.rickosborne.romance.sheet;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.model.SchemaAttribute;
import org.rickosborne.romance.db.model.WatchAttributes;
import org.rickosborne.romance.db.model.WatchModel;
import org.rickosborne.romance.util.ModelSetter;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WatchSheetAdapter implements ModelSheetAdapter<WatchModel> {
    private final static ModelSetter<WatchModel> WMS = new ModelSetter<>() {
    };
    @Getter
    private final DbModel dbModel = DbModel.Watch;
    @Getter
    private final Map<String, BiConsumer<WatchModel, Object>> setters = Stream
        .of(SheetFields.values())
        .collect(Collectors.toMap(Enum::name, sf -> sf.setter));
    @Getter
    private final Map<SchemaAttribute<WatchModel, ?>, ModelSetter<WatchModel>> sheetFields = Stream
        .of(SheetFields.values())
        .filter(sf -> sf.watchAttribute != null && sf.safeToWriteToSheet)
        .collect(Collectors.toMap(sf -> sf.watchAttribute, sf -> sf));

    @Override
    public String fileNameForModel(final @NonNull WatchModel model, final @NonNull NamingConvention namingConvention) {
        return namingConvention.fileNameFromTexts(model.getAuthorName(), model.getBookTitle());
    }

    @Override
    public void setKeyValue(final @NonNull WatchModel model, final @NonNull String key, final Object value) {
        final BiConsumer<WatchModel, Object> setter = setters.get(key);
        if (setter == null) {
            throw new IllegalArgumentException("Unknown key for WatchModel: " + key);
        }
        setter.accept(model, value);
    }

    @RequiredArgsConstructor
    enum SheetFields implements ModelSetter<WatchModel> {
        watchlistBookTitle(WMS.stringSetter(WatchModel::setBookTitle)),
        watchlistAuthor(WMS.stringSetter(WatchModel::setAuthorName)),
        linkGoodreads(WMS.urlSetter(WatchModel::setGoodreadsUrl)),
        ;
        private final boolean safeToWriteToSheet;
        private final BiConsumer<WatchModel, Object> setter;
        private final WatchAttributes watchAttribute;

        SheetFields(@NonNull final BiConsumer<WatchModel, Object> setter) {
            this(false, setter, null);
        }
    }
}
