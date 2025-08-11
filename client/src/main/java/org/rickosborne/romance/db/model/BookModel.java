package org.rickosborne.romance.db.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rickosborne.romance.db.Importable;
import org.rickosborne.romance.util.BookRating;
import org.rickosborne.romance.util.BookStuff;
import org.rickosborne.romance.util.DoubleSerializer;
import org.rickosborne.romance.util.StringStuff;
import org.rickosborne.romance.util.UrlRank;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.rickosborne.romance.util.BookStuff.cleanAuthor;
import static org.rickosborne.romance.util.BookStuff.cleanTitle;
import static org.rickosborne.romance.util.MathStuff.twoPlaces;
import static org.rickosborne.romance.util.StringStuff.normalizeNames;
import static org.rickosborne.romance.util.StringStuff.splitNames;
import static org.rickosborne.romance.util.StringStuff.unescape;

@Data
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties({"stars"})
public class BookModel {
    public static BookModel build() {
        return BookModel.builder().build();
    }

    public static String fileNameForBook(final BookModel book) {
        final StringBuilder sb = new StringBuilder();
        sb.append(book.getAuthorName());
        final LocalDate publish = book.getDatePublish();
        if (publish == null) {
            sb.append(" () ");
        } else {
            sb.append(" (")
                .append(publish.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .append(") ");
        }
        sb.append(BookStuff.cleanTitle(book.getTitle()));
        return sb.toString();
    }

    public static String hashKeyForBook(final BookModel book) {
        return Optional.ofNullable(cleanAuthor(book.getAuthorName())).map(String::toLowerCase).orElse("") + "\t" +
            Optional.ofNullable(cleanTitle(book.getTitle())).map(String::toLowerCase).orElse("");
    }

    public static Stream<String> hashKeysForBook(final BookModel book) {
        if (book == null) {
            return Stream.empty();
        }
        final String title = Optional.ofNullable(cleanTitle(book.getTitle())).map(String::toLowerCase).orElse("");
        return book.streamAuthors().map(author -> author.concat("\t").concat(title));
    }

    private Integer audiobookStoreRatings;
    private String audiobookStoreSku;
    private URL audiobookStoreUrl;
    private URL audiobooksDotComUrl;
    private String authorGoodreadsUrl;
    private String authorName;
    private String breakup;
    private LocalDate datePublish;
    private LocalDate datePurchase;
    private LocalDate dateRead;
    public Boolean dnf;
    @JsonSerialize(using = DoubleSerializer.class)
    private Double durationHours;
    private String feelBad;
    private String feelGood;
    private String feelOther;
    private String genre;
    private URL goodreadsUrl;
    private String hea;
    private URL imageUrl;
    private String isbn;
    private String like;
    private String location;
    private URL mastodonUrl;
    private final MainChar mc1 = new MainChar();
    private final MainChar mc2 = new MainChar();
    private String narratorName;
    private String neurodiversity;
    private Integer pages;
    private String pairing;
    private String pov;
    private String publisherDescription;
    private String publisherName;
    @JsonPropertyOrder(alphabetic = true)
    private final Map<BookRating, Double> ratings = new TreeMap<>();
    private Boolean reading;
    private URL rickReviewUrl;
    private String seriesName;
    private String seriesPart;
    private String sexExplicitness;
    private String sexScenes;
    private String sexVariety;
    private String source;
    private String speed;
    private URL storygraphUrl;
    private String synopsis;
    @JsonPropertyOrder(alphabetic = true)
    private final Set<String> tags = new TreeSet<>();
    private String title;
    private String warnings;

    @JsonIgnore
    public String getDurationText() {
        if (durationHours == null || durationHours == 0) {
            return null;
        }
        final Duration duration = Duration.ofMinutes(Math.round(durationHours * 60d));
        final int minutes = duration.toMinutesPart();
        return duration.toHoursPart() + ":" + (minutes < 10 ? "0" : "") + minutes;
    }

    public String getPublisherDescription() {
        return unescape(publisherDescription);
    }

    @SuppressWarnings("unused")  // Jackson
    public String getStars() {
        return StringStuff.starsFromNumber(ratings.get(BookRating.Overall));
    }

    @Override
    public int hashCode() {
        return hashKeyForBook(this).hashCode();
    }

    public void setAuthorName(final String name) {
        if (name == null) {
            return;
        }
        if (authorName != null && authorName.length() > name.length()) {
            return;
        }
        authorName = normalizeNames(name);
    }

    public void setDurationHours(final Double value) {
        if (value != null) {
            this.durationHours = twoPlaces(value);
        }
    }

    public void setImageUrl(final URL url) {
        imageUrl = UrlRank.choose(imageUrl, url);
    }

    public void setNarratorName(final String name) {
        if (name == null || name.contains("Narrator Info Added Soon")) {
            return;
        }
        if (narratorName != null && narratorName.length() > name.length()) {
            return;
        }
        narratorName = normalizeNames(name);
    }

    public void setPublisherDescription(final String text) {
        if (text == null) {
            return;
        }
        publisherDescription = unescape(text.replaceAll("^.+? audiobook, by .+?\\.\\.\\.\\s+", ""));
    }

    public void setSeriesName(final String updated) {
        if (updated == null || seriesName != null) {
            return;
        }
        seriesName = updated.replaceAll("(?i)^(a|an|the)\\s+", "")
            .replaceAll("(?i)\\s+series$", "");
    }

    @JsonIgnore
    public Stream<String> streamAuthors() {
        if (authorName == null) {
            return Stream.empty();
        }
        return splitNames(authorName).map(BookStuff::cleanAuthor);
    }

    public String toString() {
        if (title == null && authorName == null) {
            return "<no book>";
        }
        return (authorName == null ? "<no author>" : authorName)
            + " "
            + (datePublish == null ? "(?)" : "(" + datePublish.getYear() + ")")
            + " "
            + (title == null ? "<no title>" : title);
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MainChar implements Importable<MainChar> {
        private String age;
        private String attachment;
        private String gender;
        private String name;
        private String profession;
        private String pronouns;

        public void clear() {
            age = null;
            attachment = null;
            gender = null;
            name = null;
            profession = null;
            pronouns = null;
        }

        @Override
        public void importFrom(final MainChar other) {
            if (other == null) {
                return;
            }
            Importable.setIf(other.age, this::setAge);
            Importable.setIf(other.attachment, this::setAttachment);
            Importable.setIf(other.gender, this::setGender);
            Importable.setIf(other.name, this::setName);
            Importable.setIf(other.profession, this::setProfession);
            Importable.setIf(other.pronouns, this::setPronouns);
        }

        @Override
        public void importFromIfNotNull(final MainChar other) {
            if (other == null) {
                return;
            }
            Importable.setIfNotNull(age, other.age, this::setAge);
            Importable.setIfNotNull(attachment, other.attachment, this::setAttachment);
            Importable.setIfNotNull(gender, other.gender, this::setGender);
            Importable.setIfNotNull(name, other.name, this::setName);
            Importable.setIfNotNull(profession, other.profession, this::setProfession);
            Importable.setIfNotNull(pronouns, other.pronouns, this::setPronouns);
        }

        public String toString() {
            return (name == null ? "<no name>" : name)
                + (pronouns == null ? "" : " (" + pronouns + ")")
                + (age == null ? "" : " " + age)
                + (gender == null ? "" : " " + gender)
                + (attachment == null ? "" : " " + attachment)
                + (profession == null ? "" : " " + profession);
        }
    }
}
