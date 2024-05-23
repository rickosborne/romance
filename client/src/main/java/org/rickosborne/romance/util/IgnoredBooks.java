package org.rickosborne.romance.util;

import lombok.Getter;
import lombok.SneakyThrows;
import org.rickosborne.romance.db.model.BookModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

public class IgnoredBooks {
    public static final String IGNORED_DOWNLOADS_TSV = "ignored-downloads.tsv";
    public static final String IGNORED_PURCHASES_CSV = "ignored-purchases.csv";
    public static final String PIPE_DELIM = "\\|";
    public static final String TAB_DELIM = "\t";
    @Getter(lazy = true)
    private static final List<Predicate<BookModel>> ignoredBooks = buildIgnoredBooks();
    @Getter(lazy = true)
    private static final List<Predicate<BookModel>> ignoredDownloads = buildIgnoredDownloads();

    private static List<Predicate<BookModel>> buildIgnoredBooks() {
        return buildIgnoredList(IGNORED_PURCHASES_CSV, PIPE_DELIM, false);
    }

    private static List<Predicate<BookModel>> buildIgnoredDownloads() {
        return buildIgnoredList(IGNORED_DOWNLOADS_TSV, TAB_DELIM, true);
    }

    private static List<Predicate<BookModel>> buildIgnoredList(
        final String filePath,
        final String recordDelimiter,
        final boolean skipHeader
    ) {
        return eachTextFileLine(filePath, recordDelimiter, (final String[] parts, final int lineNum, final String line) -> {
            if (skipHeader && lineNum == 1) {
                return null;
            }
            final String title = parts[0];
            final String author = parts[1];
            if (title == null || title.isBlank() || author == null || author.isBlank()) {
                throw new IllegalArgumentException("In " + IGNORED_PURCHASES_CSV + ":" + lineNum + " bogus line: " + line);
            }
            return b -> StringStuff.fuzzyMatch(b.getTitle(), title) && StringStuff.fuzzyListMatch(b.getAuthorName(), author);
        });
    }

    @SneakyThrows
    private static <T> List<T> eachTextFileLine(
        final String filePath,
        final String recordDelimiter,
        final TextFileLineHandler<T> handler
    ) {
        final File csvFile = Path.of(filePath).toFile();
        if (!csvFile.isFile()) {
            return List.of();
        }
        try (
            final InputStream i = new FileInputStream(csvFile);
            final BufferedReader r = new BufferedReader(new InputStreamReader(i))
        ) {
            String line;
            int lineNum = 0;
            final List<T> ignored = new LinkedList<>();
            while ((line = r.readLine()) != null) {
                lineNum++;
                final String[] parts = line.split(recordDelimiter);
                if (parts.length == 0) {
                    continue;
                } else if (parts.length != 2) {
                    throw new IllegalArgumentException("In " + filePath + ":" + lineNum + " bogus line: " + line);
                }
                final T t = handler.apply(parts, lineNum, line);
                if (t != null) {
                    ignored.add(t);
                }
            }
            return ignored;
        }
    }

    public static boolean isIgnored(final BookModel bookModel) {
        return bookModel == null || getIgnoredBooks().stream().anyMatch(p -> p.test(bookModel));
    }

    public static boolean isNotIgnored(final BookModel bookModel) {
        return bookModel != null && getIgnoredBooks().stream().noneMatch(p -> p.test(bookModel));
    }

    public interface TextFileLineHandler<T> {
        T apply(final String[] parts, final int lineNum, final String line);
    }
}
