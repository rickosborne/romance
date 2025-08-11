package org.rickosborne.romance.db.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Data;
import org.rickosborne.romance.util.BookRating;
import org.rickosborne.romance.util.DoubleSerializer;
import org.rickosborne.romance.util.YesNoUnknown;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.rickosborne.romance.util.MathStuff.fourPlaces;

@Data
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AuthorModel {
    public static final Pattern ID_PATTERN = Pattern.compile(".*?/author/show/(\\d+)[.].*$");

    public static AuthorModel build() {
        return AuthorModel.builder().build();
    }

    private URL audiobookStoreUrl;
    private String bioHtml;
    private Integer dnfCount;
    private Integer fiveStarCount;
    private Integer fourStarPlusCount;
    private String goodreadsId;
    private URL goodreadsUrl;
    private String mastodonHandle;
    @JsonSerialize(using = DoubleSerializer.class)
    private Double maxRating;
    @JsonSerialize(using = DoubleSerializer.class)
    private Double meanDurationHours;
    @JsonSerialize(using = DoubleSerializer.class)
    private Double meanPages;
    @JsonSerialize(using = DoubleSerializer.class)
    private Double minRating;
    private String name;
    @JsonSerialize(using = DoubleSerializer.class)
    private Double odds4;
    private Integer ownedCount;
    private URL picUrl;
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

    public void setBioHtml(final String text) {
        this.bioHtml = text == null || text.isBlank() ? null : text;
    }

    @JsonIgnore
    public void setGoodreadsUrlFromString(final String url) {
        try {
            setGoodreadsUrl(url == null ? null : new URL(url));
            if (url != null) {
                final Matcher matcher = ID_PATTERN.matcher(url);
                if (matcher.find()) {
                    final String id = matcher.group(1);
                    if (id != null) {
                        setGoodreadsId(id);
                    }
                }
            }
        } catch (MalformedURLException e) {
            // do nothing
        }
    }

    public void setMaxRating(final Double value) {
        if (value != null) {
            this.maxRating = fourPlaces(value);
        }
    }

    public void setMeanDurationHours(final Double value) {
        if (value != null) {
            this.meanDurationHours = fourPlaces(value);
        }
    }

    public void setMeanPages(final Double value) {
        if (value != null) {
            this.meanPages = fourPlaces(value);
        }
    }

    public void setMinRating(final Double value) {
        if (value != null) {
            this.minRating = fourPlaces(value);
        }
    }

    public void setOdds4(final Double value) {
        if (value != null) {
            this.odds4 = fourPlaces(value);
        }
    }

    @Override
    public String toString() {
        return (name == null ? "<no name>" : name)
            + (pronouns == null ? "" : " (" + pronouns + ")")
            + (audiobookStoreUrl == null ? "" : " TABS")
            + (storyGraphUrl == null ? "" : " SG")
            + (goodreadsUrl == null ? "" : " GR");
    }
}
