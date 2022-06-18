package org.rickosborne.romance.client.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.net.URL;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoodreadsAuthor {
    @JsonProperty("id")
    Integer id;
    @JsonProperty("name")
    String name;
    @JsonProperty("profileUrl")
    URL profileUrl;
    @JsonProperty("worksListUrl")
    URL worksListUrl;
}
