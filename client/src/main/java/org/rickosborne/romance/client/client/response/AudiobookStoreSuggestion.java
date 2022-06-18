package org.rickosborne.romance.client.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.net.URL;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AudiobookStoreSuggestion {
    @JsonProperty("Format")
    String format;
    @JsonProperty("KeyId")
    String keyId;
    @JsonProperty("Name")
    String name;
    @JsonProperty("Rank")
    Integer rank;
    @JsonProperty("SmallImageUrl")
    URL smallImageUrl;
    @JsonProperty("Title")
    String title;
    @JsonProperty("TypeId")
    Integer typeId;
    @JsonProperty("Url")
    String urlPath;

    public String getCleanTitle() {
        return title == null ? null : title.replace(" (Unabridged)", "");
    }
}
