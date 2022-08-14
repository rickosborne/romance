package org.rickosborne.romance.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
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
import org.rickosborne.romance.client.response.AudiobookStoreSuggestion;
import org.rickosborne.romance.client.response.BookInformation;
import org.rickosborne.romance.client.response.GoodreadsAutoComplete;
import org.rickosborne.romance.db.json.JsonStore;
import org.rickosborne.romance.db.json.JsonStoreFactory;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.model.BookSchema;
import org.rickosborne.romance.db.sheet.SheetStoreFactory;
import org.rickosborne.romance.sheet.AdapterFactory;

import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.rickosborne.romance.client.command.BookMerger.modelFromAudiobookStoreSuggestion;
import static org.rickosborne.romance.client.command.BookMerger.modelFromBookInformation;
import static org.rickosborne.romance.client.command.BookMerger.modelFromGoodreadsAutoComplete;
import static org.rickosborne.romance.util.StringStuff.fuzzyMatch;

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
    @Getter(value = AccessLevel.PROTECTED, lazy = true)
    private final NamingConvention namingConvention = new NamingConvention();
    @Getter(lazy = true)
    private final JsonStoreFactory jsonStoreFactory = buildJsonStoreFactory();
    @Getter(lazy = true)
    private final JsonStore<BookModel> bookStore = getJsonStoreFactory().buildJsonStore(BookModel.class);
    @Getter(lazy = true)
    private final SheetStoreFactory sheetStoreFactory = buildSheetStoreFactory();
    @Getter(lazy = true)
    private final Sheets.Spreadsheets spreadsheets = BooksSheets.getSpreadsheets(getGoogleUserId());
    @Getter(lazy = true)
    private final Spreadsheet spreadsheet = BooksSheets.getSpreadsheet(getSpreadsheets());
    @Getter(lazy = true)
    private final StoryGraphHtml storyGraphHtml = new StoryGraphHtml(getCachePath(), null);

    private JsonStoreFactory buildJsonStoreFactory() {
        return new JsonStoreFactory(getDbPath(), getNamingConvention());
    }

    private SheetStoreFactory buildSheetStoreFactory() {
        return new SheetStoreFactory(getNamingConvention(), getGoogleUserId());
    }

    public BookModel extendAll(
        @NonNull final BookModel original
    ) {
        BookModel model = original;
        final BookModel existing = getBookStore().findLikeOrMatch(model, found -> fuzzyMatch(found.getTitle(), original.getTitle()) && fuzzyMatch(found.getAuthorName(), original.getAuthorName()));
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
        final String sku = original.getAudiobookStoreSku();
        final String userGuid = Optional.ofNullable(getAudiobookStoreUserGuid()).map(UUID::toString).orElse(null);
        if (userGuid == null || sku == null) {
            return original;
        }
        final BookInformation info2 = getAudiobookStoreCache().fetchFomCache(new TypeReference<>() {
        }, s -> s.bookInformation(userGuid, sku), userGuid + sku);
        return mergeBooks(original, modelFromBookInformation(info2));
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

    public BookModel extendWithAudiobookStoreSuggestion(
        @NonNull final BookModel original
    ) {
        if (original.getAudiobookStoreUrl() != null) {
            return original;
        }
        final AudiobookStoreSuggestion suggestion = getAudiobookStoreSuggestService().findBookLike(original);
        if (suggestion != null) {
            return mergeBooks(original, modelFromAudiobookStoreSuggestion(suggestion));
        }
        return original;
    }

    public BookModel extendWithGoodReadsAutoComplete(
        @NonNull final BookModel original
    ) {
        if (original.getGoodreadsUrl() != null) {
            return original;
        }
        final GoodreadsAutoComplete grBook = getGoodreadsService().findBook(original.getTitle(), original.getAuthorName());
        if (grBook != null) {
            return mergeBooks(original, modelFromGoodreadsAutoComplete(grBook));
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
}
