package org.rickosborne.romance.client.command;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.client.AudiobookStoreService;
import org.rickosborne.romance.client.CacheClient;
import org.rickosborne.romance.client.response.BookInformation;
import org.rickosborne.romance.client.response.UserInformation2;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.sheet.SheetStore;
import org.rickosborne.romance.util.BookBot;
import org.rickosborne.romance.util.BookMerger;
import org.rickosborne.romance.util.IgnoredBooks;
import picocli.CommandLine;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.rickosborne.romance.util.BookMerger.bookLikeFilter;
import static org.rickosborne.romance.util.StringStuff.CRLF;

@Slf4j
@CommandLine.Command(
    name = "last",
    description = "Fetch and display recent purchases"
)
public class LastCommand extends ASheetCommand {
    public static final LocalDate ROMANCE_PURCHASE_START = LocalDate.of(2022, 2, 1);
    @SuppressWarnings("FieldMayBeFinal")
    @CommandLine.Option(names = "--no-nlp", description = "Disable NLP")
    private boolean noNLP = false;
    @CommandLine.Parameters(paramLabel = "SPEC", description = "Specification for how many to show")
    String spec;

    @Override
    public Integer doWithSheets() {
        final Spec effectiveSpec = Spec.fromString(spec);
        if (effectiveSpec == null) {
            throw new IllegalArgumentException("Bad spec: " + spec);
        }
        final CacheClient<AudiobookStoreService> cachingService = AudiobookStoreService.buildCaching();
        final AudiobookStoreService service = cachingService.getService();
        getTabsAuth().ensureAuthGuid(service);
        final UserInformation2 info;
        try {
            info = service.userInformation2(getTabsAuth().getAbsUserGuid().toString()).execute().body();
        } catch (IOException e) {
            log.error("Failed to get TABS userInformation2", e);
            return 1;
        }
        if (info == null) {
            throw new NullPointerException("Could not fetch user info for GUID: " + getTabsAuth().getAbsUserGuid());
        }
        final List<BookInformation> books = info.getAudiobooks();
        if (books == null || books.isEmpty()) {
            throw new NullPointerException("Missing books");
        }
        final SheetStore<BookModel> bookSheet = getSheetStoreFactory().buildSheetStore(BookModel.class);
        final BookBot bookBot = getBookBot();
        final List<BookModel> lastBooks = effectiveSpec.filterBooks(books
            .stream()
            .filter(b -> LocalDate.ofInstant(b.getPurchaseInstant(), ZoneOffset.UTC).isAfter(ROMANCE_PURCHASE_START))
            .sorted(Comparator.comparing(BookInformation::getPurchaseInstant).reversed())
            .map(BookMerger::modelFromBookInformation)
            .map(bookBot::extendWithJsonStored)
            .map(bookBot::extendWithAudiobookStoreDetails)
            .filter(IgnoredBooks::isNotIgnored)
            .filter(bookBot::isRomance)
            .filter(bi -> !bookSheet.hasMatch(bookLikeFilter(bi)))
            .collect(Collectors.toList()));
        if (lastBooks.isEmpty()) {
            System.out.println("No new books which are not already in the spreadsheet.");
            return 0;
        }
        final StringBuilder sb = new StringBuilder();
        lastBooks
            .forEach(book -> {
                BookModel model = bookBot.extendAll(book);
                if (!noNLP) {
                    model = bookBot.extendWithTextInference(model);
                }
                sb.append(DocTabbed.fromBookModel(model)).append(CRLF);
            });
//        if (isWrite()) {
//            lastBooks.forEach(bookSheet::save);
//        }
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
        public List<BookModel> filterBooks(final List<BookModel> books) {
            return books.stream()
                .sorted(Comparator.comparing(BookModel::getDatePurchase).reversed()
                    .thenComparing(BookModel::getTitle))
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

        public abstract List<BookModel> filterBooks(final List<BookModel> books);
    }

    @AllArgsConstructor
    private static class TimeSpec extends Spec {
        private static final Pattern PATTERN = Pattern.compile("^(?<num>\\d+)[dD]$");

        public static TimeSpec fromString(final String value) {
            final Matcher matcher = PATTERN.matcher(value);
            if (matcher.matches()) {
                final String days = matcher.group("num");
                final Duration duration = Duration.ofDays(Long.parseLong(days, 10));
                return new TimeSpec(LocalDate.from(Instant.now().minus(duration)));
            }
            return null;
        }

        private final LocalDate since;

        @Override
        public List<BookModel> filterBooks(final List<BookModel> books) {
            return books.stream()
                .filter(book -> since.isBefore(book.getDatePurchase()))
                .collect(Collectors.toList());
        }
    }
}
