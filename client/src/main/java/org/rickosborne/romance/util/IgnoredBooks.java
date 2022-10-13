package org.rickosborne.romance.util;

import lombok.Getter;
import lombok.SneakyThrows;
import org.rickosborne.romance.client.command.LastCommand;
import org.rickosborne.romance.db.model.BookModel;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

public class IgnoredBooks {
    public static final String IGNORED_PURCHASES_CSV = "ignored-purchases.csv";
    @Getter(lazy = true)
    private static final List<Predicate<BookModel>> ignoredBooks = buildIgnoredBooks();

    @SneakyThrows
    private static List<Predicate<BookModel>> buildIgnoredBooks() {
        try (
            final InputStream i = LastCommand.class.getClassLoader().getResourceAsStream(IGNORED_PURCHASES_CSV);
            final BufferedReader r = new BufferedReader(new InputStreamReader(i))
        ) {
            String line;
            int lineNum = 0;
            final List<Predicate<BookModel>> ignored = new LinkedList<>();
            while ((line = r.readLine()) != null) {
                lineNum++;
                final String[] parts = line.split("\\|");
                if (parts.length == 0) {
                    continue;
                } else if (parts.length != 2) {
                    throw new IllegalArgumentException("In " + IGNORED_PURCHASES_CSV + ":" + lineNum + " bogus line: " + line);
                }
                final String title = parts[0];
                final String author = parts[1];
                if (title == null || title.isBlank() || author == null || author.isBlank()) {
                    throw new IllegalArgumentException("In " + IGNORED_PURCHASES_CSV + ":" + lineNum + " bogus line: " + line);
                }
                ignored.add(b -> StringStuff.fuzzyMatch(b.getTitle(), title) && StringStuff.fuzzyListMatch(b.getAuthorName(), author));
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
}
