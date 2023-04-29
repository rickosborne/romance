package org.rickosborne.romance.db.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import org.rickosborne.romance.util.DoubleSerializer;
import org.rickosborne.romance.util.NarratorRating;
import org.rickosborne.romance.util.StringStuff;

import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.rickosborne.romance.util.MathStuff.twoPlaces;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties({"meanPagesPerHour"})
public class NarratorModel {
    private String accent;
    private URL goodreadsUrl;
    private String name;
    @JsonPropertyOrder(alphabetic = true)
    private final Set<String> negatives = new TreeSet<>();
    private Integer ownedCount;
    @JsonPropertyOrder(alphabetic = true)
    private final Map<NarratorRating, Double> ratings = new TreeMap<>();
    private URL siteUrl;
    @JsonSerialize(using = DoubleSerializer.class)
    private Double totalDurationHours;
    private Integer totalPages;
    private URL twitterUrl;

    public Double getMeanPagesPerHour() {
        return totalDurationHours != null
            && totalDurationHours > 0
            && totalPages != null
            && totalPages > 0
            ? totalPages / totalDurationHours : null;
    }

    public String getStars() {
        return StringStuff.starsFromNumber(ratings.get(NarratorRating.Overall));
    }

    public void setTotalDurationHours(final Double value) {
        if (value != null) {
            this.totalDurationHours = twoPlaces(value);
        }
    }
}
