package org.rickosborne.romance.db.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import org.rickosborne.romance.util.BookRating;
import org.rickosborne.romance.util.YesNoUnknown;

import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AuthorModel {
    private Integer fiveStarCount;
    private Integer fourStarPlusCount;
    private URL goodreadsUrl;
    private Double maxRating;
    private Double meanDurationHours;
    private Double meanPages;
    private Double minRating;
    private String name;
    private Integer ownedCount;
    private String pronouns;
    private YesNoUnknown queer;
    private Integer ratedCount;
    @JsonPropertyOrder(alphabetic = true)
    private final Map<BookRating, Double> ratings = new TreeMap<>();
    private URL siteUrl;
    private String stars;
    private String twitterName;
    private URL twitterUrl;
}
