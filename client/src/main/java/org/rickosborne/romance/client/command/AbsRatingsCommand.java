package org.rickosborne.romance.client.command;

import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.client.AudiobookStoreService;
import org.rickosborne.romance.client.audiobookstore.AbsCredentials;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.sheet.SheetStore;
import org.rickosborne.romance.util.BookBot;
import org.rickosborne.romance.util.BookRating;
import picocli.CommandLine;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.rickosborne.romance.util.BookMerger.bookLikeFilter;
import static org.rickosborne.romance.util.IgnoredBooks.isIgnored;

@Slf4j
@CommandLine.Command(
    name = "abs-ratings",
    description = "Synchronize AudiobookStore.com ratings"
)
public class AbsRatingsCommand extends ASheetCommand {
    @Override
    protected Integer doWithSheets() {
        final BookBot bookBot = getBookBot();
        final AudiobookStoreAuthOptions tabsAuth = bookBot.getAuth();
        final AudiobookStoreService tabsService = bookBot.getAudiobookStoreCache().getService();
        final UUID userGuid = tabsAuth.ensureAuthGuid(tabsService).getUserGuid();
        final AbsCredentials absCredentials = tabsAuth.getAbsCredentials();
        final String penName = Optional.ofNullable(absCredentials.getPenName()).orElse("");
        final String location = Optional.ofNullable(absCredentials.getLocation()).orElse("");
        final List<BookModel> allTabsBooks = bookBot.getTabsAudiobooks();
        final List<BookModel> unratedTabsBooks = allTabsBooks.stream()
            .filter(b -> b.getAudiobookStoreRatings() == 0 && b.getDateRead() != null && !isIgnored(b))
            .collect(Collectors.toList());
        log.info("Found {} unrated TABS books.", unratedTabsBooks.size());
        final SheetStore<BookModel> bookSheet = bookBot.getSheetStoreFactory().buildSheetStore(BookModel.class);
        final List<BookModel> ratedSheetBooks = bookSheet.stream()
            .filter(b -> b.getRatings().get(BookRating.Overall) != null)
            .collect(Collectors.toList());
        log.info("Found {} rated books in the spreadsheet.", ratedSheetBooks.size());
        for (final BookModel unratedBook : unratedTabsBooks) {
            final List<BookModel> ratedMatches = ratedSheetBooks.stream().filter(bookLikeFilter(unratedBook)).collect(Collectors.toList());
            if (ratedMatches.size() > 1) {
                log.warn("More than one book matches {}: {}", unratedBook, ratedMatches.stream().map(BookModel::toString).collect(Collectors.joining("; ")));
            } else if (ratedMatches.isEmpty()) {
                log.debug("Unrated: {}", unratedBook);
            } else {
                final BookModel ratedBook = ratedMatches.get(0);
                final Double sheetOverall = ratedBook.getRatings().get(BookRating.Overall);
                final String tabsSku = unratedBook.getAudiobookStoreSku();
                if (sheetOverall == null || tabsSku == null) {
                    continue;
                }
                final int performance = (int) Math.round(sheetOverall);
                final int narration = (int) Math.ceil(sheetOverall);
                final int story = (int) Math.round((sheetOverall * 3) - performance - narration);
                log.info("Rating {} from {}: {} performance, {} narration, {} story", unratedBook, sheetOverall, performance, narration, story);
                final String isRecommended = sheetOverall >= 4.0d ? "1" : sheetOverall < 3d ? "0" : "null";
                if (isWrite()) {
                    try {
                        tabsService.rateBook(
                            userGuid.toString(),
                            tabsSku,
                            performance,
                            narration,
                            story,
                            penName,
                            location,
                            "",
                            "",
                            isRecommended
                        ).execute();
                    } catch (IOException e) {
                        log.error("Failed to rate " + unratedBook, e);
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return 0;
    }
}
