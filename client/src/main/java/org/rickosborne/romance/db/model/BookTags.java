package org.rickosborne.romance.db.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.mp4.field.Mp4TagTextField;
import org.rickosborne.romance.util.BookStuff;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

@Slf4j
public enum BookTags {
    album(FieldKey.ALBUM, BookModel::getTitle),
    albumYear(FieldKey.ALBUM_YEAR, (b) -> Optional.ofNullable(b.getDatePublish()).map((d) -> String.valueOf(d.getYear())).orElse(null)),
    artist(FieldKey.ARTIST, BookModel::getAuthorName),
    artists(FieldKey.ARTISTS, (b) -> Optional.ofNullable(b.getAuthorName()).map((a) -> a.replaceAll("\\s*,\\s*", "/")).orElse(null)),
    catalogNo(FieldKey.CATALOG_NO, BookModel::getAudiobookStoreSku),
    composer(FieldKey.COMPOSER, BookModel::getNarratorName),
    description1("desc", BookModel::getPublisherDescription),
    // description2("©des", BookModel::getPublisherDescription),
    discNo(FieldKey.DISC_NO, (_b, e) -> Optional.ofNullable(e.filePartNumber).map(String::valueOf).orElse(null)),
    group(FieldKey.GROUP, BookModel::getSeriesName),
    isbn("isbn", BookModel::getIsbn),
    // discSubtitle(FieldKey.DISC_SUBTITLE, BookTags::buildSubtitle),
    // lyrics(FieldKey.LYRICS, BookModel::getPublisherDescription),
    part(FieldKey.PART, BookTags::buildPart),
    partNumber(FieldKey.PART_NUMBER, (_b, e) -> Optional.ofNullable(e.filePartNumber).map(String::valueOf).orElse(null)),
    performer(FieldKey.PERFORMER, BookModel::getNarratorName),
    publisher("publisher", BookModel::getPublisherName),
    series("mvnm", BookModel::getSeriesName),
    seriesPart("mvin", BookModel::getSeriesPart),
    // subtitle(FieldKey.SUBTITLE, BookTags::buildSubtitle),
    title(FieldKey.TITLE, (b) -> BookStuff.cleanTitle(b.getTitle())),
    track(FieldKey.TRACK, (_b, e) -> Optional.ofNullable(e.filePartNumber).map(String::valueOf).orElse(null)),
    trackTotal(FieldKey.TRACK_TOTAL, (_b, e) -> Optional.ofNullable(e.filePartCount).map(String::valueOf).orElse(null)),
    year(FieldKey.YEAR, (b) -> Optional.ofNullable(b.getDatePublish()).map((d) -> String.valueOf(d.getYear())).orElse(null)),
    ;

    public static void applyTags(@NonNull final File file, @NonNull final BookModel book, @NonNull final Extras extras) {
        try {
            final AudioFile audioFile = AudioFileIO.readAs(file, "m4a");
            final Tag tag = audioFile.getTag();
            boolean changed = false;
            for (final BookTags bookTag : BookTags.values()) {
                final String value;
                if (bookTag.accessor != null) {
                    value = bookTag.accessor.apply(book);
                } else if (bookTag.accessorWithExtras != null) {
                    value = bookTag.accessorWithExtras.apply(book, extras);
                } else {
                    value = null;
                }
                if (value != null) {
                    if (bookTag.fieldKey != null) {
                        if (!tag.hasField(bookTag.fieldKey)) {
                            tag.addField(bookTag.fieldKey, value);
                            log.info("  + {} = {}", bookTag.fieldKey.name(), value);
                            changed = true;
                        } else {
                            final List<TagField> fields = tag.getFields(bookTag.fieldKey);
                            if (fields.size() == 1) {
                                final TagField field = fields.get(0);
                                final String oldValue = field.toString();
                                if (!field.isBinary() && !Objects.equals(oldValue, value)) {
                                    log.info("  ~ {}: {} => {}", bookTag.fieldKey, oldValue, value);
                                    tag.setField(bookTag.fieldKey, value);
                                    changed = true;
                                }
                            }
                        }
                    }
                    if (bookTag.fieldName != null) {
                        final Mp4TagTextField textField = new Mp4TagTextField(bookTag.fieldName, value);
                        if (!tag.hasField(bookTag.fieldName)) {
                            log.info("  + custom {} = {}", bookTag.fieldName, value);
                            tag.addField(textField);
                        } else {
                            final List<TagField> fields = tag.getFields(bookTag.fieldName);
                            if (fields.size() == 1) {
                                final TagField field = fields.get(0);
                                final String oldValue = field.toString();
                                if (!field.isBinary() && !Objects.equals(oldValue, value)) {
                                    log.info("  ~ custom {} = {} => {}", bookTag.fieldName, oldValue, value);
                                    tag.setField(textField);
                                    changed = true;
                                }
                            }
                        }
                    }
                }
            }
            if (changed) {
                audioFile.commit();
            }
        } catch (CannotWriteException ex) {
            log.warn("Tags may be busted: {}", file);
        } catch (CannotReadException | TagException | InvalidAudioFrameException | ReadOnlyFileException |
                 IOException e) {
            log.error("Failed to retag: {}", book);
            throw new RuntimeException(e);
        }
    }

    public static String buildPart(
        final BookModel bookModel,
        final Extras extras
    ) {
        if (extras.filePartNumber == null) {
            return null;
        } else if (extras.filePartCount == null) {
            return String.valueOf(extras.filePartNumber);
        }
        return String.format("%d/%d", extras.filePartNumber, extras.filePartCount);
    }

    public static String buildSubtitle(
        final BookModel bookModel,
        final Extras extras
    ) {
        if (extras.filePartNumber == null) {
            return null;
        }
        if (extras.filePartCount != null) {
            return String.format("Part %d of %d", extras.filePartNumber, extras.filePartCount);
        }
        return String.format("Part %d", extras.filePartNumber);
    }

    public static void readChapters(@NonNull final File file) {
        try {
            final AudioFile audioFile = AudioFileIO.readAs(file, "m4a");
        } catch (CannotReadException | IOException | TagException | ReadOnlyFileException |
                 InvalidAudioFrameException e) {
            log.error("Could not read chapters: {}: {}", file, e.getMessage());
        }
    }
    @Getter
    private Function<BookModel, String> accessor;
    @Getter
    private BiFunction<BookModel, Extras, String> accessorWithExtras;
    @Getter
    private final FieldKey fieldKey;
    @Getter
    private final String fieldName;

    BookTags(final String fieldName, final Function<BookModel, String> accessor) {
        this.fieldKey = null;
        this.fieldName = fieldName;
        this.accessor = accessor;
    }

    BookTags(final FieldKey fieldKey, final Function<BookModel, String> accessor) {
        this.fieldName = null;
        this.fieldKey = fieldKey;
        this.accessor = accessor;
    }

    BookTags(final FieldKey fieldKey, final BiFunction<BookModel, Extras, String> accessorWithExtras) {
        this.fieldName = null;
        this.fieldKey = fieldKey;
        this.accessorWithExtras = accessorWithExtras;
    }

    @Data
    @AllArgsConstructor
    public static class Extras {
        Integer filePartCount;
        Integer filePartNumber;
    }
}
