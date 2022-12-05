package org.rickosborne.romance.client.bookwyrm;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.db.DbJsonWriter;

import java.nio.file.Path;

@Slf4j
@RequiredArgsConstructor
@Getter
public class BookWyrmConfig {
    public static final Path CONFIG_PATH_DEFAULT = Path.of(".credentials", "bookwyrm.json");
    @Getter(lazy = true)
    private static final BookWyrmConfig instance = new BookWyrmConfig();

    public static Data readData(final BookWyrmConfig config) {
        return DbJsonWriter.readFile(config.getConfigPath().toFile(), Data.class);
    }

    private final Path configPath;
    @Delegate
    @Getter(lazy = true, value = AccessLevel.PROTECTED)
    private final Data data = readData(this);

    protected BookWyrmConfig() {
        this(CONFIG_PATH_DEFAULT);
    }

    @Value
    private static class Data {
        String apiBase;
        String apiKey;
        Shelf dnfShelf;
        String jdbcUrl;
        Shelf otherShelf;
        Shelf readShelf;
        Shelf readingShelf;
        int userId;
    }
}
