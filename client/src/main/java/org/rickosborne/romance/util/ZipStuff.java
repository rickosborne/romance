package org.rickosborne.romance.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@UtilityClass
@Slf4j
public class ZipStuff {
    public static int unzipFiles(final Path zipPath, final Path outDir) {
        final String zipName = zipPath.getFileName().toString();
        log.info("Unzip: {}", zipName);
        int count = 0;
        try (
            final InputStream in = new FileInputStream(zipPath.toFile());
            final ZipInputStream zip = new ZipInputStream(in)
        ) {
            ZipEntry entry = zip.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    final String name = entry.getName();
                    log.info("Unzip: {} <- {}", name, zipName);
                    final Path outPath = outDir.resolve(name);
                    try (final OutputStream out = new FileOutputStream(outPath.toFile())) {
                        zip.transferTo(out);
                    }
                }
                entry = zip.getNextEntry();
                count++;
            }
            zip.closeEntry();
        } catch (final IOException e) {
            log.error("Could not unzip {}: {}", zipPath, e.getMessage());
        }
        return count;
    }
}
