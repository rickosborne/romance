package org.rickosborne.romance.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.BooksSheets;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.client.AudiobookStoreService;
import org.rickosborne.romance.client.AudiobookStoreSuggestService;
import org.rickosborne.romance.client.CacheClient;
import org.rickosborne.romance.client.GoodreadsService;
import org.rickosborne.romance.client.JsonCookieStore;
import org.rickosborne.romance.client.command.AudiobookStoreAuthOptions;
import org.rickosborne.romance.client.html.AudiobookStoreHtml;
import org.rickosborne.romance.client.html.GoodreadsHtml;
import org.rickosborne.romance.client.html.StoryGraphHtml;
import org.rickosborne.romance.client.response.BookInformation;
import org.rickosborne.romance.client.response.UserInformation2;
import org.rickosborne.romance.db.json.JsonStore;
import org.rickosborne.romance.db.json.JsonStoreFactory;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.model.BookSchema;
import org.rickosborne.romance.db.sheet.SheetStoreFactory;
import org.rickosborne.romance.sheet.AdapterFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.rickosborne.romance.util.BookMerger.modelFromBookInformation;
import static org.rickosborne.romance.util.StringStuff.fuzzyListMatch;
import static org.rickosborne.romance.util.StringStuff.fuzzyMatch;

@Slf4j
@Getter
@RequiredArgsConstructor
public class BookBot {
    @Getter(lazy = true, value = AccessLevel.PROTECTED)
    private final AdapterFactory adapterFactory = new AdapterFactory();
    @Getter(lazy = true)
    private final CacheClient<AudiobookStoreService> audiobookStoreCache = AudiobookStoreService.buildCaching();
    @Getter(lazy = true)
    private final AudiobookStoreSuggestService audiobookStoreSuggestService = AudiobookStoreSuggestService.build();
    private final AudiobookStoreAuthOptions auth;
    @Getter(lazy = true)
    private final UUID audiobookStoreUserGuid = Once.supply(() -> {
        final AudiobookStoreAuthOptions a = getAuth();
        if (a == null) {
            return null;
        }
        a.ensureAuthGuid(getAudiobookStoreCache().getService());
        return a.getAbsUserGuid();
    });
    private final BookSchema bookSchema = new BookSchema();
    private final Path cachePath;
    @Getter(lazy = true)
    private final AudiobookStoreHtml audiobookStoreHtml = new AudiobookStoreHtml(getCachePath(), null);
    private final Path cookieStorePath;
    private final Path dbPath;
    @Getter(lazy = true)
    private final GoodreadsHtml goodreadsHtml = new GoodreadsHtml(getCachePath());
    @Getter(lazy = true)
    private final GoodreadsService goodreadsService = GoodreadsService.build();
    private final String googleUserId;
    @Getter(value = AccessLevel.PROTECTED, lazy = true)
    private final JsonCookieStore jsonCookieStore = Once.supply(() -> {
        final Path cookieStorePath = getCookieStorePath();
        if (cookieStorePath == null || !cookieStorePath.toFile().isFile()) {
            throw new IllegalArgumentException("Invalid cookie store path");
        }
        return JsonCookieStore.fromPath(cookieStorePath);
    });
    @Getter(lazy = true)
    private final LanguageParser languageParser = new LanguageParser();
    @Getter(value = AccessLevel.PROTECTED, lazy = true)
    private final NamingConvention namingConvention = new NamingConvention();
    @Getter(lazy = true)
    private final JsonStoreFactory jsonStoreFactory = buildJsonStoreFactory();
    @Getter(lazy = true)
    private final JsonStore<BookModel> bookStore = getJsonStoreFactory().buildJsonStore(BookModel.class);
    @Getter(value = AccessLevel.PROTECTED, lazy = true)
    private final JsonCookieStore sgCookieStore = buildStoryGraphCookieStore();
    @Getter(lazy = true)
    private final SheetStoreFactory sheetStoreFactory = buildSheetStoreFactory();
    @Getter(lazy = true)
    private final Sheets.Spreadsheets spreadsheets = BooksSheets.getSpreadsheets(getGoogleUserId());
    @Getter(lazy = true)
    private final Spreadsheet spreadsheet = BooksSheets.getSpreadsheet(getSpreadsheets());
    @Getter(lazy = true)
    private final StoryGraphHtml storyGraphHtml = buildStoryGraphHtml();
    @Getter(lazy = true)
    private final List<BookModel> tabsAudiobooks = fetchAudiobooks();

    private JsonStoreFactory buildJsonStoreFactory() {
        return new JsonStoreFactory(getDbPath(), getNamingConvention());
    }

    private SheetStoreFactory buildSheetStoreFactory() {
        return new SheetStoreFactory(getNamingConvention(), getGoogleUserId());
    }

    private JsonCookieStore buildStoryGraphCookieStore() {
        return JsonCookieStore.fromPath(Path.of("./.credentials/storygraph-cookies.json"));
    }

    private StoryGraphHtml buildStoryGraphHtml() {
        return new StoryGraphHtml(getCachePath(), getSgCookieStore());
    }

    public BookModel extendAll(
        @NonNull final BookModel original
    ) {
        BookModel model = original;
        model = extendWithAudiobookStorePurchase(model);
        final BookModel existing = getBookStore().findLikeOrMatch(model, found -> fuzzyMatch(found.getTitle(), original.getTitle()) && fuzzyListMatch(found.getAuthorName(), original.getAuthorName()));
        model = mergeBooks(existing, model);
        model = extendWithAudiobookStoreBookInformation(model);
        model = extendWithAudiobookStoreSuggestion(model);
        model = extendWithAudiobookStoreDetails(model);
        model = extendWithGoodReadsAutoComplete(model);
        model = extendWithGoodReadsDetails(model);
        model = extendWithStoryGraphSearch(model);
        return getBookStore().saveIfChanged(model);
    }

    public BookModel extendWithAudiobookStoreBookInformation(
        @NonNull final BookModel original
    ) {
        return mergeBooks(original, fetchAudiobookStoreBookInformation(original));
    }

    public BookModel extendWithAudiobookStoreDetails(
        @NonNull final BookModel original
    ) {
        final URL storeUrl = original.getAudiobookStoreUrl();
        if (storeUrl == null) {
            return original;
        }
        final BookModel storeBook = getAudiobookStoreHtml().getBookModel(storeUrl);
        return mergeBooks(original, storeBook);
    }

    public BookModel extendWithAudiobookStorePurchase(
        @NonNull final BookModel original
    ) {
        final List<BookModel> audiobooks = getTabsAudiobooks();
        final BookModel purchased = audiobooks.stream()
            .filter(a -> fuzzyListMatch(a.getAuthorName(), original.getAuthorName()) && fuzzyMatch(a.getTitle(), original.getTitle()))
            .findAny()
            .orElse(null);
        return mergeBooks(original, purchased);
    }

    public BookModel extendWithAudiobookStoreSuggestion(
        @NonNull final BookModel original
    ) {
        final URL originalUrl = original.getAudiobookStoreUrl();
        if (originalUrl != null) {
            return original;
        }
        final String originalSku = original.getAudiobookStoreSku();
        final BookModel suggestion = getAudiobookStoreSuggestService().findBookLike(original, book -> {
            final BookModel info = fetchAudiobookStoreBookInformation(book);
            if (info == null) {
                return null;
            }
            if ((originalSku == null || originalSku.equals(info.getAudiobookStoreSku()))
                && fuzzyListMatch(original.getAuthorName(), info.getAuthorName())
                && fuzzyMatch(original.getTitle(), info.getTitle())
            ) {
                return mergeBooks(book, info);
            }
            return null;
        });
        if (suggestion != null) {
            return mergeBooks(original, suggestion);
        }
        return original;
    }

    public BookModel extendWithGoodReadsAutoComplete(
        @NonNull final BookModel original
    ) {
        if (original.getGoodreadsUrl() != null) {
            return original;
        }

        final BookModel grBook = getGoodreadsService()
            .findBook(original.getTitle(), original.getAuthorName());
        if (grBook != null) {
            return mergeBooks(original, grBook);
        }
        return original;
    }

    public BookModel extendWithGoodReadsDetails(
        @NonNull final BookModel original
    ) {
        final URL grUrl = original.getGoodreadsUrl();
        if (grUrl == null) {
            return original;
        }
        final BookModel grModel = getGoodreadsHtml().getBookModel(grUrl);
        return mergeBooks(original, grModel);
    }

    public BookModel extendWithStoryGraphSearch(
        @NonNull final BookModel original
    ) {
        if (original.getStorygraphUrl() != null) {
            return original;
        }
        final BookModel sgBook = getStoryGraphHtml().searchForBook(original);
        return mergeBooks(original, sgBook);
    }

    public BookModel extendWithTextInference(
        @NonNull final BookModel original
    ) {
        final String description = original.getPublisherDescription();
        if (description == null || description.isBlank()) {
            return original;
        }
        final BookModel.MainChar storedMc1 = original.getMc1();
        final BookModel.MainChar storedMc2 = original.getMc2();
        if (storedMc1.getName() != null || storedMc2.getName() != null) {
            return original;
        }
        log.info("Summarizing: " + original.getTitle() + " by " + original.getAuthorName());
        log.info("Blurb: " + description);
        final LanguageParser.Summary summary = getLanguageParser().summarize(description);
        final String location = summary.getLocation();
        if (location != null && original.getLocation() == null) {
            original.setLocation(location);
        }
        final List<BookModel.MainChar> summaryChars = summary.getMainChars();
        if (summaryChars == null || summaryChars.isEmpty()) {
            return original;
        }
        for (final BookModel.MainChar summaryChar : summaryChars) {
            final BookModel.MainChar storedMC = targetMC(summaryChar, storedMc1, storedMc2);
            if (storedMC != null) {
                storedMC.importFromIfNotNull(summaryChar);
            } else {
                log.warn("Dropped MC: " + summaryChar);
            }
        }
        return original;
    }

    public BookModel fetchAudiobookStoreBookInformation(@NonNull final BookModel original) {
        final String sku = original.getAudiobookStoreSku();
        final String userGuid = Optional.ofNullable(getAudiobookStoreUserGuid()).map(UUID::toString).orElse(null);
        if (userGuid == null || sku == null) {
            return null;
        }
        final BookInformation info2 = getAudiobookStoreCache().fetchFomCache(new TypeReference<>() {
        }, s -> s.bookInformation(userGuid, sku), userGuid + sku);
        return info2 == null ? null : modelFromBookInformation(info2);
    }

    public List<BookModel> fetchAudiobooks() {
        final AudiobookStoreService storeService = getAudiobookStoreCache().getService();
        final AudiobookStoreAuthOptions storeAuth = getAuth();
        storeAuth.ensureAuthGuid(storeService);
        final UserInformation2 info;
        try {
            info = storeService.userInformation2(storeAuth.getAbsUserGuid().toString()).execute().body();
            if (info == null) {
                return Collections.emptyList();
            }
        } catch (IOException e) {
            log.warn("Could not fetch TABS user info");
            return Collections.emptyList();
        }
        return info.getAudiobooks().stream().map(BookMerger::modelFromBookInformation).collect(Collectors.toList());
    }

    public BookModel mergeBooks(
        final BookModel original,
        final BookModel... others
    ) {
        BookModel book = original;
        for (final BookModel other : others) {
            book = getBookSchema().mergeModels(book, other);
        }
        return book;
    }

    private BookModel.MainChar targetMC(
        final BookModel.MainChar needle,
        @NonNull final BookModel.MainChar... haystacks
    ) {
        if (needle == null) {
            return null;
        }
        final String name = needle.getName();
        if (name == null) {
            return null;
        }
        BookModel.MainChar otherwise = null;
        for (final BookModel.MainChar hay : haystacks) {
            final String hayName = hay.getName();
            if (fuzzyMatch(name, hayName)) {
                return hay;
            } else if (hayName == null && otherwise == null) {
                otherwise = hay;
            }
        }
        return otherwise;
    }
}
