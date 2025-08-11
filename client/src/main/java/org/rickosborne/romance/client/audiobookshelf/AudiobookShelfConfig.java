package org.rickosborne.romance.client.audiobookshelf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AudiobookShelfConfig {
    String apiToken;
    String hostName;
    int port;
    boolean secure = false;

    @JsonIgnore
    public String getUrl() {
        return (secure ? "https" : "http") +
            "://" +
            hostName +
            ":" +
            port;
    }
}
