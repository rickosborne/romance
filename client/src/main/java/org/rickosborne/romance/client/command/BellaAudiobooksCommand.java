package org.rickosborne.romance.client.command;

import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.client.html.BellaHtml;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.json.JsonStore;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.model.ModelSchema;
import org.rickosborne.romance.db.sheet.SheetStore;
import org.rickosborne.romance.util.BookBot;
import picocli.CommandLine;

import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@CommandLine.Command(
    name = "bella-audiobooks",
    description = "Pull audiobooks from Bella"
)
public class BellaAudiobooksCommand extends ASheetCommand {
    @Override
    protected Integer doWithSheets() {
        final SheetStore<BookModel> sheetStore = getSheetStoreFactory().buildSheetStore(BookModel.class);
        final Sheet sheet = sheetStore.getSheet();
        final JsonStore<BookModel> jsonStore = getJsonStoreFactory().buildJsonStore(BookModel.class);
        final int firstEmptyRowNum = sheetStore.getFirstEmptyRowNum();
        final BookBot bookBot = getBookBot();
        final BellaHtml bella = bookBot.getBellaHtml();
        final DataSet<BookModel> bookData = new DataSet<>(DbModel.Book);
        final LinkedList<Integer> donePageNums = new LinkedList<>();
        final LinkedList<Integer> toDoPageNums = new LinkedList<>();
        final Set<String> doneUrls = new HashSet<>();
        toDoPageNums.add(1);
        int nextRow = firstEmptyRowNum;
        final List<Request> allChanges = getChangeRequests();
        final ModelSchema<BookModel> bookSchema = bookData.getModelSchema();
        while (!toDoPageNums.isEmpty()) {
            final int pageNum = toDoPageNums.poll();
            final BellaHtml.LinksPage page = bella.getAudiobookLinksPage(pageNum);
            donePageNums.add(pageNum);
            for (final int otherPage : page.getPageLinkNums()) {
                if (!toDoPageNums.contains(otherPage) && !donePageNums.contains(otherPage)) {
                    toDoPageNums.add(otherPage);
                }
            }
            for (final URL bookLink : page.getBookLinks()) {
                final String bookHref = bookLink.toString();
                if (doneUrls.contains(bookHref)) {
                    continue;
                }
                doneUrls.add(bookHref);
                final BookModel book = bella.getBookModel(bookLink);
                if (book == null) {
                    continue;
                }
                final BookModel existing = sheetStore.findLike(book);
                final int rowToModify;
                final BookModel beforeBook;
                final BookModel afterBook;
                if (existing == null) {
                    beforeBook = BookModel.build();
                    afterBook = bookBot.extendAll(book);
                    rowToModify = nextRow;
                    nextRow++;
                } else {
                    rowToModify = sheetStore.getRowNum(existing);
                    beforeBook = existing;
                    afterBook = bookSchema.mergeModels(bookSchema.mergeModels(jsonStore.findLikeFromCache(existing), existing), book);
                }
                final BookModel withChars = bookBot.extendWithTextInference(afterBook);
                final Map<String, String> bookChanges = bookData.getModelSheetAdapter().findChangesToSheet(beforeBook, withChars);
                final List<Request> changeRequests = changeRequestsFromModelChanges(sheet, bookData.getColNums(), rowToModify, bookChanges);
                allChanges.addAll(changeRequests);
                jsonStore.save(withChars);
                writeChangesIfRequested();
            }
        }
        return null;
    }
}
