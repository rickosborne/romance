package org.rickosborne.romance.client.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.SheetsScopes;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.List;

import static com.google.api.client.json.gson.GsonFactory.getDefaultInstance;

@Slf4j
public class Google {
    public static final String ACCESS_TYPE_OFFLINE = "offline";
    public static final Path CREDENTIALS_PATH_DEFAULT = Path.of(".credentials/google.json");
    public static final GsonFactory JSON_FACTORY = getDefaultInstance();
    public static final List<String> SCOPES = List.of(SheetsScopes.SPREADSHEETS);
    public static final Path TOKENS_FILE_DEFAULT = Path.of(".credentials");
    public static final Path STORED_CREDENTIAL_PATH = TOKENS_FILE_DEFAULT.resolve("StoredCredential");

    public static Credential getCredentials(@NonNull final String userId) {
        return getCredentials(userId, CREDENTIALS_PATH_DEFAULT);
    }

    public static Credential getCredentials(
        @NonNull final String userId,
        @NonNull final Path credentialsPath
    ) {
        final File credsFile = credentialsPath.toFile();
        if (!credsFile.isFile()) {
            throw new IllegalArgumentException("No such credentials file: " + credsFile);
        }
        try (
            final FileInputStream fileInputStream = new FileInputStream(credsFile);
            final InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream)
        ) {
            final GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, inputStreamReader);
            final GoogleAuthorizationCodeFlow authFlow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                clientSecrets,
                SCOPES
            )
                .setDataStoreFactory(new FileDataStoreFactory(TOKENS_FILE_DEFAULT.toFile()))
                .setAccessType(ACCESS_TYPE_OFFLINE)
                .setApprovalPrompt("auto")
                .build();
            final Credential existingCredential = authFlow.loadCredential(userId);
            if (existingCredential != null) {
                return existingCredential;
            }
            final LocalServerReceiver serverReceiver = new LocalServerReceiver.Builder()
                .setHost("localhost")
                .setPort(80)
                .build();
            return new AuthorizationCodeInstalledApp(authFlow, serverReceiver).authorize(userId);
        } catch (IOException e) {
            log.error(String.format("Could not open credentials file %s: %s", credsFile, e.getMessage()));
            throw new RuntimeException(e);
        } catch (GeneralSecurityException e) {
            log.error(String.format("Could not open HTTP transport: %s", e.getMessage()));
            throw new RuntimeException(e);
        }
    }

    public static Credential regenerateCredentials(@NonNull final String userId) {
        final File credsFile = STORED_CREDENTIAL_PATH.toFile();
        if (credsFile.isFile()) {
            if (!credsFile.delete()) {
                log.warn("Could not delete: {}", credsFile);
            }
        }
        return getCredentials(userId);
    }
}
