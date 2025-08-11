package org.rickosborne.romance.client.audiobookshelf;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.UUID;

@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
public class AudiobookShelfProgressUpdate {
    Long currentTime;
    Double duration;
    String ebookLocation;
    Double ebookProgress;
    UUID episodeId;
    Long finishedAt;
    Boolean hideFromContinueListening;
    UUID id;
    Boolean isFinished;
    Long lastUpdate;
    UUID libraryItemId;
    Double markAsFinishedPercentComplete;
    Double markAsFinishedTimeRemaining;
    UUID mediaItemId;
    AudiobookShelfMediaType mediaItemType;
    Double progress;
    Long startedAt;
    UUID userId;
}
