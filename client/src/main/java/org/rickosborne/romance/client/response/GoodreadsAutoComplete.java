package org.rickosborne.romance.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.rickosborne.romance.Goodreads;
import org.rickosborne.romance.util.BookStuff;

import java.net.MalformedURLException;
import java.net.URL;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoodreadsAutoComplete {
    @JsonProperty("author")
    GoodreadsAuthor author;
    @JsonProperty("bookId")
    String bookId;
    @JsonProperty("bookTitleBare")
    String bookTitleBare;
    @JsonProperty("bookUrl")
    String bookUrl;
    @JsonProperty("imageUrl")
    URL imageUrl;
    @JsonProperty("numPages")
    Integer numPages;
    @JsonProperty("rank")
    Integer rank;
    @JsonProperty("title")
    String title;
    @JsonProperty("workId")
    String workId;

    public URL getFullBookUrl() {
        if (bookUrl == null || bookUrl.isBlank()) {
            return null;
        }
        try {
            return new URL(Goodreads.API_BASE + bookUrl);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public String getCleanTitle() {
        return BookStuff.cleanTitle(title);
    }
}
