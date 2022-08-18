package org.rickosborne.romance.client.command;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.client.html.AudiobookStoreHtml;
import org.rickosborne.romance.db.DbJsonWriter;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.sheet.SheetStore;
import org.rickosborne.romance.util.BookBot;
import org.rickosborne.romance.util.LanguageParser;
import org.rickosborne.romance.util.SheetStuff;
import picocli.CommandLine;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.rickosborne.romance.util.FileStuff.withCachedFile;
import static org.rickosborne.romance.util.StringStuff.alphaOnly;

@CommandLine.Command(
    name = "wishlist",
    description = "Fetch your wishlist from AudiobookStore"
)
@Slf4j
public class WishlistCommand extends ASheetCommand {
    private static final Duration wishlistCacheExpiry = Duration.ofHours(1);
    private static final String wishlistCacheFileName = "wishlist.json";
    private final LanguageParser languageParser = new LanguageParser();

    private Stream<String> bookTitleAuthor(final BookModel bookModel) {
        return Arrays.stream(bookModel.getAuthorName().split(",\\s*"))
            .map(authorName -> alphaOnly(authorName) + "\t" + alphaOnly(bookModel.getTitle()));
    }

    @Override
    public Integer doWithSheets() {
        final String absUsername = getTabsAuth().getAbsUsername();
        final String absPassword = getTabsAuth().getAbsPassword();
        Objects.requireNonNull(absUsername, "TABS username is required.");
        Objects.requireNonNull(absPassword, "TABS password is required");
        final SheetStore<BookModel> sheetStore = getSheetStoreFactory().buildSheetStore(BookModel.class);
        final AudiobookStoreHtml storeHtml = getAudiobookStoreHtml();
        final List<BookModel> wishlist = getWishlist(absUsername, absPassword, sheetStore, storeHtml);
        if (wishlist.isEmpty()) {
            System.out.println("(no wishlist)");
            return 0;
        }
        final BookBot bookBot = getBookBot();
        final List<BookModel> extendedBooks = wishlist.stream()
            .map(bookBot::extendAll)
            .map(bookBot::extendWithTextInference)
            .collect(Collectors.toList());
        for (final BookModel book : extendedBooks) {
            final DocTabbed docTabbed = DocTabbed.fromBookModel(book);
            System.out.println(docTabbed.toString());
        }
        return 0;
    }

    private List<BookModel> getWishlist(
        final String absUsername,
        final String absPassword,
        final SheetStore<BookModel> sheetStore,
        final AudiobookStoreHtml storeHtml
    ) {
        return withCachedFile(
            getCachePath().resolve(wishlistCacheFileName).toFile(),
            wishlistCacheExpiry,
            () -> {
                final List<BookModel> wishlist = storeHtml.withBrowser(browser -> {
                    storeHtml.headlessSignIn(browser, absUsername, absPassword);
                    return storeHtml.getWishlist(browser);
                });
                final Set<String> bookTitlesAuthors = sheetStore.getRecords().stream()
                    .map(SheetStuff.Indexed::getModel)
                    .flatMap(this::bookTitleAuthor)
                    .collect(Collectors.toSet());
                wishlist.removeIf(book -> bookTitleAuthor(book).anyMatch(bookTitlesAuthors::contains));
                return wishlist;
            },
            file -> {
                try {
                    log.info("Using cached wishlist: " + file);
                    return DbJsonWriter.getJsonMapper().readValue(file, WishlistCacheFile.class).getWishlist();
                } catch (IOException e) {
                    throw new RuntimeException("Could not read Wishlist cache file", e);
                }
            },
            (file, wl) -> {
                try {
                    log.info("Saving wishlist to cache: " + file);
                    DbJsonWriter.getJsonWriter().writeValue(file, new WishlistCacheFile(wl));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }

    @Value
    private static class WishlistCacheFile {
        List<BookModel> wishlist;
    }
}
