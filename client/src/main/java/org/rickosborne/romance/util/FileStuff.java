package org.rickosborne.romance.util;

import lombok.NonNull;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class FileStuff {
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
