package org.rickosborne.romance.sheet;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.model.AuthorAttributes;
import org.rickosborne.romance.db.model.AuthorModel;
import org.rickosborne.romance.db.model.SchemaAttribute;
import org.rickosborne.romance.util.BookRating;
import org.rickosborne.romance.util.ModelSetter;
import org.rickosborne.romance.util.YesNoUnknown;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class AuthorSheetAdapter implements ModelSheetAdapter<AuthorModel> {
    private final static ModelSetter<AuthorModel> AMS = new ModelSetter<>() {
    };
    @Getter
    private final DbModel dbModel = DbModel.Author;
    @Getter
    private final Map<String, BiConsumer<AuthorModel, Object>> setters = Stream
        .of(SheetFields.values())
        .collect(Collectors.toMap(Enum::name, sf -> sf.setter));
    @Getter
    private final Map<SchemaAttribute<AuthorModel, ?>, ModelSetter<AuthorModel>> sheetFields = Stream
        .of(SheetFields.values())
        .filter(sf -> sf.authorAttribute != null && sf.safeToWriteToSheet)
        .collect(Collectors.toMap(sf -> sf.authorAttribute, sf -> sf));

    @Override
    public String fileNameForModel(final @NonNull AuthorModel model, final @NonNull NamingConvention namingConvention) {
        return namingConvention.fileNameFromTexts(model.getName());
    }

    @RequiredArgsConstructor
    enum SheetFields implements ModelSetter<AuthorModel> {
        authorName(AuthorAttributes.name, false, AMS.stringSetter(AuthorModel::setName)),
        authorGoodreadsLink(AuthorAttributes.goodreadsUrl, true, AMS.urlSetter(AuthorModel::setGoodreadsUrl)),
        authorAudiobookstoreLink(AuthorAttributes.audiobookStoreUrl, true, AMS.urlSetter(AuthorModel::setAudiobookStoreUrl)),
        authorStorygraphLink(AuthorAttributes.storyGraphUrl, true, AMS.urlSetter(AuthorModel::setStoryGraphUrl)),
        authorSiteLink(AuthorAttributes.siteUrl, true, AMS.urlSetter(AuthorModel::setSiteUrl)),
        authorTwitter(AuthorAttributes.twitterName, false, AMS.stringSetter(AuthorModel::setTwitterName)),
        authorPronouns(AuthorAttributes.pronouns, false, AMS.stringSetter(AuthorModel::setPronouns)),
        authorQueer(AuthorAttributes.queer, false, AMS.stringSetter((m, q) -> m.setQueer(YesNoUnknown.fromString(q)))),
        authorBookCount(AuthorAttributes.ownedCount, false, AMS.intSetter(AuthorModel::setOwnedCount)),
        authorRatedCount(AuthorAttributes.ratedCount, false, AMS.intSetter(AuthorModel::setRatedCount)),
        authorRep(AuthorAttributes.rep, false, AMS.stringSetter(AuthorModel::setRep)),
        rateCharacters(AMS.doubleSetter(AMS.ifNotNull((m, r) -> m.getRatings().put(BookRating.CharacterDepth, r)))),
        rateGrowth(AMS.doubleSetter(AMS.ifNotNull((m, r) -> m.getRatings().put(BookRating.CharacterGrowth, r)))),
        rateConsistency(AMS.doubleSetter(AMS.ifNotNull((m, r) -> m.getRatings().put(BookRating.CharacterConsistency, r)))),
        rateWorld(AMS.doubleSetter(AMS.ifNotNull((m, r) -> m.getRatings().put(BookRating.World, r)))),
        rateTension(AMS.doubleSetter(AMS.ifNotNull((m, r) -> m.getRatings().put(BookRating.Tension, r)))),
        rateBplot(AMS.doubleSetter(AMS.ifNotNull((m, r) -> m.getRatings().put(BookRating.BPlot, r)))),
        rateVibe(AMS.doubleSetter(AMS.ifNotNull((m, r) -> m.getRatings().put(BookRating.Vibe, r)))),
        rateHea(AMS.doubleSetter(AMS.ifNotNull((m, r) -> m.getRatings().put(BookRating.HEA, r)))),
        rateOverall(AMS.doubleSetter(AMS.ifNotNull((m, r) -> m.getRatings().put(BookRating.Overall, r)))),
        rateDnfCount(AuthorAttributes.dnfCount, false, AMS.intSetter(AuthorModel::setDnfCount)),
        rate4(AuthorAttributes.fourStarPlusCount, false, AMS.intSetter(AuthorModel::setFourStarPlusCount)),
        rate5(AuthorAttributes.fiveStarCount, false, AMS.intSetter(AuthorModel::setFiveStarCount)),
        rateMin(AuthorAttributes.minRating, false, AMS.doubleSetter(AuthorModel::setMinRating)),
        rateMax(AuthorAttributes.maxRating, false, AMS.doubleSetter(AuthorModel::setMaxRating)),
        rateRange(ModelSetter::setNothing),
        rateOdds4(AuthorAttributes.odds4, false, AMS.doubleSetter(AuthorModel::setOdds4)),
        statsAvgPages(AuthorAttributes.meanPages, false, AMS.doubleSetter(AuthorModel::setMeanPages)),
        statsAvgDuration(AuthorAttributes.meanDurationHours, false, AMS.doubleSetter(AuthorModel::setMeanDurationHours)),
        statsStars(AuthorAttributes.stars, false, AMS.stringSetter(AuthorModel::setStars)),
        genTwitterLink(AuthorAttributes.twitterUrl, false, AMS.urlSetter(AuthorModel::setTwitterUrl)),
        genGoodreads(ModelSetter::setNothing),
        genRating(ModelSetter::setNothing),
        ;
        private final AuthorAttributes authorAttribute;
        private final boolean safeToWriteToSheet;
        private final BiConsumer<AuthorModel, Object> setter;

        SheetFields(@NonNull final BiConsumer<AuthorModel, Object> setter) {
            this(null, false, setter);
        }
    }
}
