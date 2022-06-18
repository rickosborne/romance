package org.rickosborne.romance.client.cli;

import org.rickosborne.romance.client.client.response.BookInformation;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class BookMerger {
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

    public static BookInformation merge(final BookInformation... books) {
        return BookInformation.builder()
            .audioFiles(coalesceList(BookInformation::getAudioFiles, books))
            .authors(coalesce(BookInformation::getAuthors, books))
            .bonusMaterialFiles(coalesceList(BookInformation::getBonusMaterialFiles, books))
            .bookDescription(coalesce(BookInformation::getBookDescription, books))
            .completed(coalesce(BookInformation::getCompleted, books))
            .completionDateTime(coalesce(BookInformation::getCompletionDateTime, BookMerger::filterNonDate, books))
            .imageUrl(coalesce(BookInformation::getImageUrl, books))
            .narrators(coalesce(BookInformation::getNarrators, books))
            .playbackPosition(coalesce(BookInformation::getPlaybackPosition, books))
            .purchaseDate(coalesce(BookInformation::getPurchaseDate, books))
            .ratings(coalesce(BookInformation::getRatings, books))
            .refunded(coalesce(BookInformation::getRefunded, books))
            .sku(coalesce(BookInformation::getSku, books))
            .title(coalesce(BookInformation::getTitle, books))
            .totalSize(coalesce(BookInformation::getTotalSize, books))
            .url(coalesce(BookInformation::getUrl, BookMerger::nonEmpty, books))
            .build();
    }

    public static boolean filterNonDate(final String date) {
        return date != null && !("0001-01-01T00:00:00".equals(date));
    }

    public static boolean nonEmpty(final String value) {
        return value != null && !value.isBlank();
    }
}
