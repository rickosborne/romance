package org.rickosborne.romance.util;

import org.rickosborne.romance.client.response.AudiobookStoreSuggestion;
import org.rickosborne.romance.client.response.BookInformation;
import org.rickosborne.romance.client.response.GoodreadsAuthor;
import org.rickosborne.romance.client.response.GoodreadsAutoComplete;
import org.rickosborne.romance.db.model.BookModel;

import java.util.Optional;
import java.util.function.Predicate;

import static org.rickosborne.romance.util.DateStuff.localFromInstant;

public class BookMerger extends AMerger {
    public static Predicate<BookModel> bookLikeFilter(final BookModel like) {
        return b -> StringStuff.fuzzyMatch(b.getTitle(), like.getTitle()) && StringStuff.fuzzyListMatch(b.getAuthorName(), like.getAuthorName());
    }

    public static BookInformation merge(final BookInformation... books) {
        return BookInformation.builder()
            .audioFiles(coalesceList(BookInformation::getAudioFiles, books))
            .authors(coalesceText(BookInformation::getAuthors, books))
            .bonusMaterialFiles(coalesceList(BookInformation::getBonusMaterialFiles, books))
            .bookDescription(coalesceText(BookInformation::getBookDescription, books))
            .completed(coalesce(BookInformation::getCompleted, books))
            .completionDateTime(coalesce(BookInformation::getCompletionDateTime, BookMerger::filterNonDate, books))
            .imageUrl(UrlRank.chooseUrl(BookInformation::getImageUrl, books))
            .narrators(coalesceText(BookInformation::getNarrators, books))
            .playbackPosition(coalesce(BookInformation::getPlaybackPosition, books))
            .purchaseDate(coalesce(BookInformation::getPurchaseDate, books))
            .ratings(coalesce(BookInformation::getRatings, books))
            .refunded(coalesce(BookInformation::getRefunded, books))
            .sku(coalesceText(BookInformation::getSku, books))
            .title(coalesceText(BookInformation::getTitle, books))
            .totalSize(coalesce(BookInformation::getTotalSize, books))
            .url(coalesce(BookInformation::getUrl, books))
            .build();
    }

    public static BookModel modelFromAudiobookStoreSuggestion(final AudiobookStoreSuggestion suggestion) {
        if (suggestion == null) {
            return null;
        }
        return BookModel.builder()
            .title(suggestion.getCleanTitle())
            .audiobookStoreUrl(suggestion.getUrl())
            .audiobookStoreSku(suggestion.getKeyId())
            .build();
    }

    public static BookModel modelFromBookInformation(final BookInformation info) {
        if (info == null) {
            return null;
        }
        return BookModel.builder()
            .authorName(info.getAuthors())
            .dateRead(localFromInstant(info.getCompletionInstant()))
            .imageUrl(UrlRank.fixup(info.getImageUrl()))
            .narratorName(info.getNarrators())
            .datePurchase(localFromInstant(info.getPurchaseInstant()))
            .durationHours(info.getRuntimeHours())
            .title(info.getCleanTitle())
            .audiobookStoreUrl(info.getUrl())
            .audiobookStoreSku(info.getSku())
            .audiobookStoreRatings(info.getRatings())
            .build();
    }

    public static BookModel modelFromGoodreadsAutoComplete(final GoodreadsAutoComplete ac) {
        if (ac == null) {
            return null;
        }
        return BookModel.builder()
            .authorName(Optional.ofNullable(ac.getAuthor()).map(GoodreadsAuthor::getName).orElse(null))
            .goodreadsUrl(ac.getFullBookUrl())
            .imageUrl(UrlRank.fixup(ac.getImageUrl()))
            .pages(ac.getNumPages())
            .title(ac.getCleanTitle())
            .build();
    }
}
