package org.rickosborne.romance.db.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import org.rickosborne.romance.util.BookRating;
import org.rickosborne.romance.util.DoubleSerializer;
import org.rickosborne.romance.util.StringStuff;

import java.util.Map;
import java.util.TreeMap;

import static org.rickosborne.romance.util.MathStuff.fourPlaces;
import static org.rickosborne.romance.util.MathStuff.twoPlaces;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties({"stars"})
public class TagModel {
    private String description;
    @JsonSerialize(using = DoubleSerializer.class)
    private Double effectiveLike;
    private Integer fiveStarCount;
    private String name;
    private Integer ownedCount;
    @JsonSerialize(using = DoubleSerializer.class)
    private Double positiveDurationHours;
    @JsonSerialize(using = DoubleSerializer.class)
    private Double positiveLikelihood;
    @JsonSerialize(using = DoubleSerializer.class)
    private Double positiveRate;
    private Integer ratedCount;
    @JsonSerialize(using = DoubleSerializer.class)
    private Double ratedDurationHours;
    @JsonPropertyOrder(alphabetic = true)
    private final Map<BookRating, Double> ratings = new TreeMap<>();

    public String getStars() {
        return StringStuff.starsFromNumber(ratings.get(BookRating.Overall));
    }

    public void setEffectiveLike(final Double value) {
        if (value != null) {
            this.effectiveLike = fourPlaces(value);
        }
    }

    public void setPositiveDurationHours(final Double value) {
        if (value != null) {
            this.positiveDurationHours = twoPlaces(value);
        }
    }

    public void setPositiveLikelihood(final Double value) {
        if (value != null) {
            this.positiveLikelihood = fourPlaces(value);
        }
    }

    public void setPositiveRate(final Double value) {
        if (value != null) {
            this.positiveRate = fourPlaces(value);
        }
    }

    public void setRatedDurationHours(final Double value) {
        if (value != null) {
            this.ratedDurationHours = twoPlaces(value);
        }
    }
}
