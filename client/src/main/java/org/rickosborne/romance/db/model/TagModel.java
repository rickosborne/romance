package org.rickosborne.romance.db.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import org.rickosborne.romance.util.BookRating;
import org.rickosborne.romance.util.StringStuff;

import java.util.Map;
import java.util.TreeMap;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties({"stars"})
public class TagModel {
    private String description;
    private String name;
    private Integer ownedCount;
    private Double positiveDurationHours;
    private Double positiveLikelihood;
    private Double positiveRate;
    private Integer ratedCount;
    private Double ratedDurationHours;
    @JsonPropertyOrder(alphabetic = true)
    private final Map<BookRating, Double> ratings = new TreeMap<>();


    public String getStars() {
        return StringStuff.starsFromNumber(ratings.get(BookRating.Overall));
    }
}
