package org.rickosborne.romance.client.audiobookshelf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
public class AudiobookShelfLibrary {
    Long createdAt;
    Integer displayOrder;
    List<AudiobookShelfFolder> folders;
    String icon;
    UUID id;
    Long lastScan;
    String lastScanVersion;
    Long lastUpdate;
    AudiobookShelfMediaType mediaType;
    String name;
    String provider;
    AudiobookShelfLibrarySettings settings;

    @JsonIgnore
    public Instant getCreateAtAsInstant() {
        return Instant.ofEpochMilli(createdAt);
    }

    @JsonIgnore
    public Instant getLastUpdateAsInstant() {
        return Instant.ofEpochMilli(lastUpdate);
    }
}
