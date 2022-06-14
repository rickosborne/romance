package org.rickosborne.audiobookstore.client.cli;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.rickosborne.audiobookstore.client.AudiobookStoreService;
import org.rickosborne.audiobookstore.client.response.Login;
import picocli.CommandLine;

import java.io.IOException;
import java.util.UUID;

@Log
@Getter
public class AudiobookStoreAuthOptions {
    @CommandLine.Option(names = "password", description = "ABS Password", defaultValue = "${env:AUDIOBOOKSTORE_PASSWORD:-}")
    private String absPassword;
    @Setter
    @CommandLine.Option(names = "userGuid", description = "ABS User Guid", defaultValue = "${env:AUDIOBOOKSTORE_USERGUID:-}")
    private UUID absUserGuid;
    @CommandLine.Option(names = "username", description = "ABS Username", defaultValue = "${env:AUDIOBOOKSTORE_USERNAME:-}")
    private String absUsername;

    public Login ensureAuthGuid(final AudiobookStoreService service) {
        if (absUserGuid == null) {
            if (absUsername == null || absUsername.isBlank() || absPassword == null || absPassword.isBlank()) {
                throw new IllegalArgumentException("Missing ABS credentials");
            }
            final Login login;
            try {
                login = service.checkLogin(absUsername, absPassword).execute().body();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (login == null) {
                throw new NullPointerException("ABS user not found or bad password");
            }
            setAbsUserGuid(login.getUserGuid());
            log.fine("Fetched user GUID: " + absUserGuid);
            return login;
        }
        return null;
    }

}
