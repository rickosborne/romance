package org.rickosborne.romance.client.command;

import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.client.html.AudiobookStoreHtml;
import org.rickosborne.romance.util.BookBot;
import picocli.CommandLine;

@Slf4j
@CommandLine.Command(
    name = "abs-category",
    description = "Fetch and display ABS user info"
)
public class AbsCategoryCommand extends ASheetCommand {
    @Override
    protected Integer doWithSheets() {
        final BookBot bot = getBookBot();
        final AudiobookStoreHtml tabsHtml = bot.getAudiobookStoreHtml();

        return 0;
    }
}
