package org.rickosborne.romance.client.command;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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
    name = "preorders",
    description = "Fetch a list of preorders from AudiobookStore"
)
@Slf4j
public class PreordersCommand implements Callable<Integer> {
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
        final List<BookModel> preorders = storeHtml.withBrowser(browser -> {
            storeHtml.headlessSignIn(browser, absUsername, absPassword);
            return storeHtml.getPreorders(browser);
        });
        if (preorders.isEmpty()) {
            System.out.println("(no preorders)");
            return 0;
        }
        final JsonStoreFactory jsonStoreFactory = new JsonStoreFactory(dbPath, namingConvention);
        final JsonStore<BookModel> bookStore = jsonStoreFactory.buildJsonStore(BookModel.class);
        for (final BookModel preorder : preorders) {
            final BookModel existing = bookStore.findLike(preorder);
            final BookModel book = bookSchema.mergeModels(existing, preorder);
            System.out.println(DocTabbed.fromBookModel(book));
            bookStore.saveIfChanged(book);
        }
        return 0;
    }
}
