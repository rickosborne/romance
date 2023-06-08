package org.rickosborne.romance.client.command;

import lombok.Builder;
import org.rickosborne.romance.client.response.AudiobookStoreSuggestion;
import org.rickosborne.romance.client.response.BookInformation;
import org.rickosborne.romance.client.response.GoodreadsAuthor;
import org.rickosborne.romance.client.response.GoodreadsAutoComplete;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.util.UrlRank;

import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.rickosborne.romance.util.DateStuff.instantFromLocal;

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

    public static String emptyIfNull(final Integer d) {
        return Optional.ofNullable(d).map(String::valueOf).orElse("");
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
            .absUrl(suggestion.getUrl())
            .build();
    }

    public static DocTabbed fromBookInformation(final BookInformation book) {
        return DocTabbed.builder()
            .title(book.getCleanTitle())
            .author(book.getAuthors())
            .narrator(book.getNarrators())
            .hours(Optional.ofNullable(book.getRuntime()).map(DocTabbed::hoursFromSeconds).orElse(null))
            .purchased(book.getPurchaseInstant())
            .imageUrl(book.getImageUrl())
            .build();
    }

    public static DocTabbed fromBookModel(final BookModel book) {
        return DocTabbed.builder()
            .title(book.getTitle())
            .author(book.getAuthorName())
            .pages(book.getPages())
            .publisher(book.getPublisherName())
            .narrator(book.getNarratorName())
            .hours(book.getDurationHours())
            .published(instantFromLocal(book.getDatePublish()))
            .purchased(instantFromLocal(book.getDatePurchase()))
            .read(Boolean.TRUE.equals(book.getDnf()) ? "DNF" : Optional.ofNullable(book.getDateRead()).map(d -> d.format(DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null))
            .grUrl(book.getGoodreadsUrl())
            .absUrl(book.getAudiobookStoreUrl())
            .rickReview(book.getRickReviewUrl())
            .imageUrl(book.getImageUrl())
            .isbn(book.getIsbn())
            .sgUrl(book.getStorygraphUrl())
            .mc1Name(book.getMc1().getName())
            .mc1Gender(book.getMc1().getGender())
            .mc1Pronouns(book.getMc1().getPronouns())
            .mc1Age(book.getMc1().getAge())
            .mc1Profession(book.getMc1().getProfession())
            .mc1Attachment(book.getMc1().getAttachment())
            .mc2Name(book.getMc2().getName())
            .mc2Gender(book.getMc2().getGender())
            .mc2Pronouns(book.getMc2().getPronouns())
            .mc2Age(book.getMc2().getAge())
            .mc2Profession(book.getMc2().getProfession())
            .mc2Attachment(book.getMc2().getAttachment())
            .sexScenes(book.getSexScenes())
            .sexVariety(book.getSexVariety())
            .sexExplicitness(book.getSexExplicitness())
            .seriesName(book.getSeriesName())
            .seriesPart(book.getSeriesPart())
            .location(book.getLocation())
            .build();
    }

    public static DocTabbed fromGoodreadsAutoComplete(final GoodreadsAutoComplete ac) {
        return DocTabbed.builder()
            .title(ac.getTitle())
            .author(Optional.ofNullable(ac.getAuthor()).map(GoodreadsAuthor::getName).orElse(null))
            .pages(ac.getNumPages())
            .grUrl(ac.getFullBookUrl())
            .imageUrl(ac.getImageUrl())
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
    private final URL imageUrl;
    private final String isbn;
    private final String location;
    private final String mc1Age;
    private final String mc1Attachment;
    private final String mc1Gender;
    private final String mc1Name;
    private final String mc1Profession;
    private final String mc1Pronouns;
    private final String mc2Age;
    private final String mc2Attachment;
    private final String mc2Gender;
    private final String mc2Name;
    private final String mc2Profession;
    private final String mc2Pronouns;
    private final String narrator;
    private final Integer pages;
    private final Instant published;
    private final String publisher;
    private final Instant purchased;
    private final String read;
    private final URL rickReview;
    private final String seriesName;
    private final String seriesPart;
    private final String sexExplicitness;
    private final String sexScenes;
    private final String sexVariety;
    private final URL sgUrl;
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
            .imageUrl(UrlRank.choose(imageUrl, other.imageUrl))
            .isbn(coalesce(isbn, other.isbn))
            .location(coalesce(location, other.location))
            .mc1Age(coalesce(mc1Age, other.mc1Age))
            .mc1Attachment(coalesce(mc1Attachment, other.mc1Attachment))
            .mc1Gender(coalesce(mc1Gender, other.mc1Gender))
            .mc1Name(coalesce(mc1Name, other.mc1Name))
            .mc1Profession(coalesce(mc1Profession, other.mc1Profession))
            .mc1Pronouns(coalesce(mc1Pronouns, other.mc1Pronouns))
            .mc2Age(coalesce(mc2Age, other.mc2Age))
            .mc2Attachment(coalesce(mc2Attachment, other.mc2Attachment))
            .mc2Gender(coalesce(mc2Gender, other.mc2Gender))
            .mc2Name(coalesce(mc2Name, other.mc2Name))
            .mc2Profession(coalesce(mc2Profession, other.mc2Profession))
            .mc2Pronouns(coalesce(mc2Pronouns, other.mc2Pronouns))
            .narrator(coalesce(narrator, other.narrator))
            .pages(coalesce(pages, other.pages))
            .published(coalesce(published, other.published))
            .publisher(coalesce(publisher, other.publisher))
            .purchased(coalesce(purchased, other.purchased))
            .read(coalesce(read, other.read))
            .rickReview(coalesce(rickReview, other.rickReview))
            .seriesName(coalesce(seriesName, other.seriesName))
            .seriesPart(coalesce(seriesPart, other.seriesPart))
            .sexExplicitness(coalesce(sexExplicitness, other.sexExplicitness))
            .sexScenes(coalesce(sexScenes, other.sexScenes))
            .sexVariety(coalesce(sexVariety, other.sexVariety))
            .title(coalesce(title, other.title))
            .build();
    }

    @Override
    public String toString() {
        final List<String> columns = new LinkedList<>();
        for (final String attr : new String[]{
            title, author, emptyIfNull(pages), publisher, narrator, emptyIfNull(hours),
            dateOnly(published), dateOnly(purchased), read, emptyIfNull(grUrl),
            emptyIfNull(absUrl), emptyIfNull(rickReview), emptyIfNull(imageUrl), isbn,
            emptyIfNull(sgUrl),
            mc1Name, mc1Gender, mc1Pronouns, mc1Age, mc1Profession, mc1Attachment,
            mc2Name, mc2Gender, mc2Pronouns, mc2Age, mc2Profession, mc2Attachment,
            sexScenes, sexVariety, sexExplicitness,
            seriesName, seriesPart,
            location
        }) {
            columns.add(emptyIfNull(attr));
        }
        return String.join("\t", columns);
    }
}
