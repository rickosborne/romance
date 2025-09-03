package org.rickosborne.romance.util;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ImageDownloader {
    public static final Pattern EXT_PATTERN = Pattern.compile("(?=[.])[-_a-zA-Z0-9]+$");

    @Getter(lazy = true)
    private static final OkHttpClient client = new OkHttpClient.Builder()
        .readTimeout(Duration.ofSeconds(60))
        .followRedirects(true)
        .followRedirects(true)
        .build();

    public static File downloadImage(
        @NonNull final URL url,
        @NonNull final Function<String, File> fileNamer
    ) {
        try {
            final Call call = getClient().newCall(new Request.Builder()
                .get()
                .url(url)
                .build());
            try (final Response response = call.execute()) {
                if (response.isSuccessful()) {
                    final String contentType = response.header("content-type");
                    final File file = fileNamer.apply(contentType);
                    final ResponseBody body = response.body();
                    if (body == null) {
                        log.warn("Did not download: {} {} {}", response.code(), response.message(), url);
                    } else {
                        try (final FileOutputStream out = new FileOutputStream(file)) {
                            final InputStream inputStream = body.byteStream();
                            inputStream.transferTo(out);
                            return file;
                        }
                    }
                } else {
                    log.warn("HTTP response: {} {} << GET {}", response.code(), response.message(), url);
                }
            }
            return null;
        } catch (IOException e) {
            log.error("Failed to download: {} {}", url, e.getMessage());
            return null;
        }
    }

    public static String getExtension(final URL url) {
        if (url == null) {
            return null;
        }
        try {
            final String fileName = Path.of(url.toURI().getPath()).getFileName().toString();
            final Matcher matcher = EXT_PATTERN.matcher(fileName);
            if (matcher.matches()) {
                return matcher.group(0);
            }
            return null;
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
