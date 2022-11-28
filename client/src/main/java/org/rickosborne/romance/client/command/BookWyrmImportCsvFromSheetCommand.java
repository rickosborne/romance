package org.rickosborne.romance.client.command;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.rickosborne.romance.client.bookwyrm.BookWyrmCsvStore;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.sheet.SheetStore;
import org.rickosborne.romance.util.BookRating;
import org.rickosborne.romance.util.SheetStuff;
import org.rickosborne.romance.util.StringStuff;
import picocli.CommandLine;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@CommandLine.Command(
    name = "bw-import-csv",
    description = "Generate a CSV in BookWyrm-ready import format"
)
public class BookWyrmImportCsvFromSheetCommand extends ASheetCommand {
    public static final String[] SG_COLUMNS = {
        "Title",
        "Authors",
        "Contributors",
        "ISBN/UID",
        "Format",
        "Read Status",
        "Date Added",
        "Last Date Read",
        "Dates Read",
        "Read Count",
        "Moods",
        "Pace",
        "Character- or Plot-Driven?",
        "Strong Character Development?",
        "Loveable Characters?",
        "Diverse Characters?",
        "Flawed Characters?",
        "Star Rating",
        "Review",
        "Content Warnings",
        "Content Warning Description",
        "Tags",
        "Owned?"
    };
    private static final DateTimeFormatter SG_DATE_FORMAT = new DateTimeFormatterBuilder()
        .appendValue(ChronoField.YEAR, 4)
        .appendLiteral('/')
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .appendLiteral('/')
        .appendValue(ChronoField.DAY_OF_MONTH, 2)
        .toFormatter();
    private final static String SG_NOW = LocalDateTime.now().format(SG_DATE_FORMAT);
    @CommandLine.Option(names = "--bw-csv", description = "BookWyrm Export CSV", required = true)
    private Path bwCsv;
    @CommandLine.Option(names = "--out-csv", description = "Output CSV", required = true)
    private Path outCsv;

    private String[] csvRecordFromBook(final BookModel bookModel) {
        final String narrator = bookModel.getNarratorName();
        return new String[]{
            bookModel.getTitle(),
            bookModel.getAuthorName(),
            narrator == null || narrator.isBlank() ? null : narrator + " (Narrator)",
            bookModel.getIsbn(),
            bookModel.getAudiobookStoreUrl() == null ? null : "audio",
            bookModel.getDateRead() == null ? null : "read",
            Optional.ofNullable(bookModel.getDatePurchase()).map(d -> d.format(SG_DATE_FORMAT)).orElse(SG_NOW),
            Optional.ofNullable(bookModel.getDateRead()).map(d -> d.format(SG_DATE_FORMAT)).orElse(null),
            null,
            bookModel.getDateRead() == null ? null : "1",
            null, // moods
            null, // pace
            null, // character or plot driven
            null, // strong character dev
            null, // lovable
            null, // diverse
            null, // flawed
            Optional.ofNullable(bookModel.getRatings().get(BookRating.Overall)).map(String::valueOf).orElse(null),
            null, // review
            null, // content warnings
            null, // cw desc
            Optional.ofNullable(bookModel.getTags()).map(t -> t.stream().sorted().map(s -> s.replace("#", "")).collect(Collectors.joining(", "))).orElse(null),
            bookModel.getDatePurchase() == null ? "No" : "Yes",
        };
    }

    @Override
    protected Integer doWithSheets() {
        final BookWyrmCsvStore csvStore = new BookWyrmCsvStore(bwCsv);
        final List<Predicate<BookModel>> predicates = csvStore.getBookMinimals().stream()
            .map(minimal -> {
                return (Predicate<BookModel>) (book) -> {
                    return Objects.equals(book.getIsbn(), minimal.getIsbn())
                        || (StringStuff.fuzzyMatch(book.getTitle(), minimal.getTitle())
                        && StringStuff.fuzzyMatch(book.getAuthorName(), minimal.getAuthor()));
                };
            })
            .collect(Collectors.toList());
        final SheetStore<BookModel> sheetStore = getSheetStoreFactory().buildSheetStore(BookModel.class);
        try (final CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(outCsv.toFile()), CSVFormat.RFC4180)) {
            csvPrinter.printRecord(SG_COLUMNS);
            for (final SheetStuff.Indexed<BookModel> indexed : sheetStore.getRecords()) {
                final BookModel sheetBook = indexed.getModel();
                final boolean exists = predicates.stream().anyMatch(p -> p.test(sheetBook));
                if (exists) {
                    continue;
                }
                log.info("Need to add: \"{}\" by {}", sheetBook.getTitle(), sheetBook.getAuthorName());
                csvPrinter.printRecord((Object[]) csvRecordFromBook(sheetBook));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }
}
