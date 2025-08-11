package org.rickosborne.romance.client.audiobookshelf;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AudiobookShelfSeriesBooks implements IAudiobookShelfAddedAt {
    Long addedAt;
    List<AudiobookShelfLibraryItemSequence> books;
    UUID id;
    String name;
    String nameIgnorePrefix;
    String nameIgnorePrefixSort;
    /**
     * In seconds.
     */
    int totalDuration;
    /**
     * Should always be `series`.
     */
    String type;
}
