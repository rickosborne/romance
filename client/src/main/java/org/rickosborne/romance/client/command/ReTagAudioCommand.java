package org.rickosborne.romance.client.command;

import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.db.Diff;
import org.rickosborne.romance.db.SchemaDiff;
import org.rickosborne.romance.db.json.JsonStore;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.model.BookSchema;
import org.rickosborne.romance.util.BidirectionalMultiMap;
import org.rickosborne.romance.util.BookBot;
import org.rickosborne.romance.util.FFMetadata;
import org.rickosborne.romance.util.FileStuff;
import picocli.CommandLine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.rickosborne.romance.util.BookMerger.bookLikeFilter;
import static org.rickosborne.romance.util.FFMetadata.updateTags;
import static org.rickosborne.romance.util.FileStuff.scanBookFiles;
import static org.rickosborne.romance.util.FileStuff.withoutExtensions;

@Slf4j
@CommandLine.Command(
    name = "retag-audio",
    description = "Re-tag audio files from Book metadata"
)
public class ReTagAudioCommand extends ASheetCommand {
    @CommandLine.Option(names = {"--audio-path"}, description = "Path to a directory where audio already exists", required = true)
    private List<File> audioPaths;
    @CommandLine.Option(names = {"--temp-path"}, description = "Path for temp files", required = false)
    private Path tempPath = Path.of(".cache", "download");

    @Override
    protected Integer doWithSheets() {
        Objects.requireNonNull(audioPaths, "Audio paths");
        Objects.requireNonNull(tempPath, "Temp path");
        if (audioPaths.isEmpty()) {
            throw new IllegalArgumentException("Must have at least one audio path");
        }
        if (!tempPath.toFile().exists() || !tempPath.toFile().isDirectory()) {
            throw new IllegalArgumentException("Temp Path does not exist or is not a directory: " + tempPath);
        }
        final BidirectionalMultiMap<BookModel, File, String> bookFiles = scanBookFiles(audioPaths);
        log.info("Found {} existing books in {} files/dirs", bookFiles.leftSize(), bookFiles.rightSize());
        final BookBot bot = getBookBot();
        final JsonStore<BookModel> bookStore = bot.getBookStore();
        final BookSchema bookSchema = bot.getBookSchema();
        final SchemaDiff schemaDiff = new SchemaDiff();
        for (final BookModel fileBook : bookFiles.getLefts()) {
            final List<File> filesAndDirs = bookFiles.rightsFor(fileBook);
            if (filesAndDirs == null) {
                log.error("Unexpected lack of files for book: {}", fileBook);
                continue;
            }
            final List<File> files = filesAndDirs.stream().filter(File::isFile).toList();
            if (files.isEmpty()) {
                log.error("Dirs only for book: {}", fileBook);
                continue;
            }
            final BookModel storeBook = bookStore.findLikeOrMatch(fileBook, bookLikeFilter(fileBook));
            if (storeBook == null) {
                continue;
            }
            BookModel mergedBook = storeBook;
            log.info("{} has {} file{}", storeBook, files.size(), files.size() == 1 ? "" : "s");
            for (final File file : files) {
                final long fileSize = file.length();
                if (fileSize <= 0) {
                    log.info("Skip: {}", file);
                    continue;
                }
                // try {
                //     final AudioFile m4a = AudioFileIO.readAs(file, "m4a");
                //     final Tag tag = m4a.getTag();
                //     log.info("{} => {}", file.getPath(), tag);
                // } catch (CannotReadException | IOException | TagException | ReadOnlyFileException |
                //          IllegalArgumentException | InvalidAudioFrameException e) {
                //     log.warn("Could not read with AudioFile: {} for {}", e.getMessage(), file);
                // }
                final FFMetadata ffMetadata = FFMetadata.fromFileJson(file);
                if (ffMetadata == null) {
                    log.warn("Could not read with ffprobe: {}", file);
                } else {
                    ffMetadata.importFromBook(storeBook, true);
                    final BookModel ffBook = ffMetadata.asBookModel();
                    mergedBook = bookSchema.mergeModels(ffBook, mergedBook);
                    final String ffText = FFMetadata.tagsFileForBook(mergedBook, FileStuff.interpretFileParts(file.getName(), mergedBook));
                    if (ffText != null) {
                        final String ffMetadataFileName = withoutExtensions(file.getName()) + ".ffmetadata.txt";
                        final File ffFile = file.getParentFile().toPath().resolve(ffMetadataFileName).toFile();
                        try (final FileWriter writer = new FileWriter(ffFile)) {
                            writer.write(ffText);
                            log.info("FFMetadata: {}", ffFile);
                        } catch (IOException e) {
                            log.error("Could not write: {} :: {}", e.getMessage(), ffFile);
                        }
                    }
                }
                final Diff<BookModel> diff = schemaDiff.diffModels(storeBook, mergedBook);
                if (diff.hasChanged()) {
                    log.info("Diff: {}\n{}", mergedBook, diff.asDiffLines());
                    updateTags(file, tempPath.toAbsolutePath(), mergedBook, files.size());
                }
            }
        }
        return 0;
    }

}
