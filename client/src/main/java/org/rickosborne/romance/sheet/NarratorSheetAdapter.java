package org.rickosborne.romance.sheet;

import lombok.Getter;
import lombok.NonNull;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.model.NarratorModel;
import org.rickosborne.romance.util.ModelSetter;
import org.rickosborne.romance.util.NarratorRating;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class NarratorSheetAdapter implements ModelSheetAdapter<NarratorModel> {
    @Getter
    private final DbModel dbModel = DbModel.Narrator;
    @Getter
    private final Map<String, BiConsumer<NarratorModel, Object>> setters = new HashMap<>();

    {
        setters.put("narratorName", stringSetter(NarratorModel::setName));
        setters.put("narratorGoodreadsLink", urlSetter(NarratorModel::setGoodreadsUrl));
        setters.put("narratorSiteLink", urlSetter(NarratorModel::setSiteUrl));
        setters.put("narratorAccent", stringSetter(NarratorModel::setAccent));
        setters.put("narratorBookCount", intSetter(NarratorModel::setOwnedCount));
        setters.put("rateFemme", doubleSetter(ifNotNull((m, r) -> m.getRatings().put(NarratorRating.Femme, r))));
        setters.put("rateMasc", doubleSetter(ifNotNull((m, r) -> m.getRatings().put(NarratorRating.Masc, r))));
        setters.put("rateMen", doubleSetter(ifNotNull((m, r) -> m.getRatings().put(NarratorRating.Men, r))));
        setters.put("rateKids", doubleSetter(ifNotNull((m, r) -> m.getRatings().put(NarratorRating.Kids, r))));
        setters.put("rateVariety", doubleSetter(ifNotNull((m, r) -> m.getRatings().put(NarratorRating.Variety, r))));
        setters.put("rateClarity", doubleSetter(ifNotNull((m, r) -> m.getRatings().put(NarratorRating.Clarity, r))));
        setters.put("rateAccents", doubleSetter(ifNotNull((m, r) -> m.getRatings().put(NarratorRating.Accents, r))));
        setters.put("rateEmotion", doubleSetter(ifNotNull((m, r) -> m.getRatings().put(NarratorRating.Emotion, r))));
        setters.put("rateDistinctNarrator", doubleSetter((m, r) -> m.getRatings().put(NarratorRating.DistinctNarrator, r)));
        setters.put("negNeg1", stringSetter(ifNotNull((m, s) -> m.getNegatives().add(s))));
        setters.put("negNeg2", stringSetter(ifNotNull((m, s) -> m.getNegatives().add(s))));
        setters.put("negNeg3", stringSetter(ifNotNull((m, s) -> m.getNegatives().add(s))));
        setters.put("negOverallRating", ModelSetter::setNothing);
        setters.put("statsTotalDuration", doubleSetter(NarratorModel::setTotalDurationHours));
        setters.put("statsTotalPages", intSetter(NarratorModel::setTotalPages));
        setters.put("statsAvgPph", ModelSetter::setNothing);
        setters.put("statsBookCount", ModelSetter::setNothing);
        setters.put("statsMeanBookRating", ModelSetter::setNothing);
        setters.put("statsReadCount", ModelSetter::setNothing);
        setters.put("statsStars", ModelSetter::setNothing);
        setters.put("genGoodreadsMarkdownLink", ModelSetter::setNothing);
    }

    @Override
    public String fileNameForModel(final @NonNull NarratorModel model, final @NonNull NamingConvention namingConvention) {
        return namingConvention.fileNameFromTexts(model.getName());
    }
}
