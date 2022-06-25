package org.rickosborne.romance.db.model;

import lombok.NonNull;
import org.rickosborne.romance.util.YesNoUnknown;

import java.net.URL;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public enum AuthorAttributes implements SchemaAttribute<AuthorModel, Object> {
    fiveStarCount(AuthorModel::getFiveStarCount, AuthorModel::setFiveStarCount, Integer.class),
    fourStarPlusCount(AuthorModel::getFourStarPlusCount, AuthorModel::setFourStarPlusCount, Integer.class),
    goodreadsUrl(AuthorModel::getGoodreadsUrl, AuthorModel::setGoodreadsUrl, URL.class),
    maxRating(AuthorModel::getMaxRating, AuthorModel::setMaxRating, Double.class),
    meanDurationHours(AuthorModel::getMeanDurationHours, AuthorModel::setMeanDurationHours, Double.class),
    meanPages(AuthorModel::getMeanPages, AuthorModel::setMeanPages, Double.class),
    minRating(AuthorModel::getMinRating, AuthorModel::setMinRating, Double.class),
    name(AuthorModel::getName, AuthorModel::setName, String.class),
    ownedCount(AuthorModel::getOwnedCount, AuthorModel::setOwnedCount, Integer.class),
    pronouns(AuthorModel::getPronouns, AuthorModel::setPronouns, String.class),
    queer(AuthorModel::getQueer, AuthorModel::setQueer, YesNoUnknown.class),
    ratedCount(AuthorModel::getRatedCount, AuthorModel::setRatedCount, Integer.class),
    ratings(AuthorModel::getRatings, null, Map.class),
    siteUrl(AuthorModel::getSiteUrl, AuthorModel::setSiteUrl, URL.class),
    stars(AuthorModel::getStars, AuthorModel::setStars, String.class),
    twitterName(AuthorModel::getTwitterName, AuthorModel::setTwitterName, String.class),
    twitterUrl(AuthorModel::getTwitterUrl, AuthorModel::setTwitterUrl, URL.class),
    ;
    private final Function<AuthorModel, ?> accessor;
    private final Class<?> attributeType;
    private final BiConsumer<AuthorModel, Object> mutator;

    <T> AuthorAttributes(final Function<AuthorModel, T> accessor, final BiConsumer<AuthorModel, T> mutator, final Class<T> attributeType) {
        this.accessor = accessor;
        @SuppressWarnings("unchecked") final BiConsumer<AuthorModel, Object> typedMutator = (BiConsumer<AuthorModel, Object>) mutator;
        this.mutator = typedMutator;
        this.attributeType = attributeType;
    }

    public Object getAttribute(@NonNull final AuthorModel model) {
        return accessor.apply(model);
    }

    @Override
    public String getAttributeName() {
        return name();
    }

    @Override
    public Class<Object> getAttributeType() {
        @SuppressWarnings("unchecked") final Class<Object> typed = (Class<Object>) attributeType;
        return typed;
    }

    @Override
    public Class<AuthorModel> getModelType() {
        return AuthorModel.class;
    }

    public void setAttribute(@NonNull final AuthorModel model, final Object value) {
        if (mutator == null) {
            throw new NullPointerException("AuthorModel cannot set " + name());
        }
        if ((value != null) && !attributeType.isInstance(value)) {
            throw new IllegalArgumentException(String.format("Expected %s but found %s", attributeType.getSimpleName(), value.getClass().getSimpleName()));
        }
        mutator.accept(model, value);
    }
}
