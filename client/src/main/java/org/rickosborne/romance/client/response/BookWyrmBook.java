package org.rickosborne.romance.client.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.List;

import static org.rickosborne.romance.client.bookwyrm.BookWyrm.bookDbIdFromUrl;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookWyrmBook {
    public static final String ACTIVITY_STREAMS_NS = "https://www.w3.org/ns/activitystreams";

    List<URL> authors;
    Cover cover;
    String description;
    Integer editionRank;
    List<FileLink> fileLinks;
    ZonedDateTime firstPublishedDate;
    URL id;
    String isbn10;
    String isbn13;
    List<String> languages;
    URL lastEditedBy;
    String openlibraryKey;
    Integer pages;
    String physicalFormat;
    String physicalFormatDetail;
    ZonedDateTime publishedDate;
    List<String> publishers;
    String series;
    String seriesNumber;
    List<String> subjectPlaces;
    List<String> subjects;
    String subtitle;
    String title;
    String type;
    URL work;

    @JsonProperty("@context")
    public String getContext() {
        return ACTIVITY_STREAMS_NS;
    }

    @JsonIgnore
    public Integer getDbId() {
        return bookDbIdFromUrl(id);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Cover {
        String name;
        String type;
        URL url;

        @JsonProperty("@context")
        public String getContext() {
            return ACTIVITY_STREAMS_NS;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class FileLink {
        URL attributedTo;
        String availability;
        URL href;
        String mediaType;
    }
}
