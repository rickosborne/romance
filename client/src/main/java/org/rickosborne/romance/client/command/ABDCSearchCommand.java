package org.rickosborne.romance.client.command;

import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.client.html.AudiobooksDotComHtml;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.sheet.SheetStore;
import picocli.CommandLine;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.rickosborne.romance.util.BookMerger.bookLikeFilter;

@Slf4j
@CommandLine.Command(
    name = "abdc-search",
    description = "Fetch and display a book from Audiobooks.com"
)
public class ABDCSearchCommand extends ASheetCommand {
    @CommandLine.Parameters(description = "audiobooks.com book URLs")
    private final List<URL> abdcUrls = new LinkedList<>();

    @Override
    protected Integer doWithSheets() {
        final SheetStore<BookModel> sheetBooks = getSheetStoreFactory().buildSheetStore(BookModel.class);
        abdcUrls
            .stream()
            .map(abdcUrl -> {
                log.info("audiobooks.com URL: {}", abdcUrl);
                final AudiobooksDotComHtml html = getAudiobooksDotComHtml();
                BookModel book = html.getBookModelFromBook(abdcUrl);
                final BookModel sheetBook = sheetBooks.findLikeOrMatch(book, bookLikeFilter(book));
                if (sheetBook != null) {
                    return null;
                }
                book = getBookBot().extendAll(book);
                book = getBookBot().extendWithTextInference(book);
                getJsonStoreFactory().buildJsonStore(BookModel.class).save(book);
                return book;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList())
            .forEach(book -> System.out.println(DocTabbed.fromBookModel(book)));
        return null;
    }
}
