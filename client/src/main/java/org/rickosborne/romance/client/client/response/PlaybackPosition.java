package org.rickosborne.romance.client.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaybackPosition {
    @JsonProperty("AppPlaybackPositionId")
    String appPlaybackPositionId;

    @JsonProperty("CreateDateTime")
    String createDateTime;

    @JsonProperty("DeviceId")
    String deviceId;

    @JsonProperty("FileOrder")
    Integer fileOrder;

    @JsonProperty("IsPodcast")
    Boolean isPodcast;

    @JsonProperty("PlaybackPosition")
    Integer playbackPosition;
}
