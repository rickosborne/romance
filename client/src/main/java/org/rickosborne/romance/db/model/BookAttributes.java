package org.rickosborne.romance.db.model;

import lombok.Getter;
import lombok.NonNull;

import java.net.URL;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

public enum BookAttributes implements SchemaAttribute<BookModel, Object> {
    audiobookStoreUrl(BookModel::getAudiobookStoreUrl, BookModel::setAudiobookStoreUrl, URL.class),
    authorName(BookModel::getAuthorName, BookModel::setAuthorName, String.class),
    datePublish(BookModel::getDatePublish, BookModel::setDatePublish, LocalDate.class),
    datePurchase(BookModel::getDatePurchase, BookModel::setDatePurchase, LocalDate.class),
    dateRead(BookModel::getDateRead, BookModel::setDateRead, LocalDate.class),
    dnf(BookModel::getDnf, BookModel::setDnf, Boolean.class),
    durationHours(BookModel::getDurationHours, BookModel::setDurationHours, Double.class),
    feelBad(BookModel::getFeelBad, BookModel::setFeelBad, String.class),
    feelGood(BookModel::getFeelGood, BookModel::setFeelGood, String.class),
    feelOther(BookModel::getFeelOther, BookModel::setFeelOther, String.class),
    genre(BookModel::getGenre, BookModel::setGenre, String.class),
    goodreadsUrl(BookModel::getGoodreadsUrl, BookModel::setGoodreadsUrl, URL.class),
    hea(BookModel::getHea, BookModel::setHea, String.class),
    like(BookModel::getLike, BookModel::setLike, String.class),
    location(BookModel::getLocation, BookModel::setLocation, String.class),
    mc1(BookModel::getMc1, null, BookModel.MainChar.class),
    mc2(BookModel::getMc2, null, BookModel.MainChar.class),
    narratorName(BookModel::getNarratorName, BookModel::setNarratorName, String.class),
    neurodiversity(BookModel::getNeurodiversity, BookModel::setNeurodiversity, String.class),
    pages(BookModel::getPages, BookModel::setPages, Integer.class),
    pairing(BookModel::getPairing, BookModel::setPairing, String.class),
    pov(BookModel::getPov, BookModel::setPov, String.class),
    publisherName(BookModel::getPublisherName, BookModel::setPublisherName, String.class),
    ratings(BookModel::getRatings, null, Map.class),
    reading(BookModel::getReading, BookModel::setReading, Boolean.class),
    rickReviewUrl(BookModel::getRickReviewUrl, BookModel::setRickReviewUrl, URL.class),
    seriesName(BookModel::getSeriesName, BookModel::setSeriesName, String.class),
    seriesPart(BookModel::getSeriesPart, BookModel::setSeriesPart, String.class),
    sexExplicitness(BookModel::getSexExplicitness, BookModel::setSexExplicitness, String.class),
    sexScenes(BookModel::getSexScenes, BookModel::setSexScenes, String.class),
    sexVariety(BookModel::getSexVariety, BookModel::setSexVariety, String.class),
    source(BookModel::getSource, BookModel::setSource, String.class),
    speed(BookModel::getSpeed, BookModel::setSpeed, String.class),
    synopsis(BookModel::getSynopsis, BookModel::setSynopsis, String.class),
    tags(BookModel::getTags, null, Set.class),
    title(BookModel::getTitle, BookModel::setTitle, String.class),
    warnings(BookModel::getWarnings, BookModel::setWarnings, String.class),
    ;

    private final Function<BookModel, ?> accessor;
    private final Class<?> attributeType;
    private final BiConsumer<BookModel, Object> mutator;

    <T> BookAttributes(final Function<BookModel, T> accessor, final BiConsumer<BookModel, T> mutator, final Class<T> attributeType) {
        this.accessor = accessor;
        @SuppressWarnings("unchecked") final BiConsumer<BookModel, Object> typedMutator = (BiConsumer<BookModel, Object>) mutator;
        this.mutator = typedMutator;
        this.attributeType = attributeType;
    }

    public Object getAttribute(@NonNull final BookModel model) {
        return accessor.apply(model);
    }

    @Override
    public Class<Object> getAttributeType() {
        @SuppressWarnings("unchecked") final Class<Object> typed = (Class<Object>) attributeType;
        return typed;
    }

    @Override
    public String getAttributeName() {
        return name();
    }

    @Override
    public Class<BookModel> getModelType() {
        return BookModel.class;
    }

    public void setAttribute(@NonNull final BookModel model, final Object value) {
        if (mutator == null) {
            throw new NullPointerException("BookModel cannot set " + name());
        }
        if ((value != null) && !attributeType.isInstance(value)) {
            throw new IllegalArgumentException(String.format("Expected %s but found %s", attributeType.getSimpleName(), value.getClass().getSimpleName()));
        }
        mutator.accept(model, value);
    }

}
