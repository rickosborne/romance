package org.rickosborne.romance.client.audiobookshelf;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AudiobookShelfSeriesPage extends AAudiobookShelfPage {
    List<AudiobookShelfSeriesBooks> results;
}
