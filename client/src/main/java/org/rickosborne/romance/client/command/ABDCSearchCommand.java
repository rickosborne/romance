package org.rickosborne.romance.client.command;

import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.client.html.AudiobooksDotComHtml;
import org.rickosborne.romance.db.model.BookModel;
import picocli.CommandLine;

import java.net.URL;

@Slf4j
@CommandLine.Command(
    name = "abdc-search",
    description = "Fetch and display a book from Audiobooks.com"
)
public class ABDCSearchCommand extends ASheetCommand {
    @CommandLine.Option(names = {"--abdc-url"}, description = "audiobooks.com book URL")
    private URL abdcUrl;

    @Override
    protected Integer doWithSheets() {
        if (abdcUrl == null) {
            return 1;
        }
        log.info("audiobooks.com URL: {}", abdcUrl);
        final AudiobooksDotComHtml html = getAudiobooksDotComHtml();
        BookModel book = html.getBookModelFromBook(abdcUrl);
        book = getBookBot().extendAll(book);
        book = getBookBot().extendWithTextInference(book);
        getJsonStoreFactory().buildJsonStore(BookModel.class).save(book);
        System.out.println(DocTabbed.fromBookModel(book));
        return null;
    }
}
