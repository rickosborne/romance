package org.rickosborne.romance.client.audiobookshelf;

import lombok.Data;

@Data
public class AudiobookShelfFileMetadata {
    Long birthtimeMs;
    Long ctimeMs;
    String ext;
    String filename;
    Long mtimeMs;
    String path;
    String relPath;
    Long size;
}
