package org.rickosborne.romance.client.audiobookstore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AbsCredentials {
    String cookie;
    String location;
    String password;
    String penName;
    String requestVerificationToken;
    UUID userGuid;
    String username;
}
