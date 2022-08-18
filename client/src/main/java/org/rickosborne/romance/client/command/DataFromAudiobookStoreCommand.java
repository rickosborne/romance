package org.rickosborne.romance.client.command;

import lombok.Getter;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.client.AudiobookStoreService;
import org.rickosborne.romance.client.CacheClient;
import org.rickosborne.romance.client.html.AudiobookStoreHtml;
import org.rickosborne.romance.client.response.BookInformation;
import org.rickosborne.romance.client.response.UserInformation2;
import org.rickosborne.romance.db.json.JsonStore;
import org.rickosborne.romance.db.json.JsonStoreFactory;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.model.BookSchema;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.rickosborne.romance.util.BookMerger.modelFromBookInformation;

@CommandLine.Command(
    name = "data-from-abs",
    description = "Fetch all available data from the ABS Library"
)
public class DataFromAudiobookStoreCommand implements Callable<Integer> {
    @CommandLine.Mixin
    AudiobookStoreAuthOptions auth;

    private final BookSchema bookSchema = new BookSchema();

    @SuppressWarnings("unused")
    @Getter
    @CommandLine.Option(names = {"--cache", "-c"}, description = "Path to cache dir", defaultValue = ".cache/html")
    private Path cachePath;

    @SuppressWarnings("unused")
    @Getter
    @CommandLine.Option(names = {"--path", "-p"}, description = "Path to DB dir", defaultValue = "book-data")
    private Path dbPath;

    private JsonStoreFactory jsonStoreFactory;
    private final NamingConvention namingConvention = new NamingConvention();

    @Override
    public Integer call() throws Exception {
        final AudiobookStoreHtml audiobookStoreHtml = new AudiobookStoreHtml(cachePath, null);
        final CacheClient<AudiobookStoreService> cachingService = AudiobookStoreService.buildCaching();
        final AudiobookStoreService service = cachingService.getService();
        auth.ensureAuthGuid(service);
        final UserInformation2 info = service.userInformation2(auth.getAbsUserGuid().toString()).execute().body();
        if (info == null) {
            throw new NullPointerException("Could not fetch user info for GUID: " + auth.getAbsUserGuid());
        }
        final List<BookInformation> books = info.getAudiobooks();
        if (books == null || books.isEmpty()) {
            throw new NullPointerException("Missing books");
        }
        final String userGuid = Optional.ofNullable(auth.getAbsUserGuid()).map(UUID::toString).orElse(null);
        jsonStoreFactory = new JsonStoreFactory(dbPath, namingConvention);
        final JsonStore<BookModel> bookStore = jsonStoreFactory.buildJsonStore(BookModel.class);
        for (final BookInformation book : books) {
            BookModel model = modelFromBookInformation(book);
            final BookModel existing = bookStore.findLikeOrMatch(model, bm -> Objects.equals(bm.getTitle(), book.getCleanTitle()) && Objects.equals(bm.getAuthorName(), book.getAuthors()));
            if (existing == null) {
                continue;
            }
            model = bookSchema.mergeModels(existing, model);
            bookStore.saveIfChanged(model);
        }
        return 0;
    }
}
