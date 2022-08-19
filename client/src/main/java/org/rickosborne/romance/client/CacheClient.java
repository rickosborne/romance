package org.rickosborne.romance.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.rickosborne.romance.util.StringStuff.FILE_NAME_MAX_LENGTH;
import static org.rickosborne.romance.util.StringStuff.noLongerThan;

@Slf4j
public class CacheClient<S> {
    public static String encodeCacheKey(final String key) {
        // return Base64.getMimeEncoder().encodeToString(key.getBytes()).replaceAll("=+$", "");
        final Base64.Encoder encoder = Base64.getMimeEncoder().withoutPadding();
        final StringBuilder sb = new StringBuilder();
        final Pattern pattern = Pattern.compile("[^-_a-zA-Z0-9]");
        final Matcher matcher = pattern.matcher(key);
        while (matcher.find()) {
            matcher.appendReplacement(sb, encoder.encodeToString(matcher.group().getBytes()));
        }
        matcher.appendTail(sb);
        return noLongerThan(FILE_NAME_MAX_LENGTH, sb.toString());
    }

    @NonNull
    private final Path cachePath;
    private final Integer delaySeconds;
    @NonNull
    @Getter
    private final S service;

    public CacheClient(
        @NonNull final S service,
        @NonNull final Path basePath,
        @NonNull final String cacheName,
        final Integer delaySeconds
    ) {
        this.cachePath = basePath.resolve(cacheName);
        this.service = service;
        final File cacheDirFile = cachePath.toFile();
        if (!cacheDirFile.exists()) {
            if (!cacheDirFile.mkdirs()) {
                throw new IllegalStateException("Could not create: " + cachePath);
            }
        }
        this.delaySeconds = delaySeconds;
    }

    public CacheClient(
        @NonNull final S service,
        @NonNull final Path basePath,
        @NonNull final String cacheName
    ) {
        this(service, basePath, cacheName, null);
    }

    public <T> T fetchFomCache(
        final TypeReference<T> type,
        final Function<S, Call<T>> clientCall,
        final String key
    ) {
        final String fileName = encodeCacheKey(key) + ".json";
        final File cacheFile = cachePath.resolve(fileName).toFile();
        final JsonMapper jsonMapper = new JsonMapper();
        if (cacheFile.exists()) {
            try {
                return jsonMapper.readerFor(type).readValue(cacheFile);
            } catch (IOException e) {
                throw new RuntimeException("Could not read: " + cacheFile, e);
            }
        }
        final Call<T> call = clientCall.apply(service);
        try {
            if (this.delaySeconds != null) {
                try {
                    Thread.sleep(1000L * delaySeconds);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
            final Response<T> response = call.execute();
            if (response.isSuccessful()) {
                final T result = response.body();
                jsonMapper.writerFor(type)
                    .withDefaultPrettyPrinter()
                    .writeValue(cacheFile, result);
                return result;
            } else {
                log.warn("Unable to fetch: " + key);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
