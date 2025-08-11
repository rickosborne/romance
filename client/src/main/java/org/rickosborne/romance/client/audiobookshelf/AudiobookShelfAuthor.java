package org.rickosborne.romance.client.audiobookshelf;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
public class AudiobookShelfAuthor extends AudiobookShelfAuthorMinified implements IAudiobookShelfAddedAt, IAudiobookShelfUpdatedAt {
    Long addedAt;
    String asin;
    String description;
    String imagePath;
    String lastFirst;
    String libraryId;
    List<AudiobookShelfLibraryItemMinified> libraryItems;
    List<AudiobookShelfAuthorSeries> series;
    Long updatedAt;
}
