package org.rickosborne.romance.sheet;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.Diff;
import org.rickosborne.romance.db.SchemaDiff;
import org.rickosborne.romance.db.model.BookAttributes;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.util.BookRating;
import org.rickosborne.romance.util.ModelSetter;
import org.rickosborne.romance.util.Pair;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.rickosborne.romance.util.StringStuff.setButNot;

@Slf4j
public class BookSheetAdapter implements ModelSheetAdapter<BookModel> {
    private final static ModelSetter<BookModel> BS = new ModelSetter<>() {
    };
    public static final String DNF_SENTINEL = "DNF";
    public static final String READING_SENTINEL = "reading";
    private static final Map<BookAttributes, SheetFields> sfByAttr = Stream
        .of(SheetFields.values())
        .filter(sf -> sf.bookAttribute != null && sf.safeToWriteToSheet)
        .collect(Collectors.toMap(sf -> sf.bookAttribute, sf -> sf));
    @Getter
    private final DbModel dbModel = DbModel.Book;
    @Getter
    private final Map<String, BiConsumer<BookModel, Object>> setters = Stream
        .of(SheetFields.values())
        .collect(Collectors.toMap(Enum::name, sf -> sf.setter));

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

    @Override
    public Map<String, String> findChangesToSheet(@NonNull final BookModel sheetBook, @NonNull final BookModel existing) {
        return new SchemaDiff().diffModels(sheetBook, existing).getChanges().stream()
            .filter(c -> c.getOperation() == Diff.Operation.Add || c.getOperation() == Diff.Operation.Change)
            .map(c -> Pair.build(c, sfByAttr.get((BookAttributes) c.getAttribute())))
            .filter(p -> p.hasRight() && p.getLeft().getAfterValue() != null)
            .peek(p -> System.out.printf("%s: %s => %s%n", p.getRight().name(), p.getLeft().getBeforeValue(), p.getLeft().getAfterValue()))
            .collect(Collectors.toMap(p -> p.getRight().name(), p -> p.getLeft().getAfterValue().toString()));
    }

    @RequiredArgsConstructor
    enum SheetFields implements ModelSetter<BookModel> {
        bookTitle(BookAttributes.title, true, BS.stringSetter(BookModel::setTitle)),
        bookAuthor(BookAttributes.authorName, true, BS.stringSetter(BookModel::setAuthorName)),
        bookPages(BookAttributes.pages, true, BS.intSetter(BookModel::setPages)),
        bookPublisher(BookAttributes.publisherName, true, BS.stringSetter(BookModel::setPublisherName)),
        audiobookNarrator(BookAttributes.narratorName, true, BS.stringSetter(BookModel::setNarratorName)),
        audiobookDurationHours(BookAttributes.durationHours, true, BS.doubleSetter(BookModel::setDurationHours)),
        datePublish(BookAttributes.datePublish, true, BS.localDateSetter(BookModel::setDatePublish)),
        datePurchase(BookAttributes.datePurchase, true, BS.localDateSetter(BookModel::setDatePurchase)),
        dateRead((BookModel m, Object o) -> {
            if ((o instanceof String) && ((String) o).contains(READING_SENTINEL)) {
                m.setReading(true);
            } else if ((o instanceof String) && ((String) o).contains(DNF_SENTINEL)) {
                m.setDnf(true);
            } else {
                BS.localDateSetter(BookModel::setDateRead).accept(m, o);
            }
        }),
        isbn(BookAttributes.isbn, true, BS.stringSetter(setButNot(BookModel::setIsbn, "null", ""))),
        linkGoodreads(BookAttributes.goodreadsUrl, true, BS.urlSetter(BookModel::setGoodreadsUrl)),
        linkAudiobookstore(BookAttributes.audiobookStoreUrl, true, BS.urlSetter(BookModel::setAudiobookStoreUrl)),
        linkRickReview(BookAttributes.rickReviewUrl, false, BS.urlSetter(BookModel::setRickReviewUrl)),
        linkStorygraph(BookAttributes.storygraphUrl, true, BS.urlSetter(BookModel::setStorygraphUrl)),
        linkImage(BookAttributes.imageUrl, true, BS.urlSetter(BookModel::setImageUrl)),
        calcAuthorBookCount(ModelSetter::setNothing),
        calcNarratorBookCount(ModelSetter::setNothing),
        calcDurationFilled(ModelSetter::setNothing),
        mc1Name(BS.stringSetter(BS.ifNotNull((m, s) -> m.getMc1().setName(s)))),
        mc1Gender(BS.stringSetter(BS.ifNotNull((m, s) -> m.getMc1().setGender(s)))),
        mc1Pronouns(BS.stringSetter(BS.ifNotNull((m, s) -> m.getMc1().setPronouns(s)))),
        mc1Age(BS.stringSetter(BS.ifNotNull((m, s) -> m.getMc1().setAge(s)))),
        mc1Profession(BS.stringSetter(BS.ifNotNull((m, s) -> m.getMc1().setProfession(s)))),
        mc1Attachment(BS.stringSetter(BS.ifNotNull((m, s) -> m.getMc1().setAttachment(s)))),
        mc2Name(BS.stringSetter(BS.ifNotNull((m, s) -> m.getMc2().setName(s)))),
        mc2Gender(BS.stringSetter(BS.ifNotNull((m, s) -> m.getMc2().setGender(s)))),
        mc2Pronouns(BS.stringSetter(BS.ifNotNull((m, s) -> m.getMc2().setPronouns(s)))),
        mc2Age(BS.stringSetter(BS.ifNotNull((m, s) -> m.getMc2().setAge(s)))),
        mc2Profession(BS.stringSetter(BS.ifNotNull((m, s) -> m.getMc2().setProfession(s)))),
        mc2Attachment(BS.stringSetter(BS.ifNotNull((m, s) -> m.getMc2().setAttachment(s)))),
        sexScenes(BookAttributes.sexScenes, false, BS.stringSetter(BookModel::setSexScenes)),
        sexVariety(BookAttributes.sexVariety, false, BS.stringSetter(BookModel::setSexVariety)),
        sexExplicitness(BookAttributes.sexExplicitness, false, BS.stringSetter(BookModel::setSexExplicitness)),
        seriesName(BookAttributes.seriesName, true, BS.stringSetter(BookModel::setSeriesName)),
        seriesPart(BookAttributes.seriesPart, true, BS.stringSetter(BookModel::setSeriesPart)),
        catLocation(BookAttributes.location, false, BS.stringSetter(BookModel::setLocation)),
        catSpeed(BookAttributes.speed, false, BS.stringSetter(BookModel::setSpeed)),
        catEra(BookAttributes.genre, false, BS.stringSetter(BookModel::setGenre)),
        catRepresentation(BookAttributes.neurodiversity, false, BS.stringSetter(BookModel::setNeurodiversity)),
        catPov(BookAttributes.pov, false, BS.stringSetter(BookModel::setPov)),
        catLike(BookAttributes.like, false, BS.stringSetter(BookModel::setLike)),
        catSynopsis(BookAttributes.synopsis, false, BS.stringSetter(BookModel::setSynopsis)),
        catSource(BookAttributes.source, false, BS.stringSetter(BookModel::setSource)),
        catBreakup(BookAttributes.breakup, false, BS.stringSetter(BookModel::setBreakup)),
        catHeaSpoilers(BookAttributes.hea, false, BS.stringSetter(BookModel::setHea)),
        feelGoodStuff(BookAttributes.feelGood, false, BS.stringSetter(BookModel::setFeelGood)),
        feelBadStuff(BookAttributes.feelBad, false, BS.stringSetter(BookModel::setFeelBad)),
        feelOtherStuff(BookAttributes.feelOther, false, BS.stringSetter(BookModel::setFeelOther)),
        feelWarnings(BookAttributes.warnings, false, BS.stringSetter(BookModel::setWarnings)),
        tag1(BS.stringSetter(BS.ifNotNull((m, s) -> m.getTags().add(s)))),
        tag2(BS.stringSetter(BS.ifNotNull((m, s) -> m.getTags().add(s)))),
        tag3(BS.stringSetter(BS.ifNotNull((m, s) -> m.getTags().add(s)))),
        tag4(BS.stringSetter(BS.ifNotNull((m, s) -> m.getTags().add(s)))),
        tag5(BS.stringSetter(BS.ifNotNull((m, s) -> m.getTags().add(s)))),
        tag6(BS.stringSetter(BS.ifNotNull((m, s) -> m.getTags().add(s)))),
        tag7(BS.stringSetter(BS.ifNotNull((m, s) -> m.getTags().add(s)))),
        tag8(BS.stringSetter(BS.ifNotNull((m, s) -> m.getTags().add(s)))),
        tag9(BS.stringSetter(BS.ifNotNull((m, s) -> m.getTags().add(s)))),
        tag10(BS.stringSetter(BS.ifNotNull((m, s) -> m.getTags().add(s)))),
        genTagsJoined(ModelSetter::setNothing),
        rateCharacters(BS.doubleSetter(BS.ifNotNull((m, s) -> m.getRatings().put(BookRating.CharacterDepth, s)))),
        rateGrowth(BS.doubleSetter(BS.ifNotNull((m, s) -> m.getRatings().put(BookRating.CharacterGrowth, s)))),
        rateConsistency(BS.doubleSetter(BS.ifNotNull((m, s) -> m.getRatings().put(BookRating.CharacterConsistency, s)))),
        rateWorld(BS.doubleSetter(BS.ifNotNull((m, s) -> m.getRatings().put(BookRating.World, s)))),
        rateTension(BS.doubleSetter(BS.ifNotNull((m, s) -> m.getRatings().put(BookRating.Tension, s)))),
        rateBplot(BS.doubleSetter(BS.ifNotNull((m, s) -> m.getRatings().put(BookRating.BPlot, s)))),
        rateVibe(BS.doubleSetter(BS.ifNotNull((m, s) -> m.getRatings().put(BookRating.Vibe, s)))),
        rateResolution(BS.doubleSetter(BS.ifNotNull((m, s) -> m.getRatings().put(BookRating.HEA, s)))),
        rateOverall(BS.doubleSetter(BS.ifNotNull((m, s) -> m.getRatings().put(BookRating.Overall, s)))),
        scoreZeroOne(ModelSetter::setNothing),
        scorePlusMinus(ModelSetter::setNothing),
        scoreDurationPlusMinus(ModelSetter::setNothing),
        scoreLikeAuthor(ModelSetter::setNothing),
        scoreLikeTags(ModelSetter::setNothing),
        scoreLikePairing(ModelSetter::setNothing),
        scoreLikeBook(ModelSetter::setNothing),
        scoreLikeRating(ModelSetter::setNothing),
        scoreLikeError(ModelSetter::setNothing),
        genStars(ModelSetter::setNothing),
        genPairing(BS.stringSetter(BookModel::setPairing)),
        genNotes(ModelSetter::setNothing),
        genDescription(ModelSetter::setNothing),
        genMdLink(ModelSetter::setNothing),
        genMdLinkPlus(ModelSetter::setNothing),
        genPlainTitle(ModelSetter::setNothing),
        genPlainDescription(ModelSetter::setNothing),
        ;
        private final BookAttributes bookAttribute;
        private final boolean safeToWriteToSheet;
        private final BiConsumer<BookModel, Object> setter;

        SheetFields(@NonNull final BiConsumer<BookModel, Object> setter) {
            this(null, false, setter);
        }
    }
}
