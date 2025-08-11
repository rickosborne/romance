package org.rickosborne.romance.client.audiobookshelf;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
public class AudiobookShelfBookMetadataMinified extends AudiobookShelfBookMetadataBase {
    Boolean abridged;
    String authorName;
    String authorNameLF;
    String narratorName;
    String seriesName;
    String titleIgnorePrefix;
}
