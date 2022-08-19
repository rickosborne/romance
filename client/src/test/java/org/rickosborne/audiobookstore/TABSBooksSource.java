package org.rickosborne.audiobookstore;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.client.response.BookInformation;
import org.rickosborne.romance.db.json.JsonStore;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class TABSBooksSource {
    protected static final JsonStore<BookInformation> bookInfoStore = new JsonStore<>(
        null,
        null,
        BookInformation.class,
        new NamingConvention(),
        Path.of("../.cache/abs-api")
    );

    public static class WithAudio implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(final ExtensionContext context) throws Exception {
            return bookInfoStore.streamWithFiles()
                .filter(pair -> pair.getLeft().getAudioFiles() != null && !pair.getLeft().getAudioFiles().isEmpty())
                .sorted(Comparator.comparing(p -> p.getLeft().getTitle()))
                .map(p -> Arguments.of(p.getLeft(), p.getRight()));
        }
    }
}
