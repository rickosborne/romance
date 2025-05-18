package org.rickosborne.romance.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LibraryFileV2 {
    @JsonProperty("runTime")
    String runTime;
    @JsonProperty("size")
    String size;
    @JsonProperty("url")
    String url;
}
