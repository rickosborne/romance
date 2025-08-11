package org.rickosborne.romance.client.command;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.rickosborne.romance.client.audiobookshelf.AudiobookShelfAuthor;
import org.rickosborne.romance.client.audiobookshelf.AudiobookShelfAuthorExpanded;
import org.rickosborne.romance.client.audiobookshelf.AudiobookShelfBookMetadataMinified;
import org.rickosborne.romance.client.audiobookshelf.AudiobookShelfBookMinified;
import org.rickosborne.romance.client.audiobookshelf.AudiobookShelfLibrary;
import org.rickosborne.romance.client.audiobookshelf.AudiobookShelfLibraryItem;
import org.rickosborne.romance.client.audiobookshelf.AudiobookShelfLibraryItemMinified;
import org.rickosborne.romance.client.audiobookshelf.AudiobookShelfMediaType;
import org.rickosborne.romance.client.audiobookshelf.AudiobookShelfProgressUpdate;
import org.rickosborne.romance.client.audiobookshelf.AudiobookShelfSeriesSequence;
import org.rickosborne.romance.client.audiobookshelf.AudiobookShelfService;
import org.rickosborne.romance.db.json.JsonStore;
import org.rickosborne.romance.db.model.AuthorModel;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.util.BidirectionalMultiMap;
import org.rickosborne.romance.util.BookBot;
import org.rickosborne.romance.util.BookStuff;
import org.rickosborne.romance.util.DateStuff;
import org.rickosborne.romance.util.FFMetadata;
import org.rickosborne.romance.util.FileStuff;
import org.rickosborne.romance.util.Pair;
import org.rickosborne.romance.util.StringStuff;
import picocli.CommandLine;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.rickosborne.romance.util.BookMerger.bookLikeFilter;
import static org.rickosborne.romance.util.BookStuff.cleanAuthor;
import static org.rickosborne.romance.util.FFMetadata.chaptersFromFile;
import static org.rickosborne.romance.util.FileStuff.interpretFileParts;
import static org.rickosborne.romance.util.FileStuff.scanBookFiles;
import static org.rickosborne.romance.util.FileStuff.withoutExtensions;

@Slf4j
@CommandLine.Command(name = "fix-shelf", description = "Fix the books in an AudiobookShelf instance")
public class FixShelfCommand extends ASheetCommand {
    public static Pattern AUDIOBOOK_FILE_EXT = Pattern.compile("\\.(?:mp4|m4[ab])$");

    @CommandLine.Option(names = {"--audio-path"}, description = "Path to a directory where audio already exists", required = true)
    private List<File> audioPaths;
    private BidirectionalMultiMap<BookModel, File, String> bookFiles;
    private ChangesJson changesJson;
    @CommandLine.Option(names = "--fix-authors")
    private boolean doAuthors = false;
    @CommandLine.Option(names = "--fix-books")
    private boolean doBooks = false;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @CommandLine.Option(names = "--out-json")
    private String outFileName;

    @SneakyThrows
    @Override
    protected Integer doWithSheets() {
        if (outFileName != null) {
            changesJson = new ChangesJson();
        }
        bookFiles = scanBookFiles(audioPaths);
        final BookBot bookBot = this.getBookBot();
        final AudiobookShelfService shelf = bookBot.getShelfService();
        final List<AudiobookShelfLibrary> libraries = Objects.requireNonNull(shelf.getLibraries().execute().body()).getLibraries();
        Objects.requireNonNull(libraries, "libraries");
        final JsonStore<AuthorModel> authorStore = bookBot.getAuthorStore();
        final JsonStore<BookModel> bookStore = bookBot.getBookStore();
        final Set<String> tabsStrings = new HashSet<>();
        final List<BookModel> tabsBooks = bookBot.fetchAudiobooks().stream().filter(b -> {
            final String id = b.toString();
            if (tabsStrings.contains(id)) {
                return false;
            }
            tabsStrings.add(id);
            return true;
        }).collect(Collectors.toList());
        if (doBooks) {
            log.info("Filling out {} books", tabsBooks.size());
            int doneCount = 0;
            for (final BookModel tabsBook : tabsBooks) {
                bookBot.extendAll(tabsBook);
                doneCount++;
                if ((doneCount % 100) == 0) {
                    log.info("  Books done: {}", doneCount);
                }
            }
        }
        if (doAuthors) {
            final List<AuthorModel> tabsAuthors = tabsBooks.stream()
                .map(BookModel::getAuthorName)
                .flatMap((name) -> Stream.of(name.split("\\s*,\\s*")))
                .distinct()
                .sorted()
                .map((name) -> AuthorModel.builder().name(cleanAuthor(name)).build())
                .toList();
            log.info("Filling out {} authors", tabsAuthors.size());
            int doneCount = 0;
            for (final AuthorModel tabsAuthor : tabsAuthors) {
                authorStore.saveIfChanged(bookBot.extendAll(tabsAuthor, null));
                doneCount++;
                if ((doneCount % 100) == 0) {
                    log.info("  Authors done: {}", doneCount);
                }
            }
        }
        int authorCount = 0;
        int bookCount = 0;
        for (final AudiobookShelfLibrary library : libraries) {
            log.info("Library: {}", library.getName());
            final UUID libraryId = library.getId();
            final AudiobookShelfService.LibraryFilterData filterData = Objects.requireNonNull(shelf.getLibraryByIdWithFilterData(libraryId).execute().body()).getFilterData();
            if (filterData.getBookCount() < 1) {
                log.info("No books.");
                continue;
            }
            if (doBooks) {
                bookCount = fixBooks(shelf, libraryId, bookCount, bookStore, bookBot, tabsBooks);
            }
            if (doAuthors) {
                authorCount += fixAuthors(shelf, libraryId, authorStore, bookBot);
            }
        }
        if (changesJson != null) {
            final Path changesJsonPath = Path.of("out", outFileName + "." + DateStuff.asId() + ".json");
            objectMapper.writerFor(ChangesJson.class)
                .withDefaultPrettyPrinter()
                .writeValue(changesJsonPath.toFile(), changesJson);
            log.info("Wrote changes to JSON: {}", changesJsonPath);
        }
        log.info("Total books: {}", bookCount);
        log.info("Total authors: {}", authorCount);
        return 0;
    }

    private AuthorChanged fixAuthor(
        final @NonNull JsonStore<AuthorModel> authorStore,
        final @NonNull BookBot bookBot,
        @NonNull final AudiobookShelfAuthor author
    ) {
        boolean changed = false;
        final AudiobookShelfAuthor patch = new AudiobookShelfAuthor();
        final AuthorChanged authorChanged = new AuthorChanged();
        log.info("Author: {}", author.getName());
        final AuthorModel authorModel = AuthorModel.builder()
            .name(author.getName())
            .build();
        final AuthorModel stored = bookBot.extendAll(authorModel, null);
        if (stored != null) {
            if (stored.getBioHtml() != null && author.getDescription() == null) {
                patch.setDescription(stored.getBioHtml());
                changed = true;
            }
            if (stored.getPicUrl() != null && author.getImagePath() == null) {
                changed = true;
                authorChanged.picUrl = stored.getPicUrl();
            }
        }
        if (changed) {
            authorChanged.author = patch;
            return authorChanged;
        }
        return null;
    }

    @SneakyThrows
    private int fixAuthors(
        @NonNull final AudiobookShelfService shelf,
        @NonNull final UUID libraryId,
        @NonNull final JsonStore<AuthorModel> authorStore,
        @NonNull final BookBot bookBot
    ) {
        final List<AudiobookShelfAuthorExpanded> authors = Objects.requireNonNull(shelf.getAuthors(libraryId).execute().body()).getAuthors();
        log.info("Author count: {}", authors.size());
        for (final AudiobookShelfAuthor author : authors) {
            final AuthorChanged authorChanged = fixAuthor(authorStore, bookBot, author);
            if (authorChanged != null) {
                final UUID authorId = author.getId();
                boolean logChange = false;
                if (authorChanged.author != null && authorChanged.author.getDescription() != null) {
                    log.info("  Patching: {}", objectMapper.writeValueAsString(authorChanged.author));
                    if (changesJson == null) {
                        final Response<AudiobookShelfService.UpdateAuthorResponse> updateAuthorResponse = shelf.updateAuthorById(authorId, authorChanged.author).execute();
                        if (!updateAuthorResponse.isSuccessful()) {
                            log.error("  ❌ Patch author failed: {} {} {}", updateAuthorResponse.code(), updateAuthorResponse.message(), author.getName());
                        }
                    } else {
                        logChange = true;
                    }
                }
                if (authorChanged.picUrl != null) {
                    log.info("  + Photo: {}", authorChanged.picUrl);
                    if (changesJson != null) {
                        final Response<AudiobookShelfService.UpdateAuthorResponse> updateImageResponse = shelf.updateAuthorImageById(authorId, new AudiobookShelfService.UpdateImageRequest(authorChanged.picUrl.toString())).execute();
                        if (!updateImageResponse.isSuccessful()) {
                            log.error("  ❌ Photo update failed: {} {} {}", updateImageResponse.code(), updateImageResponse.message(), author.getName());
                        }
                    } else {
                        logChange = true;
                    }
                }
                if (logChange && changesJson != null) {
                    changesJson.authorChanges.put(authorId, authorChanged);
                }
            }
        }
        return authors.size();
    }

    @SneakyThrows
    private BookChanged fixBook(@NonNull final AudiobookShelfBookMinified book, @NonNull final JsonStore<BookModel> store, @NonNull final BookBot bookBot, final AudiobookShelfProgressUpdate progress, @NonNull final List<BookModel> tabsBooks) {
        final AudiobookShelfBookMinified fixedBook = new AudiobookShelfBookMinified();
        final AudiobookShelfBookMetadataMinified metadata = book.getMetadata();
        final AudiobookShelfBookMetadataMinified metadataPatch = new AudiobookShelfBookMetadataMinified();
        fixedBook.setMetadata(metadataPatch);
        final boolean titleChanged;
        final String fixedTitle = BookStuff.cleanTitle(metadata.getTitle());
        if (!fixedTitle.equals(metadata.getTitle())) {
            metadataPatch.setTitle(fixedTitle);
            titleChanged = true;
        } else {
            titleChanged = false;
        }
        final BookModel model = BookModel.builder().title(fixedTitle).authorName(metadata.getAuthorName()).build();
        final List<BookModel> tabsMatches = tabsBooks.stream().filter(bookLikeFilter(model)).toList();
        BookModel tabsBook;
        if (tabsMatches.isEmpty()) {
            // log.info("Not a TABS book: {}", model);
            tabsBook = null;
        } else if (tabsMatches.size() > 1) {
            // log.warn("⚠️ More than one TABS match: {}", tabsMatches.stream().map(BookModel::toString).collect(Collectors.joining("; ")));
            tabsBook = null;
        } else {
            tabsBook = tabsMatches.getFirst();
        }
        BookModel stored = null;
        if (tabsBook != null) {
            final List<BookModel> inStore = store.stream().filter(bookLikeFilter(tabsBook)).toList();
            if (inStore.isEmpty()) {
                URL tabsUrl = tabsBook.getAudiobookStoreUrl();
                if (tabsUrl == null) {
                    tabsBook = bookBot.extendWithAudiobookStoreSuggestion(tabsBook);
                }
                tabsUrl = tabsBook.getAudiobookStoreUrl();
                if (tabsUrl != null) {
                    tabsBook = bookBot.extendWithAudiobookStoreDetails(tabsBook);
                    store.save(tabsBook);
                    stored = tabsBook;
                } else {
                    log.warn("⚠️ Could not find book in store from TABS: {}", tabsBook);
                }
            } else if (inStore.size() > 1) {
                log.warn("⚠️ More than one store match found: {}", inStore.stream().map(BookModel::toString).collect(Collectors.joining("; ")));
            } else {
                stored = inStore.getFirst();
            }
        }
        if (stored == null) {
            final List<BookModel> matches = store.stream().filter(bookLikeFilter(model)).toList();
            if (matches.size() > 1) {
                final Set<String> authorNames = matches.stream().map(BookModel::getAuthorName).collect(Collectors.toSet());
                if (authorNames.size() == 1) {
                    stored = matches.getFirst();
                }
            } else if (matches.isEmpty()) {
                if (titleChanged) {
                    final BookChanged bookChanged = new BookChanged();
                    bookChanged.book = fixedBook;
                    return bookChanged;
                }
            }
        }
        // if (stored != null || tabsBook != null) {
        //     saveFFMetadata(stored == null ? tabsBook : stored);
        // }
        boolean doPatch = titleChanged;
        boolean coverChanged = false;
        boolean progressChanged = false;
        final AudiobookShelfProgressUpdate patchProgress = new AudiobookShelfProgressUpdate();
        if (stored != null) {
            if (stored != tabsBook) {
                stored = bookBot.extendWithAudiobookStoreBookInformation(stored);
                stored = bookBot.extendWithAudiobookStoreDetails(stored);
                stored = bookBot.extendWithAudiobookStorePurchase(stored);
                store.saveIfChanged(stored, false);
            }
            log.info("Match: {}", stored);
            final boolean narratorChanged = fixNarrators(stored, metadata, metadataPatch);
            final boolean seriesChanged = fixSeries(stored, metadata, metadataPatch);
            final boolean pubDateChanged = fixPubDate(stored, metadata, metadataPatch);
            final boolean isbnChanged = fixISBN(stored, metadata, metadataPatch);
            final boolean descriptionChanged = fixDescription(stored, metadata, metadataPatch);
            final boolean tagsChanged = fixTags(stored, book, fixedBook);
            coverChanged = fixCover(book, stored, fixedBook);
            doPatch = doPatch || narratorChanged || seriesChanged || pubDateChanged || isbnChanged || descriptionChanged || tagsChanged;
            progressChanged = fixProgress(stored, progress, patchProgress);
        }
        final BookChanged bookChanged = new BookChanged();
        if (doPatch) {
            final String json = objectMapper.writeValueAsString(fixedBook);
            log.info("  Patch: {}", json);
            bookChanged.book = fixedBook;
        }
        if (coverChanged) {
            bookChanged.coverUrl = stored.getImageUrl();
        }
        if (progressChanged) {
            bookChanged.progress = patchProgress;
        }
        if (bookChanged.book != null || bookChanged.coverUrl != null || bookChanged.progress != null) {
            return bookChanged;
        }
        return null;
    }

    private int fixBooks(@NonNull final AudiobookShelfService shelf, @NonNull final UUID libraryId, int bookCount, @NonNull final JsonStore<BookModel> bookStore, @NonNull final BookBot bookBot, @NonNull final List<BookModel> tabsBooks) throws IOException {
        final List<AudiobookShelfLibraryItem> items = Objects.requireNonNull(shelf.getLibraryItems(libraryId, 9999, 0, "media.metadata.title", 0, null, null, 1, null).execute().body()).getResults();
        log.info("Item count: {}", items.size());
        for (final AudiobookShelfLibraryItem item : items) {
            if (!item.getMediaType().equals(AudiobookShelfMediaType.book)) {
                continue;
            }
            final UUID libraryItemId = item.getId();
            final AudiobookShelfLibraryItemMinified expanded = Objects.requireNonNull(shelf.getItemExpanded(libraryItemId).execute().body(), "expanded");
            bookCount++;
            final AudiobookShelfBookMinified book = expanded.getMedia();
            final AudiobookShelfProgressUpdate progress = shelf.getItemProgress(libraryItemId).execute().body();
            final BookChanged bookChanged = fixBook(book, bookStore, bookBot, progress, tabsBooks);
            if (bookChanged != null && !isDryRun()) {
                boolean logChanges = false;
                if (bookChanged.book != null) {
                    log.info("  Patch book: {}", objectMapper.writeValueAsString(bookChanged.book));
                    if (changesJson == null) {
                        final Response<AudiobookShelfService.UpdateItemMediaResponse> mediaResponse = shelf.updateItemMedia(libraryItemId, bookChanged.book).execute();
                        if (!mediaResponse.isSuccessful()) {
                            log.warn("  ❌ Update failed: {} {} {}", mediaResponse.code(), mediaResponse.message(), book.loggable());
                        }
                    } else {
                        logChanges = true;
                    }
                }
                if (bookChanged.coverUrl != null) {
                    log.info("  Patch cover: {}", bookChanged.coverUrl);
                    if (changesJson == null) {
                        final Response<AudiobookShelfService.UpdateItemCoverResponse> coverResponse = shelf.updateItemCover(libraryItemId, new AudiobookShelfService.UpdateImageRequest(bookChanged.coverUrl.toString())).execute();
                        if (!coverResponse.isSuccessful()) {
                            log.warn("  ❌ Cover failed: {} {} {}", coverResponse.code(), coverResponse.message(), book.loggable());
                        }
                    } else {
                        logChanges = true;
                    }
                }
                if (bookChanged.progress != null) {
                    log.info("  Patch progress: {}", objectMapper.writeValueAsString(bookChanged.progress));
                    if (changesJson == null) {
                        final Response<Void> response = shelf.updateItemProgress(libraryItemId, bookChanged.progress).execute();
                        if (!response.isSuccessful()) {
                            log.warn("  ❌ Progress failed: {} {} {}", response.code(), response.message(), book.loggable());
                        }
                    } else {
                        logChanges = true;
                    }
                }
                if (logChanges && changesJson != null) {
                    changesJson.bookChanges.put(book.getId(), bookChanged);
                }
            }
        }
        return bookCount;
    }

    private boolean fixCover(final @NotNull AudiobookShelfBookMinified book, final BookModel stored, final AudiobookShelfBookMinified fixedBook) {
        final URL imageUrl = stored.getImageUrl();
        final String coverPath = book.getCoverPath();
        final boolean changed = imageUrl != null && (coverPath == null || coverPath.isBlank());
        if (changed) {
            log.info("  + Cover: {}", imageUrl);
        }
        return changed;
    }

    private boolean fixDescription(@NonNull final BookModel stored, final @NonNull AudiobookShelfBookMetadataMinified metadata, @NonNull final AudiobookShelfBookMetadataMinified patch) {
        final String description = Optional.ofNullable(stored.getPublisherDescription()).map(String::trim).orElse(null);
        if (description != null && !description.isBlank()) {
            final String found = metadata.getDescription();
            if (found == null || found.isBlank() || found.length() < (description.length() * 0.5)) {
                log.info("  + Description: {} bytes", description.length());
                patch.setDescription(description);
                return true;
            }
        }
        return false;
    }

    private boolean fixISBN(@NonNull final BookModel stored, @NonNull final AudiobookShelfBookMetadataMinified metadata, @NonNull final AudiobookShelfBookMetadataMinified patch) {
        final String isbn = stored.getIsbn();
        if (isbn != null && !isbn.isBlank()) {
            final String foundIsbn = metadata.getIsbn();
            if (foundIsbn == null || foundIsbn.isBlank()) {
                log.info("  + ISBN: {}", isbn);
                patch.setIsbn(isbn);
                return true;
            } else if (!isbn.equals(foundIsbn)) {
                log.warn("  ⚠️ ISBN mismatch: {} shelf vs store {}", foundIsbn, isbn);
            }
        }
        return false;
    }

    private boolean fixNarrators(@NonNull final BookModel stored, @NonNull final AudiobookShelfBookMetadataMinified metadata, @NonNull final AudiobookShelfBookMetadataMinified patch) {
        boolean changed = false;
        final List<String> narrators = Optional.ofNullable(stored.getNarratorName()).map(s -> List.of(s.split("\\s*,\\s*"))).orElseGet(Collections::emptyList);
        if (!narrators.isEmpty()) {
            if (metadata.getNarratorName() == null && narrators.size() == 1) {
                final String narratorName = narrators.getFirst();
                log.info("  Set narratorName: {}", narratorName);
                patch.setNarratorName(narratorName);
                changed = true;
            }
            final List<String> original = Optional.ofNullable(metadata.getNarrators()).orElseGet(List::of);
            final List<String> found = original
                .stream()
                .map(StringStuff::normalizeNames)
                .filter(Objects::nonNull)
                .toList();
            if (found.isEmpty()) {
                log.info("  + Narrators: {}", String.join(", ", narrators));
                patch.setNarrators(narrators);
                changed = true;
            } else if (!new HashSet<>(narrators).equals(new HashSet<>(found)) || found.size() != original.size()) {
                final List<String> patched = new ArrayList<>(found);
                patch.setNarrators(patched);
                for (final String narrator : narrators) {
                    if (!found.contains(narrator)) {
                        patched.add(narrator);
                        log.info("  + Narrator: {}", narrator);
                        changed = true;
                    }
                }
                patched.sort(String::compareTo);
            }
        }
        return changed;
    }

    private boolean fixProgress(@NonNull final BookModel stored, final AudiobookShelfProgressUpdate progress, @NonNull final AudiobookShelfProgressUpdate patch) {
        final Long dateRead = Optional.ofNullable(stored.getDateRead()).map(dr -> dr.toEpochSecond(LocalTime.NOON, ZoneOffset.UTC) * 1000).orElse(null);
        final boolean dnf = Optional.ofNullable(stored.getDnf()).orElse(false);
        if (dnf) {
            if (progress != null && progress.getHideFromContinueListening()) {
                return false;
            }
            patch.setHideFromContinueListening(true);
            patch.setProgress(0.0);
            if (dateRead != null) {
                patch.setFinishedAt(dateRead);
            }
            return true;
        }
        if (dateRead != null) {
            if (progress != null && progress.getProgress() == 1 && progress.getFinishedAt() > 946684800000L) {
                return false;
            }
            patch.setProgress(1.0);
            patch.setStartedAt(dateRead);
            patch.setFinishedAt(dateRead);
            patch.setIsFinished(true);
            return true;
        }
        return false;
    }

    private boolean fixPubDate(@NonNull final BookModel stored, @NonNull final AudiobookShelfBookMetadataMinified metadata, @NonNull final AudiobookShelfBookMetadataMinified patch) {
        boolean changed = false;
        final LocalDate datePublish = stored.getDatePublish();
        final String foundYear = metadata.getPublishedYear();
        final String publishedDate = metadata.getPublishedDate();
        if (datePublish != null) {
            final String iso = datePublish.format(DateTimeFormatter.ISO_LOCAL_DATE);
            if (!iso.equals(publishedDate)) {
                log.info("  + Publish date: {}", iso);
                patch.setPublishedDate(iso);
                changed = true;
            }
            final String year = String.valueOf(datePublish.getYear());
            if (foundYear == null || foundYear.isBlank()) {
                log.info("  + Year: {}", year);
                patch.setPublishedYear(year);
                changed = true;
            } else if (!year.equals(foundYear)) {
                log.warn("  ⚠️ Year mismatch: {} shelf vs store {}", foundYear, year);
            }
        }
        return changed;
    }

    private boolean fixSeries(@NonNull final BookModel stored, @NonNull final AudiobookShelfBookMetadataMinified metadata, @NonNull final AudiobookShelfBookMetadataMinified patch) {
        boolean changed = false;
        final String seriesName = stored.getSeriesName();
        final String seriesPart = stored.getSeriesPart();
        if (seriesName != null) {
            final String foundName = metadata.getSeriesName();
            final String formattedName = seriesName + (seriesPart == null ? "" : (" #" + seriesPart));
            if ((foundName == null || foundName.isBlank())) {
                if (seriesPart != null && !seriesPart.isBlank()) {
                    log.info("  + Series name: {}", formattedName);
                    patch.setSeriesName(formattedName);
                    changed = true;
                }
            } else if (!foundName.equals(formattedName)) {
                log.warn("  ⚠️ Series mismatch: {} shelf vs store {}", foundName, seriesName);
            }
            if (seriesPart != null) {
                final List<AudiobookShelfSeriesSequence> foundSeries = metadata.getSeries();
                if (foundSeries == null || foundSeries.isEmpty()) {
                    log.info("  + Series 1: {}", formattedName);
                    patch.setSeries(List.of(new AudiobookShelfSeriesSequence(seriesName, seriesName, seriesName, seriesPart)));
                    changed = true;
                } else {
                    final List<AudiobookShelfSeriesSequence> patchSeries = new ArrayList<>(foundSeries);
                    final AudiobookShelfSeriesSequence foundSequence = patchSeries.stream().filter(s -> seriesName.equals(s.getName()) || seriesName.equals(s.getDisplayName())).findAny().orElse(null);
                    if (foundSequence == null) {
                        log.info("  + Series: {}", formattedName);
                        patch.setSeries(patchSeries);
                        patchSeries.add(new AudiobookShelfSeriesSequence(seriesName, seriesName, seriesName, seriesPart));
                        changed = true;
                    } else if (!seriesPart.equals(foundSequence.getSequence())) {
                        log.warn("  ⚠️ Series part mismatch: {} shelf vs store {}", foundSequence.getSequence(), seriesPart);
                    }
                }
            }
        }
        return changed;
    }

    private boolean fixTags(@NonNull final BookModel stored, @NonNull final AudiobookShelfBookMinified book, @NonNull final AudiobookShelfBookMinified fixedBook) {
        final Set<String> tags = stored.getTags();
        if (tags != null && !tags.isEmpty()) {
            final Set<String> found = new HashSet<>(book.getTags());
            if (tags.equals(found)) {
                return false;
            }
            log.info("  + Tags: {}", String.join(" ", tags));
            fixedBook.setTags(new ArrayList<>(tags));
            return true;
        }
        return false;
    }

    @SneakyThrows
    private void saveFFMetadata(final BookModel book) {
        final List<File> files = bookFiles.rightsFor(book)
            .stream().filter(File::isFile).toList();
        if (!files.isEmpty()) {
            final Map<File, FileStuff.TrackAndCount> fileTracks = files.stream()
                .map(f -> Pair.build(f, interpretFileParts(f.getName(), book)))
                .filter(Pair::hasRight)
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
            Integer trackCount = null;
            final Set<Integer> trackCounts = fileTracks.values().stream().map(FileStuff.TrackAndCount::getCount)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            if (trackCounts.size() == 1) {
                trackCount = trackCounts.iterator().next();
            }
            if (trackCount == null) {
                trackCount = fileTracks.values().stream().map(FileStuff.TrackAndCount::getTrack)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo).orElse(null);
            }
            if (trackCount == null) {
                trackCount = files.size();
            }
            for (FileStuff.TrackAndCount ft : fileTracks.values()) {
                if (ft.getCount() == null) {
                    ft.setCount(trackCount);
                }
            }
            for (final File audioFile : files) {
                final String baseName = withoutExtensions(audioFile.getName());
                final Path metaPath = audioFile.toPath().getParent()
                    .resolve(baseName + ".ffmetadata.txt");
                final File metaFile = metaPath.toFile();
                final FileStuff.TrackAndCount trackAndCount = fileTracks.get(audioFile);
                String metaText = FFMetadata.fromBookModel(book, trackAndCount);
                final String chapters = chaptersFromFile(audioFile);
                if (chapters != null) {
                    metaText += chapters;
                }
                if (metaFile.exists() && metaFile.isFile() && metaFile.canRead()) {
                    final String existingText = Files.readString(metaPath);
                    if (existingText.equals(metaText)) {
                        continue;
                    }
                }
                Files.writeString(metaPath, metaText);
            }
        }
    }

    @Data
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
    private static class AuthorChanged {
        AudiobookShelfAuthor author;
        URL picUrl;
    }

    @Data
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
    private static class BookChanged {
        AudiobookShelfBookMinified book;
        URL coverUrl;
        AudiobookShelfProgressUpdate progress;
    }

    @Data
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
    private static class ChangesJson {
        final Map<UUID, AuthorChanged> authorChanges = new HashMap<>();
        final Map<UUID, BookChanged> bookChanges = new HashMap<>();
    }
}
