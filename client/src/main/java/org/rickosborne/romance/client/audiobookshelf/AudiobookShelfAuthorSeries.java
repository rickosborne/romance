package org.rickosborne.romance.client.audiobookshelf;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AudiobookShelfAuthorSeries implements IAudiobookShelfAddedAt, IAudiobookShelfUpdatedAt {
    Long addedAt;
    UUID id;
    List<AudiobookShelfLibraryItemMinified> items;
    String name;
    Long updatedAt;
}
