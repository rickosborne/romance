package org.rickosborne.audiobookstore.client.cli;

import lombok.Builder;
import org.rickosborne.audiobookstore.AudiobookStore;
import org.rickosborne.audiobookstore.client.response.AudiobookStoreSuggestion;
import org.rickosborne.audiobookstore.client.response.BookInformation;
import org.rickosborne.audiobookstore.client.response.GoodreadsAuthor;
import org.rickosborne.audiobookstore.client.response.GoodreadsAutoComplete;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Builder(toBuilder = true)
public class DocTabbed {
    public static <T> T coalesce(final T a, final T b) {
        return a == null ? b : a;
    }

    public static String dateOnly(final Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static String emptyIfNull(final Double d) {
        return Optional.ofNullable(d).map(h -> Double.toString(Math.round(h * 100) / 100.0d)).orElse("");
    }

    public static String emptyIfNull(final URL url) {
        return Optional.ofNullable(url).map(URL::toString).orElse("");
    }

    public static String emptyIfNull(final String s) {
        return s == null ? "" : s;
    }

    public static DocTabbed fromAudiobookStoreSuggestion(final AudiobookStoreSuggestion suggestion) {
        return DocTabbed.builder()
            .title(suggestion.getTitle())
            .absUrl(Optional.ofNullable(suggestion.getUrlPath()).map(p -> {
                try {
                    return new URL(AudiobookStore.SUGGEST_BASE + "/audiobooks/" + p + ".aspx");
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }).orElse(null))
            .build();
    }

    public static DocTabbed fromBookInformation(final BookInformation book) {
        return DocTabbed.builder()
            .title(book.getCleanTitle())
            .author(book.getAuthors())
            .narrator(book.getNarrators())
            .hours(Optional.ofNullable(book.getRuntime()).map(DocTabbed::hoursFromSeconds).orElse(null))
            .purchased(book.getPurchaseInstant())
            .build();
    }

    public static DocTabbed fromGoodreadsAutoComplete(final GoodreadsAutoComplete ac) {
        return DocTabbed.builder()
            .title(ac.getTitle())
            .author(Optional.ofNullable(ac.getAuthor()).map(GoodreadsAuthor::getName).orElse(null))
            .pages(ac.getNumPages())
            .grUrl(ac.getFullBookUrl())
            .build();
    }

    public static Double hoursFromSeconds(final Double seconds) {
        if (seconds == null) {
            return null;
        }
        return ((seconds / 60d) / 60d);
    }

    private final URL absUrl;
    private final String author;
    private final URL grUrl;
    private final Double hours;
    private final String narrator;
    private final Integer pages;
    private final Instant published;
    private final String publisher;
    private final Instant purchased;
    private final String read;
    private final String title;

    public DocTabbed merge(final DocTabbed other) {
        if (other == null) {
            return this;
        }
        return DocTabbed.builder()
            .absUrl(coalesce(absUrl, other.absUrl))
            .author(coalesce(author, other.author))
            .grUrl(coalesce(grUrl, other.grUrl))
            .hours(coalesce(hours, other.hours))
            .narrator(coalesce(narrator, other.narrator))
            .pages(coalesce(pages, other.pages))
            .published(coalesce(published, other.published))
            .publisher(coalesce(publisher, other.publisher))
            .purchased(coalesce(purchased, other.purchased))
            .read(coalesce(read, other.read))
            .title(coalesce(title, other.title))
            .build();
    }

    @Override
    public String toString() {
        final List<String> columns = new LinkedList<>();
        columns.add(emptyIfNull(title));
        columns.add(emptyIfNull(author));
        columns.add(Optional.ofNullable(pages).map(String::valueOf).orElse("")); // pages
        columns.add(emptyIfNull(publisher));
        columns.add(emptyIfNull(narrator));
        columns.add(emptyIfNull(hours));
        columns.add(emptyIfNull(dateOnly(published)));
        columns.add(emptyIfNull(dateOnly(purchased)));
        columns.add(emptyIfNull(read));
        columns.add(emptyIfNull(grUrl));
        columns.add(emptyIfNull(absUrl));
        return String.join("\t", columns);
    }
}
