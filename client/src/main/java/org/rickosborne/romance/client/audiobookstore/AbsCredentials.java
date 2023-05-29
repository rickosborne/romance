package org.rickosborne.romance.client.audiobookstore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AbsCredentials {
    String location;
    String password;
    String penName;
    UUID userGuid;
    String username;
}
