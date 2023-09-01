package org.rickosborne.romance.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LibraryFile {
    @JsonProperty("RunTime")
    String runTime;
    @JsonProperty("Size")
    String size;
    @JsonProperty("Url")
    String url;
}
