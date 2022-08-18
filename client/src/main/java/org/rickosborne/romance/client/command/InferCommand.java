package org.rickosborne.romance.client.command;

import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.db.json.JsonStore;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.util.BookBot;
import picocli.CommandLine;

@Slf4j
@CommandLine.Command(
    name = "infer",
    description = "Take existing book data and see what can be extracted from it"
)
public class InferCommand extends ASheetCommand {
    @CommandLine.Mixin
    private BookFilterOptions bookFilterOptions;

    @Override
    protected Integer doWithSheets() {
        final JsonStore<BookModel> bookJsonStore = getJsonStoreFactory().buildJsonStore(BookModel.class);
        final BookBot bookBot = getBookBot();
        for (final BookModel storedBook : bookJsonStore) {
            if (storedBook == null || !bookFilterOptions.bookMatches(storedBook)) {
                continue;
            }
            if (!bookFilterOptions.isEmpty()) {
                storedBook.getMc1().clear();
                storedBook.getMc2().clear();
            }
            bookBot.extendWithTextInference(storedBook);
        }
        return 0;
    }
}
