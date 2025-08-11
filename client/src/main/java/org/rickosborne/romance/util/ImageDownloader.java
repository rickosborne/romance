package org.rickosborne.romance.util;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ImageDownloader {
    public static final Pattern EXT_PATTERN = Pattern.compile("(?=[.])[-_a-zA-Z0-9]+$");

    public static boolean downloadImage(@NonNull final URL url, @NonNull final File file) {
        if (file.exists()) {
            return true;
        }
        try {
            final ReadableByteChannel readChannel = Channels.newChannel(url.openStream());
            try (final FileOutputStream out = new FileOutputStream(file)) {
                final FileChannel writeChannel = out.getChannel();
                writeChannel.transferFrom(readChannel, 0, Long.MAX_VALUE);
            }
            return true;
        } catch (IOException e) {
            log.error("Failed to download: {} {} {}", url, file, e.getMessage());
            return false;
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
