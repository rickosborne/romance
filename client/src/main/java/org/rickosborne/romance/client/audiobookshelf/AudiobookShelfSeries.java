package org.rickosborne.romance.client.audiobookshelf;

import lombok.Data;

import java.util.UUID;

@Data
public class AudiobookShelfSeries {
    String description;
    UUID id;
    String name;
}
