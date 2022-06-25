package org.rickosborne.romance.sheet;

import lombok.Getter;
import lombok.NonNull;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.util.BookRating;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public class BookSheetAdapter implements ModelSheetAdapter<BookModel> {
    @Getter
    private final DbModel dbModel = DbModel.Book;
    @Getter
    private final Map<String, BiConsumer<BookModel, Object>> setters = new HashMap<>();

    {
        setters.put("bookTitle", stringSetter(BookModel::setTitle));
        setters.put("bookAuthor", stringSetter(BookModel::setAuthorName));
        setters.put("bookPages", intSetter(BookModel::setPages));
        setters.put("bookPublisher", stringSetter(BookModel::setPublisherName));
        setters.put("audiobookNarrator", stringSetter(BookModel::setNarratorName));
        setters.put("audiobookDurationHours", doubleSetter(BookModel::setDurationHours));
        setters.put("datePublish", localDateSetter(BookModel::setDatePublish));
        setters.put("datePurchase", localDateSetter(BookModel::setDatePurchase));
        setters.put("dateRead", (BookModel m, Object o) -> {
            if ("reading".equals(o)) {
                m.setReading(true);
            } else if ("DNF".equals(o)) {
                m.setDnf(true);
            } else {
                localDateSetter(BookModel::setDateRead).accept(m, o);
            }
        });
        setters.put("linkGoodreads", urlSetter(BookModel::setGoodreadsUrl));
        setters.put("linkAudiobookstore", urlSetter(BookModel::setAudiobookStoreUrl));
        setters.put("linkRickReview", urlSetter(BookModel::setRickReviewUrl));
        setters.put("calcAuthorBookCount", ModelSheetAdapter::setNothing);
        setters.put("calcNarratorBookCount", ModelSheetAdapter::setNothing);
        setters.put("calcDurationFilled", ModelSheetAdapter::setNothing);
        setters.put("mc1Name", stringSetter(ifNotNull((m, s) -> m.getMc1().setName(s))));
        setters.put("mc1Gender", stringSetter(ifNotNull((m, s) -> m.getMc1().setGender(s))));
        setters.put("mc1Pronouns", stringSetter(ifNotNull((m, s) -> m.getMc1().setPronouns(s))));
        setters.put("mc1Age", stringSetter(ifNotNull((m, s) -> m.getMc1().setAge(s))));
        setters.put("mc1Profession", stringSetter(ifNotNull((m, s) -> m.getMc1().setProfession(s))));
        setters.put("mc1Attachment", stringSetter(ifNotNull((m, s) -> m.getMc1().setAttachment(s))));
        setters.put("mc2Name", stringSetter(ifNotNull((m, s) -> m.getMc2().setName(s))));
        setters.put("mc2Gender", stringSetter(ifNotNull((m, s) -> m.getMc2().setGender(s))));
        setters.put("mc2Pronouns", stringSetter(ifNotNull((m, s) -> m.getMc2().setPronouns(s))));
        setters.put("mc2Age", stringSetter(ifNotNull((m, s) -> m.getMc2().setAge(s))));
        setters.put("mc2Profession", stringSetter(ifNotNull((m, s) -> m.getMc2().setProfession(s))));
        setters.put("mc2Attachment", stringSetter(ifNotNull((m, s) -> m.getMc2().setAttachment(s))));
        setters.put("sexScenes", stringSetter(BookModel::setSexScenes));
        setters.put("sexVariety", stringSetter(BookModel::setSexVariety));
        setters.put("sexExplicitness", stringSetter(BookModel::setSexExplicitness));
        setters.put("seriesName", stringSetter(BookModel::setSeriesName));
        setters.put("seriesPart", stringSetter(BookModel::setSeriesPart));
        setters.put("catLocation", stringSetter(BookModel::setLocation));
        setters.put("catSpeed", stringSetter(BookModel::setSpeed));
        setters.put("catEra", stringSetter(BookModel::setGenre));
        setters.put("catNeurodiversity", stringSetter(BookModel::setNeurodiversity));
        setters.put("catPov", stringSetter(BookModel::setPov));
        setters.put("catLike", stringSetter(BookModel::setLike));
        setters.put("catSynopsis", stringSetter(BookModel::setSynopsis));
        setters.put("catSource", stringSetter(BookModel::setSource));
        setters.put("catHea", stringSetter(BookModel::setHea));
        setters.put("feelGoodStuff", stringSetter(BookModel::setFeelGood));
        setters.put("feelBadStuff", stringSetter(BookModel::setFeelBad));
        setters.put("feelOtherStuff", stringSetter(BookModel::setFeelOther));
        setters.put("feelWarnings", stringSetter(BookModel::setWarnings));
        setters.put("tag1", stringSetter(ifNotNull((m, s) -> m.getTags().add(s))));
        setters.put("tag2", stringSetter(ifNotNull((m, s) -> m.getTags().add(s))));
        setters.put("tag3", stringSetter(ifNotNull((m, s) -> m.getTags().add(s))));
        setters.put("tag4", stringSetter(ifNotNull((m, s) -> m.getTags().add(s))));
        setters.put("tag5", stringSetter(ifNotNull((m, s) -> m.getTags().add(s))));
        setters.put("tag6", stringSetter(ifNotNull((m, s) -> m.getTags().add(s))));
        setters.put("tag7", stringSetter(ifNotNull((m, s) -> m.getTags().add(s))));
        setters.put("genTagsJoined", ModelSheetAdapter::setNothing);
        setters.put("rateCharacters", doubleSetter(ifNotNull((m, s) -> m.getRatings().put(BookRating.CharacterDepth, s))));
        setters.put("rateGrowth", doubleSetter(ifNotNull((m, s) -> m.getRatings().put(BookRating.CharacterGrowth, s))));
        setters.put("rateConsistency", doubleSetter(ifNotNull((m, s) -> m.getRatings().put(BookRating.CharacterConsistency, s))));
        setters.put("rateWorld", doubleSetter(ifNotNull((m, s) -> m.getRatings().put(BookRating.World, s))));
        setters.put("rateTension", doubleSetter(ifNotNull((m, s) -> m.getRatings().put(BookRating.Tension, s))));
        setters.put("rateBplot", doubleSetter(ifNotNull((m, s) -> m.getRatings().put(BookRating.BPlot, s))));
        setters.put("rateVibe", doubleSetter(ifNotNull((m, s) -> m.getRatings().put(BookRating.Vibe, s))));
        setters.put("rateHea", doubleSetter(ifNotNull((m, s) -> m.getRatings().put(BookRating.HEA, s))));
        setters.put("rateOverall", doubleSetter(ifNotNull((m, s) -> m.getRatings().put(BookRating.Overall, s))));
        setters.put("scoreZeroOne", ModelSheetAdapter::setNothing);
        setters.put("scorePlusMinus", ModelSheetAdapter::setNothing);
        setters.put("scoreDurationPlusMinus", ModelSheetAdapter::setNothing);
        setters.put("genStars", ModelSheetAdapter::setNothing);
        setters.put("genPairing", stringSetter(BookModel::setPairing));
        setters.put("genNotes", ModelSheetAdapter::setNothing);
        setters.put("genDescription", ModelSheetAdapter::setNothing);
        setters.put("genMdLink", ModelSheetAdapter::setNothing);
        setters.put("genPlainTitle", ModelSheetAdapter::setNothing);
        setters.put("genPlainDescription", ModelSheetAdapter::setNothing);
    }

    @Override
    public String fileNameForModel(
        final @NonNull BookModel model,
        final @NonNull NamingConvention namingConvention
    ) {
        return namingConvention.fileNameFromTexts(
            model.getAuthorName(),
            Optional.ofNullable(model.getDatePublish()).map(LocalDate::getYear).map(String::valueOf).orElse(""),
            model.getTitle()
        );
    }
}
