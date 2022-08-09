package org.rickosborne.romance.sheet;

import lombok.Getter;
import lombok.NonNull;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.model.TagModel;
import org.rickosborne.romance.util.BookRating;
import org.rickosborne.romance.util.ModelSetter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class TagSheetAdapter implements ModelSheetAdapter<TagModel> {
    @Getter
    private final DbModel dbModel = DbModel.Tag;
    @Getter
    private final Map<String, BiConsumer<TagModel, Object>> setters = new HashMap<>();

    {
        setters.put("tagName", stringSetter(TagModel::setName));
        setters.put("tagDescription", stringSetter(TagModel::setDescription));
        setters.put("countTotal", intSetter(TagModel::setOwnedCount));
        setters.put("countRated", intSetter(TagModel::setRatedCount));
        setters.put("countHours", doubleSetter(TagModel::setRatedDurationHours));
        setters.put("rateAvgOverall", doubleSetter(ifNotNull((m, r) -> m.getRatings().put(BookRating.Overall, r))));
        setters.put("rateStars", ModelSetter::setNothing);
        setters.put("calcPositiveRate", doubleSetter(TagModel::setPositiveRate));
        setters.put("calcSumDurationPlusMinus", ModelSetter::setNothing);
        setters.put("calcPositiveDuration", doubleSetter(TagModel::setPositiveDurationHours));
        setters.put("calcLikelihoodOfPositive", doubleSetter(TagModel::setPositiveLikelihood));
        setters.put("calcEffectiveLike", doubleSetter(TagModel::setEffectiveLike));
    }

    @Override
    public String fileNameForModel(final @NonNull TagModel model, final @NonNull NamingConvention namingConvention) {
        return namingConvention.fileNameFromTexts(model.getName());
    }

    @Override
    public void setKeyValue(final @NonNull TagModel model, final @NonNull String key, final Object value) {
        final BiConsumer<TagModel, Object> setter = setters.get(key);
        if (setter == null) {
            throw new IllegalArgumentException("Unknown key for TagModel: " + key);
        }
        setter.accept(model, value);
    }
}
