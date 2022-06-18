package org.rickosborne.audiobookstore.client.cli;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.GridData;
import com.google.api.services.sheets.v4.model.GridProperties;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.extern.java.Log;
import picocli.CommandLine;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.Callable;

import static org.rickosborne.audiobookstore.Sheets.BOOKS_SHEET_TITLE;
import static org.rickosborne.audiobookstore.Sheets.SHEET_ID;
import static org.rickosborne.audiobookstore.Sheets.booksSheet;
import static org.rickosborne.audiobookstore.Sheets.getSpreadsheet;
import static org.rickosborne.audiobookstore.Sheets.getSpreadsheets;
import static org.rickosborne.audiobookstore.Sheets.sheetValues;

@Log
@CommandLine.Command(
    name = "auth",
    description = "Authorize and save credentials"
)
public class AuthCommand implements Callable<Integer> {
    @CommandLine.Parameters(paramLabel = "USERID", description = "Google user ID, generally an email address")
    private String userId;

    @Override
    public Integer call() throws GeneralSecurityException, IOException {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Expected parameter: userId");
        }
        final Sheets.Spreadsheets spreadsheets = getSpreadsheets(userId);
        final Spreadsheet spreadsheet = getSpreadsheet(spreadsheets);
        if (spreadsheet == null || spreadsheet.isEmpty()) {
            throw new IllegalStateException("Could not find, open, or retrieve Sheet.");
        }
        final Sheet books = booksSheet(spreadsheet);
        final GridProperties gridProperties = books.getProperties().getGridProperties();
        final Integer frozenRowCount = gridProperties.getFrozenRowCount();
        final List<List<Object>> booksValues = sheetValues(BOOKS_SHEET_TITLE, spreadsheets);
        final int bookCount = booksValues.size() - frozenRowCount;
        System.out.println("Got access to \"" + spreadsheet.getProperties().getTitle() + "\" with " + bookCount + " books.");
        return 0;
    }
}
