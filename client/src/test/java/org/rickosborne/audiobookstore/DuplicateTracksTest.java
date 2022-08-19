package org.rickosborne.audiobookstore;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.rickosborne.romance.client.response.BookFile;
import org.rickosborne.romance.client.response.BookInformation;

import java.io.File;
import java.util.List;
import java.util.Objects;

@Slf4j
public class DuplicateTracksTest {
    @ArgumentsSource(TABSBooksSource.WithAudio.class)
    @ParameterizedTest
    void duplicateTracks(final BookInformation book, final File file) {
        if (book.getTitle() == null) {
            if (!file.delete()) {
                Assertions.fail("Could not delete: " + file);
            }
            Assertions.fail("Empty BookInformation");
        }
        final List<BookFile> audioFiles = book.getAudioFiles();
        BookFile previous = null;
        int sameCount = 0;
        for (final BookFile current : audioFiles) {
            if (previous != null) {
                if (Objects.equals(previous.getFileSize(), current.getFileSize())) {
                    sameCount++;
                }
            }
            previous = current;
        }
        Assertions.assertTrue(sameCount < 3, sameCount + " dupes");
    }
}
