package org.rickosborne.romance.client.audiobookshelf;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AudiobookShelfSeriesProgress {
    boolean isFinished;
    List<UUID> libraryItemIds;
    List<UUID> libraryItemIdsFinished;
}
