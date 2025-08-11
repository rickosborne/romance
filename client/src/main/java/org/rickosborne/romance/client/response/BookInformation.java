package org.rickosborne.romance.client.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.rickosborne.romance.util.BookStuff;

import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.rickosborne.romance.util.BookStuff.cleanAuthor;
import static org.rickosborne.romance.util.BookStuff.cleanTitle;
import static org.rickosborne.romance.util.StringStuff.splitNames;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class BookInformation {
    public static String hashKeyForBookInformation(final BookInformation info) {
        if (info == null) {
            return null;
        }
        final String title = cleanTitle(info.getTitle());
        final String author = cleanAuthor(info.getAuthors());
        if (title == null || author == null) {
            return null;
        }
        return author.toLowerCase().concat("\t").concat(title.toLowerCase());
    }

    public static Stream<String> hashKeysForBookInformation(final BookInformation info) {
        if (info == null) {
            return Stream.empty();
        }
        final String title = Optional.ofNullable(cleanTitle(info.getTitle())).map(String::toLowerCase).orElse("");
        return info.streamAuthors()
            .map(author -> author.toLowerCase().concat("\t").concat(title));
    }

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

    @JsonIgnore
    public Stream<String> streamAuthors() {
        if (authors == null) {
            return Stream.empty();
        }
        return splitNames(authors).map(BookStuff::cleanAuthor);
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
