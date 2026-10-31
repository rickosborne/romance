package org.rickosborne.romance.client.command;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.client.AudiobookStoreService;
import org.rickosborne.romance.client.audiobookshelf.AudiobookShelfAuthOptions;
import org.rickosborne.romance.client.audiobookshelf.AudiobookShelfBookMetadataMinified;
import org.rickosborne.romance.client.audiobookshelf.AudiobookShelfBookMinified;
import org.rickosborne.romance.client.audiobookshelf.AudiobookShelfLibrary;
import org.rickosborne.romance.client.audiobookshelf.AudiobookShelfLibraryItem;
import org.rickosborne.romance.client.audiobookshelf.AudiobookShelfLibraryItemsPage;
import org.rickosborne.romance.client.audiobookshelf.AudiobookShelfMediaType;
import org.rickosborne.romance.client.audiobookshelf.AudiobookShelfProgressUpdate;
import org.rickosborne.romance.client.audiobookshelf.AudiobookShelfService;
import org.rickosborne.romance.client.audiobookstore.AbsCredentials;
import org.rickosborne.romance.db.DbJsonWriter;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.util.BookBot;
import org.rickosborne.romance.util.BookStuff;
import picocli.CommandLine;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.rickosborne.romance.util.BookMerger.bookLikeFilter;

@Slf4j
@CommandLine.Command(
    name = "sync-shelf-tabs-progress",
    description = "Synchronize AudiobookShelf and TABS progress markers"
)
public class SyncShelfTabsProgressCommand implements Callable<Integer> {
    @Getter(value = AccessLevel.PROTECTED)
    @CommandLine.Option(names = {"--cache", "-c"}, description = "Path to cache dir", defaultValue = ".cache/html")
    private Path cachePath;
    @Getter(value = AccessLevel.PROTECTED)
    @CommandLine.Option(names = {"--cookies", "-s"}, description = "Path to cookie store", defaultValue = "./.credentials/abs-cookies.json")
    private Path cookieStorePath;
    @Getter(value = AccessLevel.PROTECTED)
    @CommandLine.Option(names = {"--path", "-p"}, description = "Path to DB dir", defaultValue = "book-data")
    private Path dbPath;
    @Getter(value = AccessLevel.PROTECTED)
    @CommandLine.Option(names = {"--dry-run"}, description = "Do not download anything", required = false)
    private boolean dryRun = false;
    @Getter(value = AccessLevel.PROTECTED)
    @CommandLine.Mixin
    private AudiobookShelfAuthOptions shelfAuthOptions;
    @Getter(value = AccessLevel.PROTECTED)
    @CommandLine.Mixin
    AudiobookStoreAuthOptions tabsAuth;

    @Override
    public Integer call() throws Exception {
        final AudiobookStoreService storeService = AudiobookStoreService.build();
        getTabsAuth().ensureAuthGuid(storeService);
        final BookBot bookBot = new BookBot(getTabsAuth(), getCachePath(), getCookieStorePath(), getDbPath(), null, getShelfAuthOptions());
        final AbsCredentials absCredentials = bookBot.getAuth().getAbsCredentials();
        final String requestVerificationToken = absCredentials.getRequestVerificationToken();
        final String tabsCookie = absCredentials.getCookie();
        final List<BookModel> tabsBooks = bookBot.fetchAudiobooks();
        final long tabsComplete = tabsBooks.stream().filter((b) -> b.getDateRead() != null).count();
        log.info("[TABS] Found {} books, {} complete", tabsBooks.size(), tabsComplete);
        final AudiobookShelfService shelf = bookBot.getShelfService();
        final List<AudiobookShelfLibrary> libraries = Objects.requireNonNull(shelf.getLibraries().execute().body()).getLibraries();
        log.info("[shelf] Found {} libraries", libraries.size());
        final int pageSize = 9999;
        final Set<BookModel> tabsUpdates = new HashSet<>();
        final Map<AudiobookShelfLibraryItem, LocalDate> shelfUpdates = new HashMap<>();
        for (final AudiobookShelfLibrary library : libraries) {
            if (library.getMediaType() != AudiobookShelfMediaType.book) continue;
            final AudiobookShelfLibraryItemsPage itemsPage = Objects.requireNonNull(shelf.getLibraryItems(library.getId(), pageSize, 0, "media.metadata.title", 0, null, null, 1, null).execute().body(), () -> String.format("Library with id %s", library.getId()));
            final List<AudiobookShelfLibraryItem> shelfItems = itemsPage.getResults();
            log.info("[shelf] Library {} has {} books", library.getName(), shelfItems.size());
            for (final AudiobookShelfLibraryItem shelfItem : shelfItems) {
                if (shelfItem.getMediaType() != AudiobookShelfMediaType.book) continue;
                final AudiobookShelfBookMinified shelfBook = shelfItem.getMedia();
                final AudiobookShelfBookMetadataMinified shelfMeta = shelfBook.getMetadata();
                final String shelfAuthor = BookStuff.cleanAuthor(shelfMeta.getAuthorName());
                final String shelfTitle = BookStuff.cleanTitle(shelfMeta.getTitle());
                final BookModel titleAndAuthor = BookModel.builder().title(shelfTitle).authorName(shelfAuthor).build();
                final List<BookModel> tabsMatches = tabsBooks.stream().filter(bookLikeFilter(titleAndAuthor)).toList();
                if (tabsMatches.size() != 1) continue;
                final BookModel tabsBook = tabsMatches.getFirst();
                if (tabsBook == null || tabsBook.getAudiobookStoreSku() == null) continue;
                final AudiobookShelfProgressUpdate shelfProgress = shelf.getItemProgress(shelfItem.getId()).execute().body();
                final LocalDate shelfFinishDate = Optional.ofNullable(shelfProgress).map(AudiobookShelfProgressUpdate::getFinishedAt).map(at -> LocalDate.ofInstant(new Date(at).toInstant(), ZoneId.systemDefault())).orElse(null);
                final boolean tabsFinished = tabsBook.getDateRead() != null;
                if (!tabsFinished && shelfFinishDate == null) continue;
                if (tabsFinished && shelfFinishDate != null) continue;
                if (shelfFinishDate != null) {
                    log.info("[TABS] Update {} as read on {}", tabsBook, shelfFinishDate);
                    tabsUpdates.add(tabsBook);
                } else {
                    log.info("[shelf] Update {} as read on {}", tabsBook, tabsBook.getDateRead());
                    shelfUpdates.put(shelfItem, tabsBook.getDateRead());
                }
            }
        }
        if (!shelfUpdates.isEmpty()) {
            log.info("[shelf] Update {}", shelfUpdates.size());
            if (!isDryRun()) {
                for (final Map.Entry<AudiobookShelfLibraryItem, LocalDate> pair : shelfUpdates.entrySet()) {
                    final AudiobookShelfLibraryItem libraryItem = pair.getKey();
                    final UUID id = libraryItem.getId();
                    final LocalDate date = pair.getValue();
                    log.info("[shelf] Update {} to {}", libraryItem.loggable(), date);
                    final long dateMs = date.toEpochSecond(LocalTime.NOON, ZoneOffset.UTC) * 1_000;
                    final AudiobookShelfProgressUpdate progress = new AudiobookShelfProgressUpdate();
                    progress.setProgress(1.0d);
                    progress.setStartedAt(dateMs);
                    progress.setFinishedAt(dateMs);
                    progress.setIsFinished(true);
                    shelf.updateItemProgress(id, progress).execute();
                }
            }
        }
        if (!tabsUpdates.isEmpty()) {
            log.info("[TABS] Update {}", tabsUpdates.size());
            if (!isDryRun()) {
                for (final BookModel book : tabsUpdates) {
                    log.info("[TABS] Read: {}", book);
                    final String json = storeService.markAsComplete(tabsAuth.getAbsUserGuid(), book.getAudiobookStoreSku(), true, requestVerificationToken, tabsCookie).execute().body();
                    final AudiobookStoreService.MarkCompleteResponse response = DbJsonWriter.getJsonMapper().readValue(json, AudiobookStoreService.MarkCompleteResponse.class);
                    if (!"Saved".equals(response.statusMessage)) {
                        log.error("Did not save: {} {}", response.status, response.statusMessage);
                        break;
                    }
                }
            }
        }
        return 0;
    }
}
