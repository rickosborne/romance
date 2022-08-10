package org.rickosborne.romance.client.command;

import lombok.Getter;
import lombok.extern.java.Log;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.client.JsonCookieStore;
import org.rickosborne.romance.client.html.AudiobookStoreHtml;
import org.rickosborne.romance.db.json.JsonStore;
import org.rickosborne.romance.db.json.JsonStoreFactory;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.model.BookSchema;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

@CommandLine.Command(
    name = "wishlist",
    description = "Fetch your wishlist from AudiobookStore"
)
@Log
public class WishlistCommand implements Callable<Integer> {
    @CommandLine.Mixin
    AudiobookStoreAuthOptions auth;
    private final BookSchema bookSchema = new BookSchema();

    @SuppressWarnings("unused")
    @Getter
    @CommandLine.Option(names = {"--cache", "-c"}, description = "Path to cache dir", defaultValue = ".cache/html")
    private Path cachePath;

    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--cookies", "-s"}, description = "Path to cookie store", defaultValue = "./.credentials/abs-cookies.json")
    private Path cookieStorePath;

    @SuppressWarnings("unused")
    @Getter
    @CommandLine.Option(names = {"--path", "-p"}, description = "Path to DB dir", defaultValue = "book-data")
    private Path dbPath;

    private final NamingConvention namingConvention = new NamingConvention();

    @Override
    public Integer call() throws Exception {
        if (cookieStorePath == null || !cookieStorePath.toFile().isFile()) {
            throw new IllegalArgumentException("Invalid cookie store path");
        }
        final String absUsername = auth.getAbsUsername();
        final String absPassword = auth.getAbsPassword();
        Objects.requireNonNull(absUsername, "TABS username is required.");
        Objects.requireNonNull(absPassword, "TABS password is required");
        final JsonCookieStore cookieStore = JsonCookieStore.fromPath(cookieStorePath);
        final AudiobookStoreHtml storeHtml = new AudiobookStoreHtml(cachePath, cookieStore);
        final List<BookModel> wishlist = storeHtml.withBrowser(browser -> {
            storeHtml.headlessSignIn(browser, absUsername, absPassword);
            return storeHtml.getWishlist(browser);
        });
        if (wishlist.isEmpty()) {
            System.out.println("(no wishlist)");
            return 0;
        }
        for (final BookModel book : wishlist) {
            final DocTabbed docTabbed = DocTabbed.fromBookModel(book);
            System.out.println(docTabbed.toString());
        }
        return 0;
    }
}
