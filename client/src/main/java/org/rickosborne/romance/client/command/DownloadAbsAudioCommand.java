package org.rickosborne.romance.client.command;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.openqa.selenium.Cookie;
import org.rickosborne.romance.AudiobookStore;
import org.rickosborne.romance.client.AudiobookStoreSuggestService;
import org.rickosborne.romance.client.audiobookshelf.AudiobookShelfMetadataJson;
import org.rickosborne.romance.client.audiobookstore.AbsCredentials;
import org.rickosborne.romance.client.html.AudiobookStoreHtml;
import org.rickosborne.romance.client.response.BookInformation;
import org.rickosborne.romance.client.response.LibraryFileV2;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.util.BidirectionalMultiMap;
import org.rickosborne.romance.util.BookBot;
import org.rickosborne.romance.util.IgnoredBooks;
import org.rickosborne.romance.util.SheetStuff;
import picocli.CommandLine;
import retrofit2.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.rickosborne.romance.client.audiobookshelf.AudiobookShelfConverter.metadataFromBook;
import static org.rickosborne.romance.db.model.BookModel.fileNameForBook;
import static org.rickosborne.romance.util.BookMerger.bookLikeFilter;
import static org.rickosborne.romance.util.FileStuff.scanBookFiles;
import static org.rickosborne.romance.util.ImageDownloader.downloadImage;
import static org.rickosborne.romance.util.ImageDownloader.getExtension;

@Slf4j
@CommandLine.Command(
    name = "download-abs-audio",
    description = "Download books from TABS"
)
public class DownloadAbsAudioCommand extends ASheetCommand {
    @CommandLine.Option(names = {"--audio-path"}, description = "Path to a directory where audio already exists", required = true)
    private List<File> audioPaths;
    private String cookieHeader;
    @CommandLine.Option(names = {"--out-path"}, description = "Path to a directory where audio will be saved", required = true)
    private Path outPath;
    @CommandLine.Option(names = {"--purchased-after"}, description = "Purchased after", required = false)
    private Date purchasedAfter;
    private String requestVerificationToken;
    @CommandLine.Option(names = {"--temp-path"}, description = "Path for temp files", required = false)
    private Path tempPath = Path.of(".cache", "download");
    @CommandLine.Option(names = {"--with-sheet"}, description = "Correlate with spreadsheet", required = false)
    private boolean withSheet = false;

    @SneakyThrows
    @Override
    protected Integer doWithSheets() {
        Objects.requireNonNull(audioPaths, "Audio paths");
        Objects.requireNonNull(outPath, "Output path");
        if (audioPaths.isEmpty()) {
            throw new IllegalArgumentException("Must have at least one audio path");
        }
        if (!tempPath.toFile().exists() || !tempPath.toFile().isDirectory()) {
            throw new IllegalArgumentException("Temp Path does not exist or is not a directory: " + tempPath);
        }
        if (!outPath.toFile().exists() || !outPath.toFile().isDirectory()) {
            throw new IllegalArgumentException("Out Path does not exist or is not a directory: " + outPath);
        }
        final BidirectionalMultiMap<BookModel, File, String> bookFiles = scanBookFiles(audioPaths);
        log.info("Found {} existing books in {} files/dirs", bookFiles.leftSize(), bookFiles.rightSize());
        final BookBot bot = getBookBot();
        final AbsCredentials absCredentials = bot.getAuth().getAbsCredentials();
        this.cookieHeader = Objects.requireNonNull(absCredentials.getCookie(), "TABS Cookie from audiobookstore.json");
        this.requestVerificationToken = Objects.requireNonNull(absCredentials.getRequestVerificationToken());
        final Predicate<BookInformation> infoPredicate;
        if (purchasedAfter != null) {
            final Instant pAfter = purchasedAfter.toInstant();
            infoPredicate = (info) -> info.getPurchaseDate() != null && info.getPurchaseInstant().isAfter(pAfter);
        } else {
            infoPredicate = (info) -> true;
        }
        final List<Predicate<BookModel>> ignoredDownloads = IgnoredBooks.getIgnoredDownloads();
        final List<BookModel> books = bot.fetchAudiobooksWithInfoFilter(infoPredicate);
        final Set<String> seen = new ConcurrentSkipListSet<>();
        final ArrayList<BookModel> todo = (ArrayList<BookModel>) books.stream()
            .filter(IgnoredBooks::isNotIgnored)
            .filter(book -> {
                final String hashKey = bookFiles.leftKey(book);
                if (bookFiles.containsLeft(book)) {
                    log.info("Already downloaded: {}", book);
                    return false;
                }
                if (ignoredDownloads.stream().anyMatch(p -> p.test(book))) {
                    log.info("Ignored: {}", book);
                    return false;
                }
                if (seen.contains(hashKey)) {
                    return false;
                }
                seen.add(hashKey);
                return true;
            })
            .sorted(Comparator.comparing(BookModel::getDatePurchase))
            .collect(Collectors.toList());
        if (todo.isEmpty()) {
            log.info("Nothing to do.");
            return 0;
        }
        final List<BookModel> sheetBooks = withSheet ? getSheetStoreFactory()
            .buildSheetStore(BookModel.class)
            .getRecords().stream()
            .map(SheetStuff.Indexed::getModel)
            .collect(Collectors.toUnmodifiableList()) : null;
        final ArrayList<BookModel> readyToFetch = new ArrayList<>(todo.size());
        for (int i = 0; i < todo.size(); i++) {
            BookModel book = todo.get(i);
            final BookModel sheetBook = sheetBooks == null ? null : sheetBooks.stream()
                .filter(bookLikeFilter(book))
                .findAny()
                .orElse(null);
            if (sheetBook != null) {
                book.setDatePublish(sheetBook.getDatePublish());
                book.setImageUrl(sheetBook.getImageUrl());
            }
            book = bot.extendWithAudiobookStorePurchase(book);
            book = bot.extendWithAudiobookStoreSuggestion(book);
            book = bot.extendWithAudiobookStoreDetails(book);
            book = bot.extendWithAudiobookStoreBookInformation(book);
            log.info("Need to download: {}. {} ({}) {}", i + 1, book, book.getDatePurchase(), book.getAudiobookStoreSku());
            readyToFetch.add(book);
        }
        if (!isDryRun()) {
            fetchBooksAudio(readyToFetch, bot);
        }
        return 0;
    }

    private void fetchBooksAudio(
        final ArrayList<BookModel> todo,
        final BookBot bot
    ) {
        while (!todo.isEmpty()) {
            log.info("{} to download", todo.size());
            final BookModel book = bot.extendAll(todo.remove(0));
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
                log.info("Cookie: {}", cookieHeader);
            }
            final AudiobookStoreSuggestService service = bot.getAudiobookStoreSuggestService();
            try {
                log.info("Book: {} {}", book.toString(), book.getAudiobookStoreSku());
                final Response<List<LibraryFileV2>> libraryFilesResponse = service.getMyLibraryFiles(
                    // AudiobookStoreSuggestService.MyLibraryFilesMethod.GetM4bFiles.name(),
                    book.getAudiobookStoreSku(),
                    requestVerificationToken,
                    cookieHeader
                ).execute();
                if (libraryFilesResponse.isSuccessful()) {
                    final List<LibraryFileV2> libraryFilesList = libraryFilesResponse.body();
                    if (libraryFilesList != null && !libraryFilesList.isEmpty()) {
                        final int fileCount = libraryFilesList.size();
                        boolean gotAny = false;
                        final String dirName = fileNameForBook(book);
                        final Path doneDir = outPath.resolve(dirName);
                        final List<AudiobookShelfMetadataJson.Chapter> chapters = new LinkedList<>();
                        for (int i = 1; i <= fileCount; i++) {
                            final LibraryFileV2 libraryFile = libraryFilesList.get(i - 1);
                            final String fileName = dirName + (fileCount > 1 ? " " + i : "") + ".m4b";
                            log.info("Downloading: {}", fileName);
                            final File partFile = tempPath.resolve(fileName + ".part").toFile();
                            final File doneFile = doneDir.resolve(fileName).toFile();
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
                                    // BookTags.applyTags(partFile, book, new BookTags.Extras(fileCount, i));
                                    Files.createDirectories(doneDir);
                                    Files.move(partFile.toPath(), doneFile.toPath());
                                    gotAny = true;
                                } else {
                                    log.error("Download was not successful: {}, {}", download.code(), download.message());
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        if (gotAny) {
                            final File metadataFile = doneDir.resolve("metadata.json").toFile();
                            if (!metadataFile.exists()) {
                                final AudiobookShelfMetadataJson metadata = metadataFromBook(book);
                                bot.getObjectWriter().writeValue(metadataFile, metadata);
                            }
                        }
                        final URL imageUrl = book.getImageUrl();
                        if (imageUrl != null) {
                            final String ext = getExtension(imageUrl);
                            if (ext != null && !ext.isBlank()) {
                                final String coverFileName = "cover." + ext;
                                final File artFile = doneDir.resolve(coverFileName).toFile();
                                if (!artFile.exists()) {
                                    log.info("Downloading art: {}", imageUrl);
                                    downloadImage(imageUrl, artFile);
                                }
                            }
                        }
                    } else {
                        log.error("Did not get result from library files: {}", libraryFilesResponse.message());
                    }
                } else {
                    log.error("Did not fetch library files: {}, {}", libraryFilesResponse.code(), libraryFilesResponse.message());
                }
            } catch (IOException err) {
                log.error("Failed to fetch library files", err);
            }
        }
    }
}
