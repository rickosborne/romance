package org.rickosborne.romance.client.audiobookshelf;

import lombok.Data;

@Data
public class AudioBookShelfEBookFile implements IAudiobookShelfAddedAt, IAudiobookShelfUpdatedAt {
    Long addedAt;
    String ebookFormat;
    String ino;
    AudiobookShelfFileMetadata metadata;
    Long updatedAt;
}
