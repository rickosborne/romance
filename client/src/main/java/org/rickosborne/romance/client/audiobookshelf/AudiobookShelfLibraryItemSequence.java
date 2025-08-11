package org.rickosborne.romance.client.audiobookshelf;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AudiobookShelfLibraryItemSequence extends AudiobookShelfLibraryItem {
    AudiobookShelfSeriesSequence sequence;
}
