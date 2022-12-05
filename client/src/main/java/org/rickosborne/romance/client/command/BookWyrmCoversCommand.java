package org.rickosborne.romance.client.command;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.rickosborne.romance.client.BookWyrmService;
import org.rickosborne.romance.client.bookwyrm.BookWyrmConfig;
import org.rickosborne.romance.client.bookwyrm.Shelf;
import org.rickosborne.romance.client.response.BookWyrmBook;
import org.rickosborne.romance.db.json.JsonStore;
import org.rickosborne.romance.db.model.AuthorModel;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.model.BookSchema;
import org.rickosborne.romance.db.postgresql.BookWyrmPGAuthorStore;
import org.rickosborne.romance.db.postgresql.BookWyrmPGBookStore;
import org.rickosborne.romance.db.sheet.SheetStore;
import org.rickosborne.romance.util.BookRating;
import org.rickosborne.romance.util.Pair;
import org.rickosborne.romance.util.Triple;
import picocli.CommandLine;
import retrofit2.Response;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.rickosborne.romance.client.bookwyrm.BookWyrm.BOOK_SUBTITLE_DELIMITER;
import static org.rickosborne.romance.client.bookwyrm.BookWyrm.bookDbIdFromUrl;
import static org.rickosborne.romance.util.BookMerger.bookLikeFilter;

@Slf4j
@CommandLine.Command(
    name = "bw-covers",
    description = "Add cover art to BookWyrm"
)
public class BookWyrmCoversCommand extends ASheetCommand {
    public static final Pattern GOODREADS_KEY = Pattern.compile("^https://(?:www\\.)?goodreads\\.com/book/show/(?<key>[^-]+)");

    private static void tryToLogResponseFail(final Response<Void> response) throws IOException {
        try (final ResponseBody responseBody = response.errorBody()) {
            if (responseBody != null) {
                log.error(responseBody.string());
            }
        }
        throw new RuntimeException("Failed: " + response.code() + " " + response.message() + "\n");
    }

    private final BookSchema bookSchema = new BookSchema();
    private final BookWyrmConfig bookWyrmConfig = BookWyrmConfig.getInstance();
    private final BookWyrmService bookWyrmService = BookWyrmService.build(bookWyrmConfig);
    private final BookWyrmService bookWyrmServiceForHtml = BookWyrmService.buildForHtml(bookWyrmConfig);

    protected void addBook(
        @NonNull final BookModel book,
        @NonNull final BookWyrmPGAuthorStore pgAuthorStore,
        @NonNull final BookWyrmPGBookStore pgBookStore
    ) throws IOException {
        final String publisherName = book.getPublisherName();
        final LocalDate datePublish = book.getDatePublish();
        final List<String> authorNames = new LinkedList<>(Optional.ofNullable(book.getAuthorName()).map(a -> a.split(",\\s+")).map(List::of).orElseGet(List::of));
        final List<String> narratorNames = new LinkedList<>(Optional.ofNullable(book.getNarratorName()).map(n -> n.split(",\\s+")).map(List::of).orElseGet(List::of));
        authorNames.addAll(narratorNames);
        final List<Triple<String, AuthorModel, Integer>> authorModels = authorNames.stream().map(name -> Triple.build(name, pgAuthorStore.findByName(name))).collect(Collectors.toList());
        final Map<String, String> authorMatch = new HashMap<>();
        for (int i = 0; i < authorModels.size(); i++) {
            final Triple<String, AuthorModel, Integer> triple = authorModels.get(i);
            final String name = triple.getLeft();
            final Integer dbId = triple.getRight();
            authorMatch.put("author_match-" + i, dbId == null ? name : String.valueOf(dbId));
        }
        Integer bookId = null;
        String title = book.getTitle();
        String subTitle = null;
        final String[] titleParts = title.split(BOOK_SUBTITLE_DELIMITER);
        if (titleParts.length == 2) {
            title = titleParts[0];
            subTitle = titleParts[1];
        }
        final Response<BookWyrmBook> response = bookWyrmService.createBook(
            bookWyrmConfig.getUserId(),
            0,
            title,
            subTitle,
            book.getPublisherDescription(),
            book.getSeriesName(),
            book.getSeriesPart(),
            "English",
            subjectsOf(book),
            publisherName == null || publisherName.isBlank() ? null : List.of(publisherName),
            datePublish == null ? null : datePublish.getYear(),
            datePublish == null ? null : datePublish.getMonthValue(),
            datePublish == null ? null : datePublish.getDayOfMonth(),
            authorModels.stream().map(t -> {
                final AuthorModel author = t.getMiddle();
                if (author != null && author.getName() != null) {
                    return author.getName();
                }
                return t.getLeft();
            }).collect(Collectors.toList()),
            book.getImageUrl(),
            book.getAudiobookStoreUrl() == null ? null : BookWyrmService.PhysicalFormat.AudiobookFormat,
            book.getDurationText(),
            book.getPages(),
            book.getIsbn(),
            null,
            null,
            null,
            null,
            null,
            authorModels.size(),
            authorMatch
        ).execute();
        if (response.code() == 302) {
            bookId = bookDbIdFromUrl(response.headers().get("Location"));
        } else if (response.isSuccessful()) {
            final BookWyrmBook created = response.body();
            bookId = created == null ? null : created.getDbId();
        } else {
            log.warn("Expected a success or book redirect: {} {}", response.code(), response.message());
            throw new IllegalStateException("Could not create book");
        }
        if (bookId != null) {
            pgBookStore.getIdCache().forceId(pgBookStore.idFromModel(book), bookId);
            final BookModel pgBook = pgBookStore.findByDbId(bookId);
            fixGoodreadsId(pgBook, book, List.of(bookId), pgBookStore);
            fixTabsUrl(pgBook, book, bookId);
            fixRating(bookId, book.getRatings().get(BookRating.Overall), pgBook, pgBookStore);
        }
    }

    @Override
    protected Integer doWithSheets() {
        final SheetStore<BookModel> sheetBookStore = getSheetStoreFactory().buildSheetStore(BookModel.class);
        final JsonStore<BookModel> jsonStore = getJsonStoreFactory().buildJsonStore(BookModel.class);
        try (
            final BookWyrmPGBookStore pgBookStore = new BookWyrmPGBookStore(bookWyrmConfig);
            final BookWyrmPGAuthorStore pgAuthorStore = new BookWyrmPGAuthorStore(bookWyrmConfig)
        ) {
            for (final BookModel sheetBook : sheetBookStore) {
                log.debug(sheetBook.toString());
                final String title = sheetBook.getTitle();
                final String authorName = sheetBook.getAuthorName();
                if (title == null || title.isBlank() || authorName == null || authorName.isBlank()) {
                    continue;
                }
                final BookModel jsonBook = jsonStore.findLikeOrMatch(sheetBook, bookLikeFilter(sheetBook));
                final Pair<BookModel, Integer> pgPair = pgBookStore.findLikeWithDbId(sheetBook, bookWyrmConfig.getUserId());
                BookModel pgBook = pgPair == null ? null : pgPair.getLeft();
                if (pgBook == null) {
                    log.info("Could not find: {}", sheetBook);
                    addBook(bookSchema.mergeModels(jsonBook, sheetBook), pgAuthorStore, pgBookStore);
                    continue;
                }
                final int bookId = pgPair.getRight();
                final List<Integer> bookIds = pgBookStore.allIdsFor(bookId);
                fixShelf(bookId, sheetBook, shelfForBook(sheetBook), pgBookStore);
                fixRating(bookId, sheetBook.getRatings().get(BookRating.Overall), pgBook, pgBookStore);
                // fixImage(pgBook, sheetBook, bookId);
                // fixTabsUrl(pgBook, sheetBook, bookId);
                // fixGoodreadsId(pgBook, sheetBook, bookIds, pgBookStore);
                // if (jsonBook != null) {
                //     fixDescription(pgBook, jsonBook, bookIds, pgBookStore);
                // }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    protected void fixDescription(
        final BookModel pgBook,
        final BookModel jsonBook,
        final List<Integer> bookIds,
        final BookWyrmPGBookStore pgBookStore
    ) {
        final String description = jsonBook.getPublisherDescription();
        if (description == null || description.isBlank() || pgBook.getPublisherDescription() != null) {
            return;
        }
        pgBookStore.updateDescription(description, bookIds);
        log.info("Fixed description: {}", jsonBook);
    }

    protected void fixGoodreadsId(
        final BookModel pgBook,
        final BookModel sheetBook,
        final List<Integer> bookIds,
        final BookWyrmPGBookStore pgBookStore
    ) {
        final URL goodreadsUrl = sheetBook.getGoodreadsUrl();
        if (goodreadsUrl == null || pgBook.getGoodreadsUrl() != null) {
            return;
        }
        final Matcher matcher = GOODREADS_KEY.matcher(goodreadsUrl.toString());
        if (!matcher.find()) {
            log.warn("Odd-looking GR URL for {}: {}", sheetBook, goodreadsUrl);
            return;
        }
        final String grKey = matcher.group("key");
        pgBookStore.updateGoodreadsKey(grKey, bookIds);
        log.info("Fixed GR key: {}", sheetBook);
    }

    protected void fixImage(
        final BookModel pgBook,
        final BookModel sheetBook,
        final int bookId
    ) throws IOException {
        final URL imageUrl = sheetBook.getImageUrl();
        if (pgBook.getImageUrl() != null || imageUrl == null) {
            return;
        }
        final Response<Void> response = bookWyrmService.updateCoverFromUrl(
            bookId,
            imageUrl
        ).execute();
        if (response.isSuccessful()) {
            log.info("Fixed cover: {}", sheetBook);
        } else if (response.code() == 404) {
            log.info("404 for " + sheetBook);
        } else {
            tryToLogResponseFail(response);
        }
    }

    private void fixRating(
        final int bookId,
        final Double rating,
        @NonNull final BookModel book,
        @NonNull final BookWyrmPGBookStore pgBookStore
    ) throws IOException {
        final Double existing = book.getRatings().get(BookRating.Overall);
        if (existing != null || rating == null) {
            return;
        }
        final Response<ResponseBody> response = bookWyrmServiceForHtml.postRating(bookWyrmConfig.getUserId(), bookId, BookWyrmService.Privacy.Unlisted, rating).execute();
        if (!response.isSuccessful()) {
            log.error("Funky postRating response: {} {} / {}", response.code(), response.message(), book);
        } else {
            log.info("Fixed rating: {}⭐️ for {}", rating, book);
        }
    }

    private void fixShelf(
        final int bookId,
        @NonNull final BookModel book,
        @NonNull final Shelf shelf,
        @NonNull final BookWyrmPGBookStore pgBookStore
    ) throws IOException {
        final Shelf existing = pgBookStore.findBookShelf(bookId, bookWyrmConfig.getUserId());
        if (existing != null) {
            return;
        }
        final Response<ResponseBody> response = bookWyrmServiceForHtml.shelveBook(bookId, shelf.getIdentifier()).execute();
        if (!response.isSuccessful()) {
            log.error("Tried to shelve, got a funky response: {} {}", response.code(), response.message());
        } else {
            log.debug("Shelved: {} / {}", shelf.getName(), book);
        }
    }

    protected void fixTabsUrl(
        final BookModel pgBook,
        final BookModel sheetBook,
        final int bookId
    ) throws IOException {
        final URL audiobookStoreUrl = sheetBook.getAudiobookStoreUrl();
        if (pgBook.getAudiobookStoreUrl() != null || audiobookStoreUrl == null) {
            return;
        }
        final Response<Void> response = bookWyrmService.addFileLink(
            bookId,
            String.valueOf(bookId),
            audiobookStoreUrl,
            "audio",
            "purchase",
            String.valueOf(bookWyrmConfig.getUserId())
        ).execute();
        if (response.isSuccessful()) {
            log.info("Added TABS link: {}", sheetBook);
        } else if (response.code() == 404) {
            log.info("Link 404: {}", sheetBook);
        } else {
            tryToLogResponseFail(response);
        }
    }

    private Shelf shelfForBook(final BookModel book) {
        if (Boolean.TRUE.equals(book.dnf)) {
            return bookWyrmConfig.getDnfShelf();
        } else if (book.getDateRead() != null) {
            return bookWyrmConfig.getReadShelf();
        } else if (Boolean.TRUE.equals(book.getReading())) {
            return bookWyrmConfig.getReadingShelf();
        }
        return bookWyrmConfig.getOtherShelf();
    }

    protected List<String> subjectsOf(final BookModel book) {
        final List<String> subjects = new LinkedList<>();
        final String genre = Optional.ofNullable(book.getGenre()).orElse("");
        if (!genre.contains("not a romance")) {
            subjects.add("Romance");
        }
        if (genre.contains("contemporary")) {
            subjects.add("Contemporary");
        }
        final BookModel.MainChar mc1 = book.getMc1();
        final BookModel.MainChar mc2 = book.getMc2();
        final String g1 = mc1.getGender();
        final String g2 = mc2.getGender();
        if (!(("F".equals(g1) && "M".equals(g2)) || ("M".equals(g1) && "F".equals(g2)))) {
            subjects.add("LGBTQIA+");
        }
        return subjects;
    }
}
