package org.rickosborne.romance.sheet;

import lombok.Getter;
import lombok.NonNull;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.model.SeriesModel;
import org.rickosborne.romance.util.BookRating;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class SeriesSheetAdapter implements ModelSheetAdapter<SeriesModel> {
    @Getter
    private final DbModel dbModel = DbModel.Series;
    @Getter
    private final Map<String, BiConsumer<SeriesModel, Object>> setters = new HashMap<>();

    {
        setters.put("seriesName", stringSetter(SeriesModel::setName));
        setters.put("linkGoodreads", urlSetter(SeriesModel::setGoodreadsUrl));
        setters.put("statsBookCount", intSetter(SeriesModel::setOwnedCount));
        setters.put("statsAvgRating", doubleSetter(ifNotNull((m, r) -> m.getRatings().put(BookRating.Overall, r))));
        setters.put("statsStars", ModelSheetAdapter::setNothing);
    }

    @Override
    public String fileNameForModel(final @NonNull SeriesModel model, final @NonNull NamingConvention namingConvention) {
        return namingConvention.fileNameFromTexts(model.getName());
    }
}
