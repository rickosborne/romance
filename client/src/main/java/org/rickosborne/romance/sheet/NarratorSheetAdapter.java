package org.rickosborne.romance.sheet;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.model.NarratorAttributes;
import org.rickosborne.romance.db.model.NarratorModel;
import org.rickosborne.romance.db.model.SchemaAttribute;
import org.rickosborne.romance.util.ModelSetter;
import org.rickosborne.romance.util.NarratorRating;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NarratorSheetAdapter implements ModelSheetAdapter<NarratorModel> {
    private final static ModelSetter<NarratorModel> NMS = new ModelSetter<>() {
    };
    @Getter
    private final DbModel dbModel = DbModel.Narrator;
    @Getter
    private final Map<String, BiConsumer<NarratorModel, Object>> setters = Stream
        .of(SheetFields.values())
        .collect(Collectors.toMap(Enum::name, sf -> sf.setter));
    @Getter
    private final Map<SchemaAttribute<NarratorModel, ?>, ModelSetter<NarratorModel>> sheetFields = Stream
        .of(SheetFields.values())
        .filter(sf -> sf.narratorAttribute != null && sf.safeToWriteToSheet)
        .collect(Collectors.toMap(sf -> sf.narratorAttribute, sf -> sf));

    @Override
    public String fileNameForModel(final @NonNull NarratorModel model, final @NonNull NamingConvention namingConvention) {
        return namingConvention.fileNameFromTexts(model.getName());
    }

    @RequiredArgsConstructor
    enum SheetFields implements ModelSetter<NarratorModel> {
        narratorName(NMS.stringSetter(NarratorModel::setName)),
        narratorGoodreadsLink(NMS.urlSetter(NarratorModel::setGoodreadsUrl)),
        narratorSiteLink(NMS.urlSetter(NarratorModel::setSiteUrl)),
        narratorAccent(NMS.stringSetter(NarratorModel::setAccent)),
        narratorBookCount(NMS.intSetter(NarratorModel::setOwnedCount)),
        rateFemme(NMS.doubleSetter(NMS.ifNotNull((m, r) -> m.getRatings().put(NarratorRating.Femme, r)))),
        rateMasc(NMS.doubleSetter(NMS.ifNotNull((m, r) -> m.getRatings().put(NarratorRating.Masc, r)))),
        rateMen(NMS.doubleSetter(NMS.ifNotNull((m, r) -> m.getRatings().put(NarratorRating.Men, r)))),
        rateKids(NMS.doubleSetter(NMS.ifNotNull((m, r) -> m.getRatings().put(NarratorRating.Kids, r)))),
        rateVariety(NMS.doubleSetter(NMS.ifNotNull((m, r) -> m.getRatings().put(NarratorRating.Variety, r)))),
        rateClarity(NMS.doubleSetter(NMS.ifNotNull((m, r) -> m.getRatings().put(NarratorRating.Clarity, r)))),
        rateAccents(NMS.doubleSetter(NMS.ifNotNull((m, r) -> m.getRatings().put(NarratorRating.Accents, r)))),
        rateEmotion(NMS.doubleSetter(NMS.ifNotNull((m, r) -> m.getRatings().put(NarratorRating.Emotion, r)))),
        rateDistinctNarrator(NMS.doubleSetter((m, r) -> m.getRatings().put(NarratorRating.DistinctNarrator, r))),
        negNeg1(NMS.stringSetter(NMS.ifNotNull((m, s) -> m.getNegatives().add(s)))),
        negNeg2(NMS.stringSetter(NMS.ifNotNull((m, s) -> m.getNegatives().add(s)))),
        negNeg3(NMS.stringSetter(NMS.ifNotNull((m, s) -> m.getNegatives().add(s)))),
        negOverallRating(ModelSetter::setNothing),
        statsTotalDuration(NMS.doubleSetter(NarratorModel::setTotalDurationHours)),
        statsTotalPages(NMS.intSetter(NarratorModel::setTotalPages)),
        statsAvgPph(ModelSetter::setNothing),
        statsBookCount(ModelSetter::setNothing),
        statsMeanBookRating(ModelSetter::setNothing),
        statsReadCount(ModelSetter::setNothing),
        statsStars(ModelSetter::setNothing),
        genGoodreadsMarkdownLink(ModelSetter::setNothing),
        ;
        private final NarratorAttributes narratorAttribute;
        private final boolean safeToWriteToSheet;
        private final BiConsumer<NarratorModel, Object> setter;

        SheetFields(@NonNull final BiConsumer<NarratorModel, Object> setter) {
            this(null, false, setter);
        }
    }
}
