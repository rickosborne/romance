package org.rickosborne.romance.client.audiobookshelf;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
public class AudiobookShelfBookMetadataBase {
    String asin;
    List<Author> authors;
    String description;
    String descriptionPlain;
    Boolean explicit;
    List<String> genres;
    String isbn;
    String language;
    List<String> narrators;
    String publishedDate;
    String publishedYear;
    String publisher;
    List<AudiobookShelfSeriesSequence> series;
    String subtitle;
    String title;

    protected void copyTo(final AudiobookShelfBookMetadataBase other) {
        other.asin = asin;
        other.description = description;
        other.explicit = explicit;
        other.genres = genres == null ? null : new ArrayList<>(genres);
        other.isbn = isbn;
        other.language = language;
        other.narrators = narrators == null ? null : new ArrayList<>(narrators);
        other.publishedDate = publishedDate;
        other.publishedYear = publishedYear;
        if (series == null) {
            other.series = null;
        } else {
            other.series = series.stream().map(AudiobookShelfSeriesSequence::copy).collect(Collectors.toList());
        }
        other.subtitle = subtitle;
        other.title = title;
    }

    @Data
    public static class Author {
        UUID id;
        String name;
    }
}
