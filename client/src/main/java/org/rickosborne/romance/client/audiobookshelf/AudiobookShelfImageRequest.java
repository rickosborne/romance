package org.rickosborne.romance.client.audiobookshelf;

import lombok.Data;

@Data
public class AudiobookShelfImageRequest {
    String format;
    int height;
    boolean raw;
    int width;
}
