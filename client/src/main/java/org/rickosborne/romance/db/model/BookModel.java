package org.rickosborne.romance.db.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;
import org.rickosborne.romance.db.Importable;
import org.rickosborne.romance.util.BookRating;
import org.rickosborne.romance.util.StringStuff;

import java.net.URL;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

@Data
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties({"stars"})
public class BookModel {
    public static BookModel build() {
        return BookModel.builder().build();
    }

    private URL audiobookStoreUrl;
    private String authorName;
    private LocalDate datePublish;
    private LocalDate datePurchase;
    private LocalDate dateRead;
    public Boolean dnf;
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
    private final MainChar mc1 = new MainChar();
    private final MainChar mc2 = new MainChar();
    private String narratorName;
    private String neurodiversity;
    private Integer pages;
    private String pairing;
    private String pov;
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
    private String synopsis;
    @JsonPropertyOrder(alphabetic = true)
    private final Set<String> tags = new TreeSet<>();
    private String title;
    private String warnings;

    @SuppressWarnings("unused")  // Jackson
    public String getStars() {
        return StringStuff.starsFromNumber(ratings.get(BookRating.Overall));
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class MainChar implements Importable<MainChar> {
        private String age;
        private String attachment;
        private String gender;
        private String name;
        private String profession;
        private String pronouns;

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
    }
}
