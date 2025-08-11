package org.rickosborne.romance.client.audiobookshelf;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AudiobookShelfBook {
    List<AudiobookShelfAudioFile> audioFiles;
    List<AudiobookShelfBookChapter> chapters;
    String coverPath;
    AudioBookShelfEBookFile ebookFile;
    UUID libraryItemId;
    List<Integer> missingParts;
    List<String> tags;
}
