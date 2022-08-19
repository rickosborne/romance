package org.rickosborne.romance.client.command;

import lombok.Getter;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.model.BookSchema;
import org.rickosborne.romance.util.BookBot;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(
    name = "data-from-abs",
    description = "Fetch all available data from the ABS Library"
)
public class DataFromAudiobookStoreCommand implements Callable<Integer> {
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

    @Override
    public Integer call() throws Exception {
        final BookBot bookBot = new BookBot(auth, cachePath, null, dbPath, null);
        for (final BookModel fetched : bookBot.fetchAudiobooks()) {
            bookBot.extendAll(fetched);
        }
        return 0;
    }
}
