package org.rickosborne.romance.client.command;

import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.client.AudiobookStoreService;
import org.rickosborne.romance.client.response.Login;
import org.rickosborne.romance.client.response.UserInformation2;
import org.rickosborne.romance.util.BookBot;
import org.rickosborne.romance.util.BookMerger;
import picocli.CommandLine;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@CommandLine.Command(
    name = "read",
    description = "Fetch and display recently finished reads"
)
public class ReadCommand extends ASheetCommand {
    @Override
    protected Integer doWithSheets() {
        final BookBot bot = getBookBot();
        final AudiobookStoreAuthOptions tabsAuth = getTabsAuth();
        final AudiobookStoreService tabsService = bot.getAudiobookStoreCache().getService();
        final Login login = tabsAuth.ensureAuthGuid(tabsService);
        try {
            final UserInformation2 userInfo = tabsService.userInformation2(login.getUserGuid().toString())
                .execute().body();
            Objects.requireNonNull(userInfo).getAudiobooks().stream()
                .map(BookMerger::modelFromBookInformation)
                .filter(b -> b.getDateRead() != null)
                .sorted((a, b) -> b.getDateRead().compareTo(a.getDateRead()))
                .limit(10)
                .forEachOrdered(b -> System.out.printf("%s\t%s%n", b.getDateRead(), b));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
