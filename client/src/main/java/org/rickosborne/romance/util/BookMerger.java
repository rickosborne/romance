package org.rickosborne.romance.util;

import org.rickosborne.romance.client.response.AudiobookStoreSuggestion;
import org.rickosborne.romance.client.response.BookInformation;
import org.rickosborne.romance.client.response.GoodreadsAuthor;
import org.rickosborne.romance.client.response.GoodreadsAutoComplete;
import org.rickosborne.romance.db.model.BookModel;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class BookMerger {
    public static Predicate<BookModel> bookLikeFilter(final BookModel like) {
        return b -> StringStuff.fuzzyMatch(b.getTitle(), like.getTitle()) && StringStuff.fuzzyListMatch(b.getAuthorName(), like.getAuthorName());
    }

    public static <T> T coalesce(final Function<BookInformation, T> accessor, final BookInformation... books) {
        for (final BookInformation book : books) {
            final T value = accessor.apply(book);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public static <T> T coalesce(final Function<BookInformation, T> accessor, final Predicate<T> predicate, final BookInformation... books) {
        for (final BookInformation book : books) {
            if (book != null) {
                final T value = accessor.apply(book);
                if (value != null && predicate.test(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    public static <T> List<T> coalesceList(final Function<BookInformation, List<T>> accessor, final BookInformation... books) {
        List<T> result = null;
        for (final BookInformation book : books) {
            if (book != null) {
                final List<T> list = accessor.apply(book);
                if (list != null) {
                    if (!list.isEmpty()) {
                        return list;
                    }
                    result = list;
                }
            }
        }
        return result;
    }

    public static boolean filterNonDate(final String date) {
        return date != null && !("0001-01-01T00:00:00".equals(date));
    }

    public static BookInformation merge(final BookInformation... books) {
        return BookInformation.builder()
            .audioFiles(coalesceList(BookInformation::getAudioFiles, books))
            .authors(coalesce(BookInformation::getAuthors, books))
            .bonusMaterialFiles(coalesceList(BookInformation::getBonusMaterialFiles, books))
            .bookDescription(coalesce(BookInformation::getBookDescription, books))
            .completed(coalesce(BookInformation::getCompleted, books))
            .completionDateTime(coalesce(BookInformation::getCompletionDateTime, BookMerger::filterNonDate, books))
            .imageUrl(UrlRank.chooseUrl(BookInformation::getImageUrl, books))
            .narrators(coalesce(BookInformation::getNarrators, books))
            .playbackPosition(coalesce(BookInformation::getPlaybackPosition, books))
            .purchaseDate(coalesce(BookInformation::getPurchaseDate, books))
            .ratings(coalesce(BookInformation::getRatings, books))
            .refunded(coalesce(BookInformation::getRefunded, books))
            .sku(coalesce(BookInformation::getSku, books))
            .title(coalesce(BookInformation::getTitle, books))
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
            .dateRead(Optional.ofNullable(info.getCompletionInstant()).map(i -> LocalDate.ofInstant(i, ZoneOffset.UTC)).orElse(null))
            .imageUrl(UrlRank.fixup(info.getImageUrl()))
            .narratorName(info.getNarrators())
            .datePurchase(Optional.ofNullable(info.getPurchaseInstant()).map(i -> LocalDate.ofInstant(i, ZoneOffset.UTC)).orElse(null))
            .durationHours(info.getRuntimeHours())
            .title(info.getCleanTitle())
            .audiobookStoreUrl(info.getUrl())
            .audiobookStoreSku(info.getSku())
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

    public static boolean nonEmpty(final String value) {
        return value != null && !value.isBlank();
    }
}
