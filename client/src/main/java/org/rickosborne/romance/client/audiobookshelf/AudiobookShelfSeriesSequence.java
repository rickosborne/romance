package org.rickosborne.romance.client.audiobookshelf;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
public class AudiobookShelfSeriesSequence {
    String displayName;
    /**
     * Like `new-1751585507014`.
     */
    String id;
    String name;
    String sequence;

    public AudiobookShelfSeriesSequence copy() {
        return new AudiobookShelfSeriesSequence(displayName, id, name, sequence);
    }
}
