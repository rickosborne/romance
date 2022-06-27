package org.rickosborne.romance.sheet;

import lombok.Getter;
import lombok.NonNull;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.model.AuthorModel;
import org.rickosborne.romance.util.BookRating;
import org.rickosborne.romance.util.ModelSetter;
import org.rickosborne.romance.util.YesNoUnknown;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class AuthorSheetAdapter implements ModelSheetAdapter<AuthorModel> {
    @Getter
    private final DbModel dbModel = DbModel.Author;
    @Getter
    private final Map<String, BiConsumer<AuthorModel, Object>> setters = new HashMap<>();

    {
        setters.put("authorName", stringSetter(AuthorModel::setName));
        setters.put("authorGoodreadsLink", urlSetter(AuthorModel::setGoodreadsUrl));
        setters.put("authorSiteLink", urlSetter(AuthorModel::setSiteUrl));
        setters.put("authorTwitter", stringSetter(AuthorModel::setTwitterName));
        setters.put("authorPronouns", stringSetter(AuthorModel::setPronouns));
        setters.put("authorQueer", stringSetter((m, q) -> m.setQueer(YesNoUnknown.fromString(q))));
        setters.put("authorBookCount", intSetter(AuthorModel::setOwnedCount));
        setters.put("authorRatedCount", intSetter(AuthorModel::setRatedCount));
        setters.put("rateCharacters", doubleSetter(ifNotNull((m, r) -> m.getRatings().put(BookRating.CharacterDepth, r))));
        setters.put("rateGrowth", doubleSetter(ifNotNull((m, r) -> m.getRatings().put(BookRating.CharacterGrowth, r))));
        setters.put("rateConsistency", doubleSetter(ifNotNull((m, r) -> m.getRatings().put(BookRating.CharacterConsistency, r))));
        setters.put("rateWorld", doubleSetter(ifNotNull((m, r) -> m.getRatings().put(BookRating.World, r))));
        setters.put("rateTension", doubleSetter(ifNotNull((m, r) -> m.getRatings().put(BookRating.Tension, r))));
        setters.put("rateBplot", doubleSetter(ifNotNull((m, r) -> m.getRatings().put(BookRating.BPlot, r))));
        setters.put("rateVibe", doubleSetter(ifNotNull((m, r) -> m.getRatings().put(BookRating.Vibe, r))));
        setters.put("rateHea", doubleSetter(ifNotNull((m, r) -> m.getRatings().put(BookRating.HEA, r))));
        setters.put("rateOverall", doubleSetter(ifNotNull((m, r) -> m.getRatings().put(BookRating.Overall, r))));
        setters.put("rate4", intSetter(AuthorModel::setFourStarPlusCount));
        setters.put("rate5", intSetter(AuthorModel::setFiveStarCount));
        setters.put("rateMin", doubleSetter(AuthorModel::setMinRating));
        setters.put("rateMax", doubleSetter(AuthorModel::setMaxRating));
        setters.put("rateRange", ModelSetter::setNothing);
        setters.put("statsAvgPages", doubleSetter(AuthorModel::setMeanPages));
        setters.put("statsAvgDuration", doubleSetter(AuthorModel::setMeanDurationHours));
        setters.put("statsStars", stringSetter(AuthorModel::setStars));
        setters.put("genTwitterLink", urlSetter(AuthorModel::setTwitterUrl));
        setters.put("genGoodreads", ModelSetter::setNothing);
        setters.put("genRating", ModelSetter::setNothing);
    }

    @Override
    public String fileNameForModel(final @NonNull AuthorModel model, final @NonNull NamingConvention namingConvention) {
        return namingConvention.fileNameFromTexts(model.getName());
    }
}
