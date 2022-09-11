package org.rickosborne.romance.client.command;

import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.client.html.AudiobookStoreHtml;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.model.AuthorModel;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.util.BookBot;
import org.rickosborne.romance.util.SheetStuff;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
@CommandLine.Command(
    name = "tabs-data-for-sheet",
    description = "Pull and materialize the Sheet data to JSON files"
)
public class FillFromTABSCommand extends ASheetCommand {

    @SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
    @CommandLine.Option(names = "--require-tabs-url")
    private boolean requireTabsUrl = false;

    @Override
    protected Integer doWithSheets() {
        if (requireTabsUrl) {
            log.info("Requiring TABS URL.  Will not expand.");
        }
        final AudiobookStoreHtml audiobookStoreHtml = getAudiobookStoreHtml();
        final List<Request> changeRequests = new LinkedList<>();
        final DataSet<BookModel> bookData = new DataSet<>(DbModel.Book);
        final DataSet<AuthorModel> authorData = new DataSet<>(DbModel.Author);
        final BookBot bookBot = getBookBot();
        for (final SheetStuff.Indexed<BookModel> bookRecord : bookData.getSheetStore().getRecords()) {
            final BookModel sheetBook = bookRecord.getModel();
            if (requireTabsUrl && sheetBook.getAudiobookStoreUrl() == null) {
                continue;
            }
            final BookModel allBook = bookBot.extendAll(sheetBook);
            final int rowNum = bookRecord.getRowNum();
            final Map<String, String> bookChanges = bookData.getModelSheetAdapter().findChangesToSheet(sheetBook, allBook);
            if (!bookChanges.isEmpty()) {
                System.out.println("~~~ " + rowNum + ":" + bookData.getJsonStore().idFromModel(allBook));
                System.out.println(bookChanges);
                changeRequests.addAll(changeRequestsFromModelChanges(bookData.getSheet(), bookData.getColNums(), rowNum, bookChanges));
            }
            bookData.getJsonStore().saveIfChanged(allBook);
            final URL tabsUrl = allBook.getAudiobookStoreUrl();
            if (tabsUrl != null) {
                final List<AuthorModel> tabsAuthors = audiobookStoreHtml.getAuthorModelsFromBook(tabsUrl);
                for (final AuthorModel tabsAuthor : tabsAuthors) {
                    final AuthorModel sheetAuthor = authorData.getSheetStore().findLike(tabsAuthor);
                    final Integer authorRowNum = authorData.getSheetStore().getRowNum(sheetAuthor);
                    if (authorRowNum != null) {
                        final AuthorModel sheetPlusTabsAuthor = authorData.getModelSchema().mergeModels(tabsAuthor, sheetAuthor);
                        final AuthorModel jsonAuthor = authorData.getJsonStore().findLike(sheetAuthor);
                        final AuthorModel allAuthor = authorData.getModelSchema().mergeModels(jsonAuthor, sheetPlusTabsAuthor);
                        final Map<String, String> authorChanges = authorData.getModelSheetAdapter().findChangesToSheet(sheetAuthor, allAuthor);
                        if (!authorChanges.isEmpty()) {
                            System.out.println("~~~ " + authorData.getJsonStore().idFromModel(allAuthor));
                            System.out.println(authorChanges);
                            changeRequests.addAll(changeRequestsFromModelChanges(authorData.getSheet(), authorData.getColNums(), authorRowNum, authorChanges));
                        }
                    }
                }
            }
        }
        if (isWrite() && !changeRequests.isEmpty()) {
            final BatchUpdateSpreadsheetRequest updateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest()
                .setRequests(changeRequests);
            try {
                getSpreadsheets().batchUpdate(getSpreadsheet().getSpreadsheetId(), updateSpreadsheetRequest).execute();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
