package org.rickosborne.romance.db.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import org.rickosborne.romance.util.BookRating;
import org.rickosborne.romance.util.StringStuff;

import java.net.URL;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class BookModel {
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

    public String getStars() {
        return StringStuff.starsFromNumber(ratings.get(BookRating.Overall));
    }

//    @JsonProperty(value = "tags")
//    protected Collection<String> getTagsSorted() {
//        return tags.stream().sorted().collect(Collectors.toList());
//    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class MainChar {
        private String age;
        private String gender;
        private String name;
        private String profession;
        private String pronouns;
    }
}
