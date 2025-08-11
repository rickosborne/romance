package org.rickosborne.romance.client.audiobookshelf;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AudiobookShelfSeriesWithProgressAndRSS extends AudiobookShelfSeries {
    AudiobookShelfSeriesProgress progress;
    String rssFeed;
}
