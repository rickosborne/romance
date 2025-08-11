package org.rickosborne.romance.client.audiobookshelf;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.annotation.Nullable;
import java.time.Instant;

public interface IAudiobookShelfUpdatedAt {
    @JsonIgnore
    @Nullable
    default Instant getUpdateAtAsInstant() {
        final Long at = getUpdatedAt();
        return at == null ? null : Instant.ofEpochMilli(at);
    }

    Long getUpdatedAt();

    void setUpdatedAt(Long updatedAt);
}
