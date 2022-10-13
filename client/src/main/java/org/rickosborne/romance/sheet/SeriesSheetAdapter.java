package org.rickosborne.romance.sheet;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.Diff;
import org.rickosborne.romance.db.SchemaDiff;
import org.rickosborne.romance.db.model.SeriesAttributes;
import org.rickosborne.romance.db.model.SeriesModel;
import org.rickosborne.romance.util.BookRating;
import org.rickosborne.romance.util.ModelSetter;
import org.rickosborne.romance.util.Pair;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class SeriesSheetAdapter implements ModelSheetAdapter<SeriesModel> {
    private final static ModelSetter<SeriesModel> MS = new ModelSetter<>() {
    };
    private static final Map<SeriesAttributes, SheetFields> sfByAttr = Stream
        .of(SheetFields.values())
        .filter(sf -> sf.seriesAttribute != null && sf.safeToWriteToSheet)
        .collect(Collectors.toMap(sf -> sf.seriesAttribute, sf -> sf));
    @Getter
    private final DbModel dbModel = DbModel.Series;
    @Getter
    private final Map<String, BiConsumer<SeriesModel, Object>> setters = Stream
        .of(SheetFields.values())
        .collect(Collectors.toMap(Enum::name, sf -> sf.setter));

    @Override
    public String fileNameForModel(final @NonNull SeriesModel model, final @NonNull NamingConvention namingConvention) {
        return namingConvention.fileNameFromTexts(model.getName());
    }

    @Override
    public Map<String, String> findChangesToSheet(@NonNull final SeriesModel sheetModel, @NonNull final SeriesModel existing) {
        return new SchemaDiff().diffModels(sheetModel, existing).getChanges().stream()
            .filter(c -> c.getOperation() == Diff.Operation.Add || c.getOperation() == Diff.Operation.Change)
            .map(c -> Pair.build(c, sfByAttr.get((SeriesAttributes) c.getAttribute())))
            .filter(p -> p.hasRight() && p.getLeft().getAfterValue() != null)
            .peek(p -> System.out.printf("%s: %s => %s%n", p.getRight().name(), p.getLeft().getBeforeValue(), p.getLeft().getAfterValue()))
            .collect(Collectors.toMap(p -> p.getRight().name(), p -> p.getLeft().getAfterValue().toString()));
    }

    @RequiredArgsConstructor
    enum SheetFields implements ModelSetter<SeriesModel> {
        seriesName(false, SeriesAttributes.name, MS.stringSetter(SeriesModel::setName)),
        linkAudiobookstore(true, SeriesAttributes.audiobookStoreUrl, MS.urlSetter(SeriesModel::setAudiobooksStoreUrl)),
        linkGoodreads(true, SeriesAttributes.goodreadsUrl, MS.urlSetter(SeriesModel::setGoodreadsUrl)),
        linkStorygraph(true, SeriesAttributes.storyGraphUrl, MS.urlSetter(SeriesModel::setStoryGraphUrl)),
        statsBookCount(false, SeriesAttributes.ownedCount, MS.intSetter(SeriesModel::setOwnedCount)),
        statsAvgRating(false, SeriesAttributes.ratings, MS.doubleSetter(MS.ifNotNull((m, r) -> m.getRatings().put(BookRating.Overall, r)))),
        statsStars(ModelSetter::setNothing);
        private final boolean safeToWriteToSheet;
        private final SeriesAttributes seriesAttribute;
        private final BiConsumer<SeriesModel, Object> setter;

        SheetFields(@NonNull final BiConsumer<SeriesModel, Object> setter) {
            this(false, null, setter);
        }
    }
}
