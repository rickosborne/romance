package org.rickosborne.romance.client.audiobookshelf;

import lombok.Data;

@Data
public class AudiobookShelfBookChapter {
    Integer chapter;
    Double end;
    String id;
    /**
     * In seconds.
     */
    Integer start;
    String title;
}
