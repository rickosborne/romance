package org.rickosborne.romance.client.bookwyrm;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.NotImplementedException;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.ModelStore;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.util.Once;
import org.rickosborne.romance.util.StringStuff;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public class BookWyrmCsvStore implements ModelStore<BookModel> {
    @SuppressWarnings("deprecation")
    public static CSVParser buildParser(final Path csvPath) {
        try {
            return CSVParser.parse(csvPath, Charset.defaultCharset(), CSVFormat.DEFAULT.withFirstRecordAsHeader());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<BookModel> modelsFromCsv(final CSVParser csvParser) {
        return csvParser.stream()
            .map(record -> {
                final BookModel bookModel = BookModel.builder().build();
                for (final BookWyrmCsv value : BookWyrmCsv.values()) {
                    value.setBook(bookModel, record.get(value));
                }
                return bookModel;
            })
            .collect(Collectors.toList());
    }

    public static List<MinimalBookInfo> minimalsFromCsv(final CSVParser csvParser) {
        return csvParser.stream().map(MinimalBookInfo::fromRecord).collect(Collectors.toList());
    }

    private final Path csvPath;
    @Getter(lazy = true)
    private final File csvFile = Once.supply(() -> getCsvPath().toFile());
    @Getter(lazy = true)
    private final CSVParser csvParser = buildParser(getCsvPath());
    @Getter(lazy = true)
    private final List<BookModel> bookModels = modelsFromCsv(getCsvParser());
    @Getter(lazy = true)
    private final List<MinimalBookInfo> bookMinimals = minimalsFromCsv(getCsvParser());
    private final DbModel dbModel = DbModel.Book;
    private final Class<BookModel> modelType = BookModel.class;

    @Override
    public BookModel findById(final String id) {
        throw new NotImplementedException("BookModel::findById");
    }

    @Override
    public BookModel findByIdFromCache(final String id) {
        throw new NotImplementedException("BookModel::findByIdFromCache");
    }

    @Override
    public String idFromModel(final BookModel model) {
        throw new NotImplementedException("BookModel::idFromModel");
    }

    @Override
    public BookModel save(final BookModel model) {
        throw new NotImplementedException("BookModel::save");
    }

    @Override
    public Stream<BookModel> stream() {
        return getBookModels().stream();
    }

    @Value
    public static class MinimalBookInfo {
        public static MinimalBookInfo fromRecord(@NonNull final CSVRecord record) {
            return new MinimalBookInfo(
                record.get(BookWyrmCsv.author_text),
                record.get(BookWyrmCsv.goodreads_key),
                StringStuff.coalesceNonBlank(record.get(BookWyrmCsv.isbn_13), record.get(BookWyrmCsv.isbn_10)),
                record.get(BookWyrmCsv.title)
            );
        }

        String author;
        String goodreadsKey;
        String isbn;
        String title;
    }
}
