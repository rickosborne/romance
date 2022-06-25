package org.rickosborne.romance.db.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import org.rickosborne.romance.util.BookRating;
import org.rickosborne.romance.util.StringStuff;

import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties({"stars"})
public class SeriesModel {
    private URL goodreadsUrl;
    private String name;
    private Integer ownedCount;
    @JsonPropertyOrder(alphabetic = true)
    private final Map<BookRating, Double> ratings = new TreeMap<>();

    public String getStars() {
        return StringStuff.starsFromNumber(ratings.get(BookRating.Overall));
    }
}
