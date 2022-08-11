package org.rickosborne.romance.db.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;
import org.rickosborne.romance.util.BookRating;
import org.rickosborne.romance.util.YesNoUnknown;

import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

@Data
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AuthorModel {
    public static AuthorModel build() {
        return AuthorModel.builder().build();
    }

    private URL audiobookStoreUrl;
    private Integer dnfCount;
    private Integer fiveStarCount;
    private Integer fourStarPlusCount;
    private URL goodreadsUrl;
    private Double maxRating;
    private Double meanDurationHours;
    private Double meanPages;
    private Double minRating;
    private String name;
    private Double odds4;
    private Integer ownedCount;
    private String pronouns;
    private YesNoUnknown queer;
    private Integer ratedCount;
    @JsonPropertyOrder(alphabetic = true)
    private final Map<BookRating, Double> ratings = new TreeMap<>();
    private String rep;
    private URL siteUrl;
    private String stars;
    private URL storyGraphUrl;
    private String twitterName;
    private URL twitterUrl;
}
