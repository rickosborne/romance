package org.rickosborne.romance.client.cli;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.java.Log;
import org.rickosborne.romance.client.client.AudiobookStoreService;
import org.rickosborne.romance.client.client.AudiobookStoreSuggestService;
import org.rickosborne.romance.client.client.CacheClient;
import org.rickosborne.romance.client.client.GoodreadsService;
import org.rickosborne.romance.client.client.response.AudiobookStoreSuggestion;
import org.rickosborne.romance.client.client.response.BookInformation;
import org.rickosborne.romance.client.client.response.GoodreadsAutoComplete;
import org.rickosborne.romance.client.client.response.UserInformation2;
import picocli.CommandLine;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Log
@CommandLine.Command(
    name = "last",
    description = "Fetch and display recent purchases"
)
public class LastCommand implements Callable<Integer> {
    @CommandLine.Mixin
    AudiobookStoreAuthOptions auth;
    @CommandLine.Parameters(paramLabel = "SPEC", description = "Specification for how many to show")
    String spec;

    @Override
    public Integer call() throws IOException {
        final Spec effectiveSpec = Spec.fromString(spec);
        if (effectiveSpec == null) {
            throw new IllegalArgumentException("Bad spec: " + spec);
        }
        final CacheClient<AudiobookStoreService> cachingService = AudiobookStoreService.buildCaching();
        final AudiobookStoreService service = cachingService.getService();
        auth.ensureAuthGuid(service);
        System.out.println("User GUID: " + auth.getAbsUserGuid());
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
        lastBooks
            .forEach(book -> {
                DocTabbed docTabbed = DocTabbed.fromBookInformation(book);
                final GoodreadsAutoComplete grBook = gr.findBook(book.getCleanTitle(), book.getAuthors());
                if (grBook != null) {
                    docTabbed = docTabbed.merge(DocTabbed.fromGoodreadsAutoComplete(grBook));
                }
                if (userGuid != null) {
                    final BookInformation info2 = cachingService.fetchFomCache(new TypeReference<>() {
                    }, s -> s.bookInformation(userGuid, book.getSku()), userGuid + book.getSku());
                    docTabbed = DocTabbed.fromBookInformation(info2).merge(docTabbed);
                }
                final AudiobookStoreSuggestion suggestion = suggestService.findBookByTitle(book.getCleanTitle());
                if (suggestion != null) {
                    docTabbed = docTabbed.merge(DocTabbed.fromAudiobookStoreSuggestion(suggestion));
                }
                System.out.println(docTabbed);
            });
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
