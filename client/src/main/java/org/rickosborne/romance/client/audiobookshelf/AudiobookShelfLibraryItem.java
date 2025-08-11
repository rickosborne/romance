package org.rickosborne.romance.client.audiobookshelf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties({"libraryFiles"})
public class AudiobookShelfLibraryItem implements IAudiobookShelfAddedAt, IAudiobookShelfUpdatedAt {
    Long addedAt;
    Long birthtimeMs;
    Long ctimeMs;
    UUID folderId;
    UUID id;
    String ino;
    Boolean isFile;
    Boolean isInvalid;
    Boolean isMissing;
    Long lastScan;
    UUID libraryId;
    AudiobookShelfBookMinified media;
    AudiobookShelfMediaType mediaType;
    Long mtimeMs;
    Integer numFiles;
    String oldLibraryItemId;
    String path;
    String relPath;
    String scanVersion;
    Long size;
    Long updatedAt;
}
