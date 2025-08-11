package org.rickosborne.romance.client.audiobookshelf;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AudiobookShelfSeriesNumBooks {
    UUID id;
    List<UUID> libraryItemIds;
    String name;
    int numBooks;
}
