package org.rickosborne.romance.client.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookFile {
    @JsonProperty("Channels")
    Integer channels;
    @JsonProperty("FileOrder")
    Integer fileOrder;
    @JsonProperty("FileSize")
    Integer fileSize;
    @JsonProperty("FrameRate")
    Integer frameRate;
    @JsonProperty("Label")
    String label;
    @JsonProperty("RunTime")
    Double runTime;
    @JsonProperty("Url")
    String url;
}
