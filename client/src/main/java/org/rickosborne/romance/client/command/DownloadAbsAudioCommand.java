package org.rickosborne.romance.client.command;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.openqa.selenium.Cookie;
import org.rickosborne.romance.AudiobookStore;
import org.rickosborne.romance.client.AudiobookStoreSuggestService;
import org.rickosborne.romance.client.html.AudiobookStoreHtml;
import org.rickosborne.romance.client.response.BookInformation;
import org.rickosborne.romance.client.response.LibraryFile;
import org.rickosborne.romance.db.model.BookAttributes;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.util.BookBot;
import org.rickosborne.romance.util.IgnoredBooks;
import org.rickosborne.romance.util.Pair;
import org.rickosborne.romance.util.SheetStuff;
import picocli.CommandLine;
import retrofit2.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.rickosborne.romance.db.model.BookModel.fileNameForBook;
import static org.rickosborne.romance.db.model.BookModel.hashKeyForBook;
import static org.rickosborne.romance.util.BookMerger.bookLikeFilter;
import static org.rickosborne.romance.util.FileStuff.recursePath;

@Slf4j
@CommandLine.Command(
    name = "download-abs-audio",
    description = "Download books from TABS"
)
public class DownloadAbsAudioCommand extends ASheetCommand {
    private static final Map<String, BookAttributes> FILE_NAME_ATTRIBUTES = Map.of(
        "author", BookAttributes.authorName,
        "title", BookAttributes.title,
        "series", BookAttributes.seriesName
    );
    private static final List<Pattern> FILE_NAME_PATTERNS = List.of(
        Pattern.compile("^(?<author>.+?)\\s+\\(.*?\\)\\s+(?<title>.+?)\\s+(?:part\\s+)?\\d+(?:\\s+of\\s+\\d+)?$"),
        Pattern.compile("^(?<author>.+?)\\s+\\(.*?\\)\\s+(?<title>.+?)\\s+\\((?<series>.+)\\)$"),
        Pattern.compile("^(?<author>.+?)\\s+\\(.*?\\)\\s+(?<title>.+?)$")
    );
    private final static Pattern MULTI_AUTHOR_PATTERN = Pattern.compile("^(.+?)(?:\\s*,\\s+|\\s+and\\s+)(.+?)$", Pattern.CASE_INSENSITIVE);
    @CommandLine.Option(names = {"--audio-path"}, description = "Path to a directory where audio already exists", required = true)
    private List<File> audioPaths;
    @CommandLine.Option(names = {"--cookie-header"}, description = "Cookie header", required = false)
    private String cookieHeader;
    @CommandLine.Option(names = {"--out-path"}, description = "Path to a directory where audio will be saved", required = true)
    private Path outPath;
    @CommandLine.Option(names = {"--purchased-after"}, description = "Purchased after", required = false)
    private Date purchasedAfter;

    @SneakyThrows
    @Override
    protected Integer doWithSheets() {
        Objects.requireNonNull(audioPaths, "Audio paths");
        Objects.requireNonNull(outPath, "Output path");
        if (audioPaths.isEmpty()) {
            throw new IllegalArgumentException("Must have at least one audio path");
        }
        if (!outPath.toFile().exists() || !outPath.toFile().isDirectory()) {
            throw new IllegalArgumentException("Out Path does not exist or is not a directory: " + outPath);
        }
        final Map<String, Pair<BookModel, File>> existing = new HashMap<>();
        audioPaths.forEach(audioPath -> {
            log.info("Scanning: {}", audioPath);
            recursePath(audioPath, file -> extractBook(file, existing), dir -> {
                extractBook(dir, existing);
                return true;
            });
        });
        log.info("Found {} existing books", existing.size());
        final BookBot bot = getBookBot();
        final Predicate<BookInformation> infoPredicate;
        if (purchasedAfter != null) {
            final Instant pAfter = purchasedAfter.toInstant();
            infoPredicate = (info) -> info.getPurchaseDate() != null && info.getPurchaseInstant().isAfter(pAfter);
        } else {
            infoPredicate = (info) -> true;
        }
        final List<Predicate<BookModel>> ignoredDownloads = IgnoredBooks.getIgnoredDownloads();
        final List<BookModel> books = bot.fetchAudiobooksWithInfoFilter(infoPredicate);
        final ArrayList<BookModel> todo = (ArrayList<BookModel>) books.stream()
            .filter(IgnoredBooks::isNotIgnored)
            .filter(book -> {
                if (existing.containsKey(hashKeyForBook(book))) {
                    log.info("Already downloaded: {}", book);
                    return false;
                }
                if (ignoredDownloads.stream().anyMatch(p -> p.test(book))) {
                    log.info("Ignored: {}", book);
                    return false;
                }
                return true;
            })
            .sorted(Comparator.comparing(BookModel::getDatePurchase))
            .collect(Collectors.toList());
        if (todo.isEmpty()) {
            log.info("Nothing to do.");
            return 0;
        }
        final List<BookModel> sheetBooks = getSheetStoreFactory()
            .buildSheetStore(BookModel.class)
            .getRecords().stream()
            .map(SheetStuff.Indexed::getModel)
            .collect(Collectors.toUnmodifiableList());
        todo
            .forEach(book -> {
                sheetBooks.stream()
                    .filter(bookLikeFilter(book))
                    .findAny()
                    .ifPresent(sheetBook -> {
                        book.setDatePublish(sheetBook.getDatePublish());
                        book.setImageUrl(sheetBook.getImageUrl());
                    });
                log.info("Need to download: {} ({}) {}", book, book.getDatePurchase(), book.getAudiobookStoreSku());
            });
        fetchBooksAudio(todo, bot);
        return 0;
    }

    protected void extractBook(
        @NonNull final File file,
        @NonNull final Map<String, Pair<BookModel, File>> books
    ) {
        final String baseName = file.getName().replaceFirst("[.][^. ]+$", "");
        if (baseName.isEmpty()) {
            return;
        }
        final List<Matcher> matchers = FILE_NAME_PATTERNS.stream().map(pattern -> pattern.matcher(baseName))
            .filter(Matcher::find).collect(Collectors.toUnmodifiableList());
        if (matchers.isEmpty()) {
            // log.warn("Did not match any patterns: {}", baseName);
            return;
        }
        final BookModel book = BookModel.build();
        matchers.stream()
            .flatMap(matcher -> FILE_NAME_ATTRIBUTES.entrySet().stream()
                .filter((entry) -> matcher.pattern().pattern().contains("?<" + entry.getKey() + ">"))
                .map(entry -> Pair.build(matcher.group(entry.getKey()), entry.getValue()))
                .filter(pair -> pair.getRight().getAttribute(book) == null))
            .forEach(pair -> {
                final BookAttributes attr = pair.getRight();
                final String value = pair.getLeft();
                attr.setAttribute(book, value);
            });
        final List<BookModel> toAdd = new LinkedList<>();
        toAdd.add(book);
        final Matcher matcher = MULTI_AUTHOR_PATTERN.matcher(book.getAuthorName());
        if (matcher.find()) {
            final String a1 = matcher.group(1);
            final String a2 = matcher.group(2);
            final BookModel b1 = book.toBuilder().authorName(a1).build();
            final BookModel b2 = book.toBuilder().authorName(a2).build();
            toAdd.add(b1);
            toAdd.add(b2);
        }
        log.info("Found {}", book);
        toAdd.forEach(b -> {
            final String hashKey = hashKeyForBook(b);
            if (!books.containsKey(hashKey)) {
                books.put(hashKey, Pair.build(b, file));
            }
        });
    }

    private void fetchBooksAudio(
        final ArrayList<BookModel> todo,
        final BookBot bot
    ) {
        while (!todo.isEmpty()) {
            log.info("{} to download", todo.size());
            final BookModel book = todo.remove(0);
            if (cookieHeader == null) {
                final AudiobookStoreHtml storeHtml = bot.getAudiobookStoreHtml();
                final AudiobookStoreAuthOptions auth = bot.getAuth();
                cookieHeader = storeHtml.withBrowser(browser -> {
                    storeHtml.headlessSignIn(browser, auth.getAbsUsername(), auth.getAbsPassword());
                    final Set<Cookie> cookies = browser.manage().getCookies();
                    return cookies.stream()
                        .filter(cookie -> cookie.getDomain() == null || cookie.getDomain().endsWith(AudiobookStore.SUGGEST_HOST))
                        .map(c -> c.getName() + "=" + URLEncoder.encode(c.getValue(), StandardCharsets.UTF_8))
                        .collect(Collectors.joining("; "));
                });
                log.info("Cookie: " + cookieHeader);
            }
            final AudiobookStoreSuggestService service = bot.getAudiobookStoreSuggestService();
            try {
                final Response<List<LibraryFile>> libraryFilesResponse = service.getMyLibraryFiles(
                    AudiobookStoreSuggestService.MyLibraryFilesMethod.GetM4bFiles.name(),
                    book.getAudiobookStoreSku(),
                    cookieHeader
                ).execute();
                if (libraryFilesResponse.isSuccessful()) {
                    final List<LibraryFile> libraryFilesList = libraryFilesResponse.body();
                    if (libraryFilesList != null && !libraryFilesList.isEmpty()) {
                        final int fileCount = libraryFilesList.size();
                        for (int i = 0; i < fileCount; i++) {
                            final LibraryFile libraryFile = libraryFilesList.get(i);
                            final String fileName = fileNameForBook(book) + (fileCount > 1 ? " " + (i + 1) : "") + ".m4b";
                            log.info("Downloading: {}", fileName);
                            final File partFile = outPath.resolve(fileName + ".part").toFile();
                            final File doneFile = outPath.resolve(fileName).toFile();
                            if (doneFile.exists()) {
                                continue;
                            }
                            try {
                                final Response<ResponseBody> download = service.downloadFile(
                                    fileName,
                                    book.getAudiobookStoreSku(),
                                    URLDecoder.decode(libraryFile.getUrl(), StandardCharsets.UTF_8),
                                    cookieHeader
                                ).execute();
                                if (download.isSuccessful()) {
                                    try (
                                        final ResponseBody body = download.body();
                                        final InputStream in = Objects.requireNonNull(body, "body").byteStream();
                                        final OutputStream out = new FileOutputStream(partFile)
                                    ) {
                                        in.transferTo(out);
                                    }
                                    if (!partFile.renameTo(doneFile)) {
                                        log.warn("Could not rename {} -> {}", partFile, doneFile);
                                    }
                                } else {
                                    log.error("Download was not successful: {}", download.message());
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    } else {
                        log.error("Did not get result from library files: {}", libraryFilesResponse.message());
                    }
                } else {
                    log.error("Did not fetch library files: {}", libraryFilesResponse.message());
                }
            } catch (IOException err) {
                log.error("Failed to fetch library files", err);
            }
        }
    }
}
