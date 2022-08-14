package org.rickosborne.romance.util;

import lombok.NonNull;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class FileStuff {
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
}
