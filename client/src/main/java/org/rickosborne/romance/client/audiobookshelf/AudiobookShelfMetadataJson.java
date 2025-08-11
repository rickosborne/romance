package org.rickosborne.romance.client.audiobookshelf;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.rickosborne.romance.util.StringStuff.sortIfNeeded;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AudiobookShelfMetadataJson {
    Boolean abridged;
    String asin;
    List<String> authors;
    List<Chapter> chapters;
    String description;
    List<String> genres;
    String isbn;
    String language;
    List<String> narrators;
    String publishedDate;
    String publishedYear;
    String publisher;
    List<String> series;
    String subtitle;
    List<String> tags;
    String title;

    public void setAuthors(final List<String> authors) {
        this.authors = sortIfNeeded(authors);
    }

    public void setGenres(final List<String> genres) {
        this.genres = sortIfNeeded(genres);
    }

    public void setNarrators(final List<String> narrators) {
        this.narrators = sortIfNeeded(narrators);
    }

    public void setPublishedDateFromLocal(final LocalDate date) {
        if (date == null) {
            this.publishedDate = null;
            this.publishedYear = null;
        } else {
            this.publishedYear = String.valueOf(date.getYear());
            this.publishedDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
    }

    public void setSeries(final List<String> series) {
        this.series = sortIfNeeded(series);
    }

    public void setTags(final List<String> tags) {
        this.tags = sortIfNeeded(tags);
    }

    @Data
    public static class Chapter {
        Double end;
        int id;
        Double start;
        String title;
    }
}
