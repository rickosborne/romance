package org.rickosborne.romance.client.command;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.java.Log;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.client.AudiobookStoreService;
import org.rickosborne.romance.client.AudiobookStoreSuggestService;
import org.rickosborne.romance.client.CacheClient;
import org.rickosborne.romance.client.GoodreadsService;
import org.rickosborne.romance.client.html.AudiobookStoreHtml;
import org.rickosborne.romance.client.html.GoodreadsHtml;
import org.rickosborne.romance.client.response.AudiobookStoreSuggestion;
import org.rickosborne.romance.client.response.BookInformation;
import org.rickosborne.romance.client.response.GoodreadsAutoComplete;
import org.rickosborne.romance.client.response.UserInformation2;
import org.rickosborne.romance.db.json.JsonStore;
import org.rickosborne.romance.db.json.JsonStoreFactory;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.model.BookSchema;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.rickosborne.romance.client.command.BookMerger.modelFromAudiobookStoreSuggestion;
import static org.rickosborne.romance.client.command.BookMerger.modelFromBookInformation;
import static org.rickosborne.romance.client.command.BookMerger.modelFromGoodreadsAutoComplete;
import static org.rickosborne.romance.util.StringStuff.CRLF;

@Log
@CommandLine.Command(
    name = "last",
    description = "Fetch and display recent purchases"
)
public class LastCommand implements Callable<Integer> {
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

    @CommandLine.Parameters(paramLabel = "SPEC", description = "Specification for how many to show")
    String spec;

    @Override
    public Integer call() throws IOException {
        final Spec effectiveSpec = Spec.fromString(spec);
        if (effectiveSpec == null) {
            throw new IllegalArgumentException("Bad spec: " + spec);
        }
        final AudiobookStoreHtml audiobookStoreHtml = new AudiobookStoreHtml(cachePath, null);
        final GoodreadsHtml goodreadsHtml = new GoodreadsHtml(cachePath);
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
        final List<BookInformation> lastBooks = effectiveSpec.filterBooks(books)
            .stream().sorted(Comparator.comparing(BookInformation::getPurchaseInstant))
            .collect(Collectors.toList());
        final String userGuid = Optional.ofNullable(auth.getAbsUserGuid()).map(UUID::toString).orElse(null);
        final GoodreadsService gr = GoodreadsService.build();
        final AudiobookStoreSuggestService suggestService = AudiobookStoreSuggestService.build();
        jsonStoreFactory = new JsonStoreFactory(dbPath, namingConvention);
        final JsonStore<BookModel> bookStore = jsonStoreFactory.buildJsonStore(BookModel.class);
        final StringBuilder sb = new StringBuilder();
        lastBooks
            .forEach(book -> {
                BookModel model = modelFromBookInformation(book);
                final BookModel existing = bookStore.findLikeOrMatch(model, bm -> Objects.equals(bm.getTitle(), book.getCleanTitle()) && Objects.equals(bm.getAuthorName(), book.getAuthors()));
                model = bookSchema.mergeModels(existing, model);
                final GoodreadsAutoComplete grBook = gr.findBook(book.getCleanTitle(), book.getAuthors());
                if (grBook != null) {
                    model = bookSchema.mergeModels(model, modelFromGoodreadsAutoComplete(grBook));
                }
                if (userGuid != null) {
                    final BookInformation info2 = cachingService.fetchFomCache(new TypeReference<>() {
                    }, s -> s.bookInformation(userGuid, book.getSku()), userGuid + book.getSku());
                    model = bookSchema.mergeModels(model, modelFromBookInformation(info2));
                }
                final AudiobookStoreSuggestion suggestion = suggestService.findBookByTitle(book.getCleanTitle());
                if (suggestion != null) {
                    model = bookSchema.mergeModels(model, modelFromAudiobookStoreSuggestion(suggestion));
                }
                final URL storeUrl = Optional.ofNullable(suggestion)
                    .map(AudiobookStoreSuggestion::getUrl)
                    .orElseGet(book::getUrl);
                if (storeUrl != null) {
                    final BookModel storeBook = audiobookStoreHtml.getBookModel(storeUrl);
                    model = bookSchema.mergeModels(model, storeBook);
                }
                final URL grUrl = model.getGoodreadsUrl();
                if (grUrl != null) {
                    final BookModel grModel = goodreadsHtml.getBookModel(grUrl);
                    model = bookSchema.mergeModels(model, grModel);
                }
                bookStore.saveIfChanged(model);
                sb.append(DocTabbed.fromBookModel(model)).append(CRLF);
            });
        System.out.println(sb);
        return 0;
    }

    @AllArgsConstructor
    private static class CountSpec extends Spec {
        public static final Pattern PATTERN = Pattern.compile("^\\d+$");

        public static CountSpec fromString(@NonNull final String value) {
            if (PATTERN.matcher(value).matches()) {
                return new CountSpec(Integer.parseInt(value, 10));
            }
            return null;
        }

        private final int count;

        @Override
        public List<BookInformation> filterBooks(final List<BookInformation> books) {
            return books.stream()
                .sorted(Comparator.comparing(BookInformation::getPurchaseInstant).reversed())
                .limit(count)
                .collect(Collectors.toList());
        }
    }

    private static abstract class Spec {
        @SuppressWarnings("StaticInitializerReferencesSubClass")
        public static final List<Function<String, Spec>> BUILDERS = List.of(
            CountSpec::fromString,
            TimeSpec::fromString
        );

        public static Spec fromString(final String value) {
            for (final Function<String, Spec> builder : BUILDERS) {
                final Spec spec = builder.apply(value);
                if (spec != null) {
                    return spec;
                }
            }
            return null;
        }

        public abstract List<BookInformation> filterBooks(final List<BookInformation> books);
    }

    @AllArgsConstructor
    private static class TimeSpec extends Spec {
        private static final Pattern PATTERN = Pattern.compile("^(?<num>\\d+)[dD]$");

        public static TimeSpec fromString(final String value) {
            final Matcher matcher = PATTERN.matcher(value);
            if (matcher.matches()) {
                final String days = matcher.group("num");
                final Duration duration = Duration.ofDays(Long.parseLong(days, 10));
                return new TimeSpec(Instant.now().minus(duration));
            }
            return null;
        }

        private final Instant since;

        @Override
        public List<BookInformation> filterBooks(final List<BookInformation> books) {
            return books.stream()
                .filter(book -> since.isBefore(book.getPurchaseInstant()))
                .collect(Collectors.toList());
        }
    }
}
