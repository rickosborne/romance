package org.rickosborne.romance.db.model;

import lombok.Getter;
import org.rickosborne.romance.util.YesNoUnknown;

import java.net.URL;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Getter
public enum AuthorAttributes implements EnumSchemaAttribute<AuthorModel> {
    audiobookStoreUrl(AuthorModel::getAudiobookStoreUrl, AuthorModel::setAudiobookStoreUrl, URL.class),
    dnfCount(AuthorModel::getDnfCount, AuthorModel::setDnfCount, Integer.class),
    fiveStarCount(AuthorModel::getFiveStarCount, AuthorModel::setFiveStarCount, Integer.class),
    fourStarPlusCount(AuthorModel::getFourStarPlusCount, AuthorModel::setFourStarPlusCount, Integer.class),
    goodreadsUrl(AuthorModel::getGoodreadsUrl, AuthorModel::setGoodreadsUrl, URL.class),
    mastodonHandle(AuthorModel::getMastodonHandle, AuthorModel::setMastodonHandle, String.class),
    maxRating(AuthorModel::getMaxRating, AuthorModel::setMaxRating, Double.class),
    meanDurationHours(AuthorModel::getMeanDurationHours, AuthorModel::setMeanDurationHours, Double.class),
    meanPages(AuthorModel::getMeanPages, AuthorModel::setMeanPages, Double.class),
    minRating(AuthorModel::getMinRating, AuthorModel::setMinRating, Double.class),
    name(AuthorModel::getName, AuthorModel::setName, String.class),
    odds4(AuthorModel::getOdds4, AuthorModel::setOdds4, Double.class),
    ownedCount(AuthorModel::getOwnedCount, AuthorModel::setOwnedCount, Integer.class),
    pronouns(AuthorModel::getPronouns, AuthorModel::setPronouns, String.class),
    queer(AuthorModel::getQueer, AuthorModel::setQueer, YesNoUnknown.class),
    ratedCount(AuthorModel::getRatedCount, AuthorModel::setRatedCount, Integer.class),
    ratings(AuthorModel::getRatings, null, Map.class),
    rep(AuthorModel::getRep, AuthorModel::setRep, String.class),
    siteUrl(AuthorModel::getSiteUrl, AuthorModel::setSiteUrl, URL.class),
    stars(AuthorModel::getStars, AuthorModel::setStars, String.class),
    storyGraphUrl(AuthorModel::getStoryGraphUrl, AuthorModel::setStoryGraphUrl, URL.class),
    twitterName(AuthorModel::getTwitterName, AuthorModel::setTwitterName, String.class),
    twitterUrl(AuthorModel::getTwitterUrl, AuthorModel::setTwitterUrl, URL.class),
    ;
    private final Function<AuthorModel, ?> accessor;
    private final Class<Object> attributeType;
    private final Class<AuthorModel> modelType = AuthorModel.class;
    private final BiConsumer<AuthorModel, Object> mutator;

    <T> AuthorAttributes(final Function<AuthorModel, T> accessor, final BiConsumer<AuthorModel, T> mutator, final Class<T> attributeType) {
        this.accessor = accessor;
        @SuppressWarnings("unchecked") final BiConsumer<AuthorModel, Object> typedMutator = (BiConsumer<AuthorModel, Object>) mutator;
        this.mutator = typedMutator;
        @SuppressWarnings("unchecked") final Class<Object> objectAttributeType = (Class<Object>) attributeType;
        this.attributeType = objectAttributeType;
    }
}
