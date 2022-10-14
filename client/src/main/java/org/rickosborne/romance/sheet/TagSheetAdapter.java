package org.rickosborne.romance.sheet;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.model.SchemaAttribute;
import org.rickosborne.romance.db.model.TagAttributes;
import org.rickosborne.romance.db.model.TagModel;
import org.rickosborne.romance.util.BookRating;
import org.rickosborne.romance.util.ModelSetter;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TagSheetAdapter implements ModelSheetAdapter<TagModel> {
    private final static ModelSetter<TagModel> TMS = new ModelSetter<>() {
    };
    @Getter
    private final DbModel dbModel = DbModel.Tag;
    @Getter
    private final Map<String, BiConsumer<TagModel, Object>> setters = Stream
        .of(SheetFields.values())
        .collect(Collectors.toMap(Enum::name, sf -> sf.setter));
    @Getter
    private final Map<SchemaAttribute<TagModel, ?>, ModelSetter<TagModel>> sheetFields = Stream
        .of(SheetFields.values())
        .filter(sf -> sf.tagAttribute != null && sf.safeToWriteToSheet)
        .collect(Collectors.toMap(sf -> sf.tagAttribute, sf -> sf));

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

    @RequiredArgsConstructor
    enum SheetFields implements ModelSetter<TagModel> {
        tagName(TMS.stringSetter(TagModel::setName)),
        tagDescription(TMS.stringSetter(TagModel::setDescription)),
        countTotal(TMS.intSetter(TagModel::setOwnedCount)),
        countRated(TMS.intSetter(TagModel::setRatedCount)),
        countHours(TMS.doubleSetter(TagModel::setRatedDurationHours)),
        rateAvgOverall(TMS.doubleSetter(TMS.ifNotNull((m, r) -> m.getRatings().put(BookRating.Overall, r)))),
        rateStars(ModelSetter::setNothing),
        calcPositiveRate(TMS.doubleSetter(TagModel::setPositiveRate)),
        calcSumDurationPlusMinus(ModelSetter::setNothing),
        calcPositiveDuration(TMS.doubleSetter(TagModel::setPositiveDurationHours)),
        calcLikelihoodOfPositive(TMS.doubleSetter(TagModel::setPositiveLikelihood)),
        calcEffectiveLike(TMS.doubleSetter(TagModel::setEffectiveLike)),
        ;
        private final boolean safeToWriteToSheet;
        private final BiConsumer<TagModel, Object> setter;
        private final TagAttributes tagAttribute;

        SheetFields(@NonNull final BiConsumer<TagModel, Object> setter) {
            this(false, setter, null);
        }
    }
}
