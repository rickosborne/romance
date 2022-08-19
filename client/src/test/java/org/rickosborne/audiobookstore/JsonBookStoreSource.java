package org.rickosborne.audiobookstore;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.json.JsonStore;
import org.rickosborne.romance.db.json.JsonStoreFactory;
import org.rickosborne.romance.db.model.BookModel;

import java.nio.file.Path;
import java.util.stream.Stream;

public class JsonBookStoreSource implements ArgumentsProvider {
    private final JsonStoreFactory jsonStoreFactory = new JsonStoreFactory(Path.of("./book-data"), new NamingConvention());
    private final JsonStore<BookModel> bookStore = jsonStoreFactory.buildJsonStore(BookModel.class);

    @Override
    public Stream<? extends Arguments> provideArguments(final ExtensionContext context) {
        return bookStore.stream().map(Arguments::of);
    }
}
