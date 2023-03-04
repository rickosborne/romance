package org.rickosborne.romance.client.command;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.client.AudiobookStoreService;
import org.rickosborne.romance.client.audiobookstore.AbsCredentials;
import org.rickosborne.romance.client.response.Login;
import org.rickosborne.romance.db.DbJsonWriter;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Getter
public class AudiobookStoreAuthOptions {
    @CommandLine.Option(names = "password", description = "ABS Password", defaultValue = "${env:AUDIOBOOKSTORE_PASSWORD:-}")
    private String absPassword;
    @Setter
    @CommandLine.Option(names = "userGuid", description = "ABS User Guid", defaultValue = "${env:AUDIOBOOKSTORE_USERGUID:-}")
    private UUID absUserGuid;
    @CommandLine.Option(names = "username", description = "ABS Username", defaultValue = "${env:AUDIOBOOKSTORE_USERNAME:-}")
    private String absUsername;
    @CommandLine.Option(names = "abs-credentials-file", description = "ABS credentials file", defaultValue = "${env:AUDIOBOOKSTORE_CREDENTIALS_FILE:.credentials/audiobookstore.json}")
    private Path credentialsFile;

    @Getter(lazy = true, value = AccessLevel.PROTECTED)
    private final AbsCredentials absCredentials = loadFromFile();

    public Login ensureAuthGuid(final AudiobookStoreService service) {
        if (getAbsUserGuid() != null) {
            return new Login(absUserGuid);
        }
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
        log.debug("Fetched user GUID: " + absUserGuid);
        return login;
    }

    public String getAbsPassword() {
        if (absPassword == null) {
            absPassword = Optional.ofNullable(getAbsCredentials())
                .map(AbsCredentials::getPassword)
                .orElse(null);
        }
        return absPassword;
    }

    public UUID getAbsUserGuid() {
        if (absUserGuid == null) {
            absUserGuid = Optional.ofNullable(getAbsCredentials())
                .map(AbsCredentials::getUserGuid)
                .orElse(null);
        }
        return absUserGuid;
    }

    public String getAbsUsername() {
        if (absUsername == null) {
            absUsername = Optional.ofNullable(getAbsCredentials())
                .map(AbsCredentials::getUsername)
                .orElse(null);
        }
        return absUsername;
    }

    private AbsCredentials loadFromFile() {
        if (credentialsFile != null) {
            try {
                return DbJsonWriter.readFile(credentialsFile.toFile(), AbsCredentials.class);
            } catch (IllegalArgumentException e) {
                log.warn("Could not read: " + credentialsFile, e);
            }
        }
        return null;
    }
}
