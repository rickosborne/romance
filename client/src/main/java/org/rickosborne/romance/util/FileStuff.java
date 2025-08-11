package org.rickosborne.romance.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.rickosborne.romance.db.model.BookAttributes;
import org.rickosborne.romance.db.model.BookModel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.rickosborne.romance.db.model.BookModel.hashKeyForBook;

@Slf4j
public class FileStuff {
    public static final Map<String, BookAttributes> FILE_NAME_ATTRIBUTES = Map.of(
        "author", BookAttributes.authorName,
        "title", BookAttributes.title,
        "series", BookAttributes.seriesName
    );
    public static final List<Pattern> FILE_NAME_PATTERNS = List.of(
        Pattern.compile("^(?<author>.+?)\\s+\\(.*?\\)\\s+(?<title>.+?)\\s+(?:part\\s+)?\\d+(?:\\s+of\\s+\\d+)?$"),
        Pattern.compile("^(?<author>.+?)\\s+\\(.*?\\)\\s+(?<title>.+?)\\s+\\((?<series>.+)\\)$"),
        Pattern.compile("^(?<author>.+?)\\s+\\(.*?\\)\\s+(?<title>.+?)$")
    );
    public final static Pattern MULTI_AUTHOR_PATTERN = Pattern.compile("^(.+?)(?:\\s*,\\s+|\\s+and\\s+)(.+?)$", Pattern.CASE_INSENSITIVE);
    public static final List<Pattern> PART_PATTERNS = List.of(
        Pattern.compile("(?i)\\bpart (?<track>\\d) of (?<count>\\d)"),
        Pattern.compile("(?i)\\[(?<track>\\d) of (?<count>\\d)]"),
        Pattern.compile("(?i)\\bpart (?<track>\\d)"),
        Pattern.compile(" (?<track>\\d)$")
    );
    public static final List<Pattern> TITLE_BLOCKLIST = List.of(
        Pattern.compile("\\s+\\[File \\d+ of \\d+]")
    );

    public static List<BookModel> bookFromFile(
        @NonNull final File file
    ) {
        final String baseName = file.getName().replaceFirst("[.][^. ]+$", "");
        if (baseName.isEmpty()) {
            return Collections.emptyList();
        }
        final List<Matcher> matchers = FileStuff.FILE_NAME_PATTERNS.stream().map(pattern -> pattern.matcher(baseName))
            .filter(Matcher::find).toList();
        if (matchers.isEmpty()) {
            // log.warn("Did not match any patterns: {}", baseName);
            return Collections.emptyList();
        }
        final Set<String> hashes = new HashSet<>();
        final List<BookModel> books = new LinkedList<>();
        for (final Matcher matcher : matchers) {
            final BookModel book = BookModel.build();
            final Map<String, Integer> namedGroups = matcher.namedGroups();
            for (final Map.Entry<String, BookAttributes> entry : FileStuff.FILE_NAME_ATTRIBUTES.entrySet()) {
                if (namedGroups.containsKey(entry.getKey())) {
                    final Pair<String, BookAttributes> pair = Pair.build(matcher.group(entry.getKey()), entry.getValue());
                    if (pair.getRight().getAttribute(book) == null) {
                        final BookAttributes attr = pair.getRight();
                        final String value = pair.getLeft();
                        attr.setAttribute(book, value);
                    }
                }
            }
            String title = book.getTitle();
            if (title == null || title.isBlank()) {
                continue;
            }
            for (final Pattern pattern : FileStuff.TITLE_BLOCKLIST) {
                title = pattern.matcher(title).replaceAll("").strip();
            }
            if (title.isBlank()) {
                continue;
            }
            book.setTitle(title);
            final List<BookModel> toAdd = new LinkedList<>();
            toAdd.add(book);
            final Matcher mapMatcher = FileStuff.MULTI_AUTHOR_PATTERN.matcher(book.getAuthorName());
            if (mapMatcher.find()) {
                final String a1 = mapMatcher.group(1);
                final String a2 = mapMatcher.group(2);
                final BookModel b1 = book.toBuilder().authorName(a1).build();
                final BookModel b2 = book.toBuilder().authorName(a2).build();
                toAdd.add(b1);
                toAdd.add(b2);
            }
            log.info("Found {}", book);
            toAdd.forEach(b -> {
                final String hashKey = hashKeyForBook(b);
                if (!hashes.contains(hashKey)) {
                    books.add(b);
                    hashes.add(hashKey);
                }
            });
        }
        return books;
    }

    public static TrackAndCount interpretFileParts(@NonNull final String fileName, final BookModel book) {
        final String name = trackPartOfFileName(fileName, book);
        for (final Pattern pattern : PART_PATTERNS) {
            final Matcher matcher = pattern.matcher(name);
            if (matcher.find()) {
                final Map<String, Integer> namedGroups = matcher.namedGroups();
                final String track = namedGroups.containsKey("track") ? matcher.group("track") : null;
                final String count = namedGroups.containsKey("count") ? matcher.group("count") : null;
                if (track != null || count != null) {
                    return new TrackAndCount(
                        count == null ? null : Integer.parseInt(count, 10),
                        track == null ? null : Integer.parseInt(track, 10)
                    );
                }
            }
        }
        return null;
    }

    public static void recursePath(
        @NonNull File startPath,
        @NonNull Consumer<File> onFile,
        @NonNull Function<File, Boolean> onDirectory
    ) {
        if (!startPath.exists() || !startPath.isDirectory()) {
            throw new IllegalArgumentException("Not a directory: " + startPath);
        }
        final File[] all = startPath.listFiles(f -> (f.isFile() || f.isDirectory()) && !f.getName().startsWith("."));
        if (all == null || all.length == 0) {
            return;
        }
        final List<File> directories = new LinkedList<>();
        Arrays.stream(all).forEach(file -> {
            if (file.isFile()) {
                onFile.accept(file);
            } else if (file.isDirectory()) {
                if (onDirectory.apply(file)) {
                    directories.add(file);
                }
            }
        });
        directories.forEach(dir -> recursePath(dir, onFile, onDirectory));
    }

    public static BidirectionalMultiMap<BookModel, File, String> scanBookFiles(@NonNull final Collection<File> audioPaths) {
        final BidirectionalMultiMap<BookModel, File, String> bookFiles = new BidirectionalMultiMap<>(
            BookModel::hashKeyForBook,
            File::getPath
        );
        audioPaths.forEach(audioPath -> {
            log.info("Scanning: {}", audioPath);
            recursePath(
                audioPath,
                file -> {
                    final String name = file.getName();
                    if (name.endsWith(".m4a") || name.endsWith(".m4b") || name.endsWith(".mp3")) {
                        bookFromFile(file).forEach(b -> bookFiles.add(b, file));
                    }
                },
                dir -> {
                    bookFromFile(dir).forEach(b -> bookFiles.add(b, dir));
                    return true;
                });
        });
        return bookFiles;
    }

    @NotNull
    private static String trackPartOfFileName(final @NotNull String fileName, final BookModel book) {
        String name = fileName.replaceAll("\\.[^.]+$", "");
        if (book != null) {
            final String title = book.getTitle();
            if (title != null) {
                final int titleAt = name.toLowerCase().indexOf(title.toLowerCase());
                if (titleAt >= 0) {
                    final int titleEnd = titleAt + title.length();
                    if (titleEnd < name.length()) {
                        name = name.substring(titleEnd);
                    }
                }
            }
        }
        return name;
    }

    public static <T> T withCachedFile(
        @NonNull final File cachedFile,
        @NonNull final Duration cacheExpiry,
        @NonNull final Supplier<T> supplier,
        @NonNull final Function<File, T> reader,
        @NonNull final BiConsumer<File, T> writer
    ) {
        boolean useCache = false;
        if (cachedFile.isFile()) {
            if (Instant.ofEpochMilli(cachedFile.lastModified()).plus(cacheExpiry).isAfter(Instant.now())) {
                useCache = true;
            }
        }
        if (useCache) {
            return reader.apply(cachedFile);
        }
        final T t = supplier.get();
        writer.accept(cachedFile, t);
        return t;
    }

    public static String withoutExtensions(final String fileName) {
        if (fileName == null) {
            return null;
        }
        return fileName.replaceAll("(\\.[-_a-zA-Z0-9]+)+$", "");
    }

    public static boolean writeTextFile(@NonNull final File file, @NonNull final String text) {
        try (final FileWriter writer = new FileWriter(file)) {
            writer.write(text);
            return true;
        } catch (IOException e) {
            log.error("Failed to write: {}", file);
        }
        return false;
    }

    @Data
    @AllArgsConstructor
    public static class TrackAndCount {
        Integer count;
        Integer track;
    }
}
