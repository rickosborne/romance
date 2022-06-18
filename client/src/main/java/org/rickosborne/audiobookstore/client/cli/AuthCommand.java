package org.rickosborne.audiobookstore.client.cli;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import lombok.extern.java.Log;
import org.rickosborne.audiobookstore.client.google.Google;
import picocli.CommandLine;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.Callable;

@Log
@CommandLine.Command(
    name = "auth",
    description = "Authorize and save credentials"
)
public class AuthCommand implements Callable<Integer> {
    @CommandLine.Parameters
    private String userId;

    @Override
    public Integer call() throws GeneralSecurityException, IOException {
        final Credential credential = Google.getCredentials(userId);
        if (credential == null) {
            log.warning("No Google credentials");
            return 2;
        }
        final Sheets sheets = new Sheets.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName(getClass().getPackageName())
            .build();
        log.info("Got a Sheets client!  Check your credentials file.");
        return 0;
    }
}
