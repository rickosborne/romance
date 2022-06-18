package org.rickosborne.romance.sheet;

import lombok.Getter;
import lombok.NonNull;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.model.WatchModel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class WatchSheetAdapter implements ModelSheetAdapter<WatchModel> {
    @Getter
    private final DbModel dbModel = DbModel.Watch;
    @Getter
    private final Map<String, BiConsumer<WatchModel, Object>> setters = new HashMap<>();

    {
        setters.put("watchlistBookTitle", stringSetter(WatchModel::setBookTitle));
        setters.put("watchlistAuthor", stringSetter(WatchModel::setAuthorName));
        setters.put("linkGoodreads", urlSetter(WatchModel::setGoodreadsUrl));
    }

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
}
