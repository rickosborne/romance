package org.rickosborne.romance.client.audiobookshelf;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AudiobookShelfAudioTrack extends AudiobookShelfAudioFile {
    String codec;
    String contentUrl;
    /**
     * In seconds.
     */
    Double startOffset;
    String title;
}
