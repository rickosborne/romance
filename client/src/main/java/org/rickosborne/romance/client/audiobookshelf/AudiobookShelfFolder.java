package org.rickosborne.romance.client.audiobookshelf;

import lombok.Data;

import java.util.UUID;

@Data
public class AudiobookShelfFolder implements IAudiobookShelfAddedAt {
    Long addedAt;
    String fullPath;
    UUID id;
    UUID libraryId;
}
