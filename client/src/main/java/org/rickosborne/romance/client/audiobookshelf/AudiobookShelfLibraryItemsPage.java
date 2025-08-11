package org.rickosborne.romance.client.audiobookshelf;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AudiobookShelfLibraryItemsPage extends AAudiobookShelfPage {
    @JsonProperty("collapseseries")
    Boolean collapseSeries;
    AudiobookShelfMediaType mediaType;
    List<AudiobookShelfLibraryItem> results;
}
