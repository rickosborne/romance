package org.rickosborne.romance;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import org.rickosborne.romance.client.client.google.Google;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.stream.Collectors;

public interface BooksSheets {
    String BOOKS_SHEET_TITLE = "Books";
    @SuppressWarnings("SpellCheckingInspection")
    String SHEET_ID = "1cxBs0FXcFNvl_kkj4l6FjndICsLQ75no5byE_I3n7-s";

    static Sheet booksSheet(final Spreadsheet spreadsheet) {
        return sheetTitled(BOOKS_SHEET_TITLE, spreadsheet);
    }

    private static Credential getSheetsCredential(final String userId) {
        final Credential credential = Google.getCredentials(userId);
        if (credential == null) {
            throw new IllegalArgumentException("Could not get credentials for: " + userId);
        }
        return credential;
    }

    static Spreadsheet getSpreadsheet(final Spreadsheets spreadsheets) {
        try {
            return spreadsheets.get(BooksSheets.SHEET_ID).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static Spreadsheets getSpreadsheets(final String userId) {
        try {
            return new com.google.api.services.sheets.v4.Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                getSheetsCredential(userId)
            )
                .setApplicationName(BooksSheets.class.getPackageName())
                .build()
                .spreadsheets();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    static Sheet sheetTitled(final String title, final Spreadsheet spreadsheet) {
        return spreadsheet.getSheets().stream()
            .filter(s -> title.equals(s.getProperties().getTitle()))
            .findAny()
            .orElseThrow(() -> new IllegalStateException(String.format("Could not find sheet \"%s\" among: %s", title, spreadsheet.getSheets().stream().map(s -> s.getProperties().getTitle()).collect(Collectors.joining(", ")))));
    }

    static List<List<Object>> sheetValues(final String sheetTitle, final Spreadsheets spreadsheets) {
        try {
            return spreadsheets.values().get(SHEET_ID, sheetTitle).execute().getValues();
        } catch (IOException e) {
            throw new RuntimeException("Could not get access to " + sheetTitle + " values");
        }
    }
}
