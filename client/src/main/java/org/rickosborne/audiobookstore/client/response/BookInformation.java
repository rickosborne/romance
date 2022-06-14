package org.rickosborne.audiobookstore.client.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

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
    @JsonProperty("ImageUrl")
    String imageUrl;
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
    Double runtime;
    @JsonProperty("SKU")
    String sku;
    @JsonProperty("Title")
    String title;
    @JsonProperty("TotalSize")
    Long totalSize;
    @JsonProperty("Url")
    String url;

    public String getCleanTitle() {
        return title == null ? null : title.replace(" (Unabridged)", "");
    }
}
