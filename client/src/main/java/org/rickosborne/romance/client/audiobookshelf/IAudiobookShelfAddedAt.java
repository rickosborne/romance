package org.rickosborne.romance.client.audiobookshelf;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

public interface IAudiobookShelfAddedAt {
    Long getAddedAt();

    @JsonIgnore
    default Instant getAddedAtAsInstant() {
        final Long at = getAddedAt();
        return at == null ? null : Instant.ofEpochMilli(at);
    }

    void setAddedAt(Long addedAt);
}
