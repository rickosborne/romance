package org.rickosborne.romance.client.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import org.rickosborne.romance.util.BookStuff;

import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder(toBuilder = true)
@AllArgsConstructor(onConstructor = @__(@JsonCreator))
public class BookInformation {
    @JsonProperty("AudioFiles")
    @ToString.Exclude
    List<BookFile> audioFiles;
    @JsonProperty("Authors")
    String authors;
    @JsonProperty("BonusMaterialFiles")
    @ToString.Exclude
    List<BookFile> bonusMaterialFiles;
    @ToString.Exclude
    @JsonProperty("BookDescription")
    String bookDescription;
    @JsonProperty("Completed")
    Boolean completed;
    @JsonProperty("CompletionDateTime")
    String completionDateTime;
    @JsonIgnore
    @Getter(lazy = true)
    private final Instant completionInstant = Optional.ofNullable(completionDateTime)
        .map(pd -> Instant.parse(pd + "Z"))
        .filter(pd -> pd.isAfter(Instant.ofEpochSecond(1)))
        .orElse(null);
    @JsonProperty("ImageUrl")
    URL imageUrl;
    @JsonProperty("Narrators")
    String narrators;
    @JsonProperty("PlayBackPosition")
    Integer playbackPosition;
    @JsonProperty("PurchaseDate")
    String purchaseDate;
    @JsonIgnore
    @Getter(lazy = true)
    private final Instant purchaseInstant = Optional.ofNullable(purchaseDate)
        .map(pd -> Instant.parse(pd + "Z"))
        .filter(pd -> pd.isAfter(Instant.ofEpochSecond(1)))
        .orElse(null);
    @JsonProperty("Ratings")
    Integer ratings;
    @JsonProperty("IsRefunded")
    Boolean refunded;
    @JsonProperty("Runtime")
    Double runtime; // seconds
    @JsonProperty("SKU")
    String sku;
    @JsonProperty("Title")
    String title;
    @JsonProperty("TotalSize")
    Long totalSize;
    @JsonProperty("Url")
    URL url;

    @JsonIgnore
    public String getCleanTitle() {
        return BookStuff.cleanTitle(title);
    }

    @JsonIgnore
    public Double getRuntimeHours() {
        if (runtime == null || runtime == 0d) {
            return null;
        }
        return Math.round(runtime / 36d) / 100d;
    }

    @Override
    public String toString() {
        if (title == null && authors == null) {
            return "<no book>";
        }
        return (title == null ? "<no title>" : title)
            + " by "
            + (authors == null ? "<no author>" : authors);
    }
}
