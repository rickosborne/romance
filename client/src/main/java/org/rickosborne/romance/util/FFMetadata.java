package org.rickosborne.romance.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.db.model.BookModel;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.rickosborne.romance.util.FileStuff.writeTextFile;
import static org.rickosborne.romance.util.StringStuff.formatMS;
import static org.rickosborne.romance.util.StringStuff.splitNames;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class FFMetadata extends ACLIRunner {
    public static Pattern CHAPTER_PATTERN = Pattern.compile("\\[CHAPTER].+?\\[/CHAPTER]", Pattern.DOTALL);

    public static String chaptersFromFile(@NonNull final File file) {
        final String all = fromFileText(file);
        if (all == null) {
            return null;
        }
        final Matcher matcher = CHAPTER_PATTERN.matcher(all);
        final StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            sb.append(matcher.group()).append("\n");
        }
        if (sb.isEmpty()) {
            return null;
        }
        return sb.toString();
    }

    public static String fromBookModel(@NonNull final BookModel book, final FileStuff.TrackAndCount trackAndCount) {
        final StringBuilder sb = new StringBuilder();
        sb.append("[FORMAT]\n");
        for (final AttrMeta attrMeta : AttrMeta.values()) {
            final String key = attrMeta.metaKey;
            final String value = attrMeta.formatter.apply(book);
            if (key != null && value != null) {
                sb.append(key).append("=").append(value).append("\n");
            }
        }
        final Integer trackNumber = trackAndCount == null ? null : trackAndCount.track;
        final Integer trackCount = trackAndCount == null ? null : trackAndCount.count;
        if (trackNumber != null) {
            sb.append("TAG:track=").append(trackNumber);
            if (trackCount != null) {
                sb.append("/").append(trackCount);
            }
            sb.append("\n");
        }
        if (trackCount != null) {
            sb.append("TAG:TRACKTOTAL=").append(trackCount).append("\n");
        }
        sb.append("[/FORMAT]\n");
        return sb.toString();
    }

    @Nullable
    public static FFMetadata fromFileJson(@NonNull final File file) {
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        final String text = runCLI(file.getParentFile(), "ffprobe", "-hide_banner", "-show_streams", "-show_format", "-show_chapters", "-show_programs", "-print_format", "json", "-i", file.toString());
        try {
            return new ObjectMapper().readerFor(FFMetadata.class).readValue(text);
        } catch (JsonProcessingException e) {
            log.error("JSON Parsing failed: {} for {}", e.getMessage(), file);
        }
        return null;
    }

    @Nullable
    public static String fromFileText(@NonNull final File file) {
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        return runCLI(file.getParentFile(), "ffprobe", "-hide_banner", "-show_streams", "-show_format", "-show_chapters", "-show_programs", "-i", file.toString());
    }

    public static String tagsFileForBook(final BookModel bookModel, final FileStuff.TrackAndCount trackAndCount) {
        if (bookModel == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder().append(";FFMETADATA1\n");
        for (final TagsOnlyMeta meta : TagsOnlyMeta.values()) {
            final String bookValue = meta.formatter.apply(bookModel);
            if (bookValue == null || bookValue.isBlank()) {
                continue;
            }
            sb.append(meta.metaKey).append("=").append(bookValue).append("\n");
        }
        if (trackAndCount != null) {
            final Integer track = trackAndCount.getTrack();
            final Integer count = trackAndCount.getCount();
            if (track != null) {
                sb.append("track=").append(track);
                if (count != null) {
                    sb.append("/").append(count);
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public static void updateTags(@NonNull final File file, @NonNull final Path tempPath, final BookModel book, final int fileCount) {
        if (book == null) {
            return;
        }
        final String fileName = file.getName();
        FileStuff.TrackAndCount trackAndCount = FileStuff.interpretFileParts(fileName, book);
        if (trackAndCount == null) {
            trackAndCount = new FileStuff.TrackAndCount(fileCount, null);
        }
        final String text = tagsFileForBook(book, trackAndCount);
        if (text == null) {
            log.info("No FFMetadata for book: {}", book);
            return;
        }
        final File parentFile = file.getParentFile();
        final String baseName = FileStuff.withoutExtensions(fileName);
        final String ffName = baseName + ".ffmetadata.txt";
        final File ffFile = parentFile.toPath().resolve(ffName).toFile();
        log.info("Writing ffmetadata.txt: {}", ffFile);
        if (!writeTextFile(ffFile, text)) {
            log.warn("Could not write FFMetadata: {} -> {}", book, ffFile);
            return;
        }
        final String tempName = "~" + fileName;
        final File tempFile = tempPath.resolve(tempName).toFile();
        log.info("Importing ffmetadata: {}", book);
        final String out = runCLI(tempPath.toFile(), "ffmpeg", "-i", file.toString(), "-i", ffFile.toString(), "-c", "copy", "-movflags", "use_metadata_tags", "-map_metadata", "1", tempFile.toString());
        if (out == null) {
            log.warn("ffmpeg failed: {}", file);
            return;
        }
        log.info("Verifying metadata: {}", tempFile);
        final FFMetadata saved = fromFileJson(tempFile);
        if (saved == null) {
            log.warn("Could not ffprobe: {}", tempFile);
            return;
        }
        final BookModel tempBook = saved.asBookModel();
        if (!Objects.equals(tempBook.getTitle(), book.getTitle())) {
            log.warn("Titles are not equal: {} vs {}", book.getTitle(), tempBook.getTitle());
            return;
        }
        final String tombstoneName = "~" + fileName + "~";
        final File tombstoneFile = parentFile.toPath().resolve(tombstoneName).toFile();
        log.info("Tombstone: {}", tombstoneName);
        if (!file.renameTo(tombstoneFile)) {
            log.warn("Rename failed: {} -> {}", fileName, tombstoneFile);
            return;
        }
        log.info("Moving updated: {}", file);
        if (!tempFile.renameTo(file)) {
            log.warn("Move failed: {} -> {}", tempFile, file);
            return;
        }
        log.info("Deleting tombstone: {}", tombstoneName);
        if (!tombstoneFile.delete()) {
            log.warn("Delete failed: {}", tombstoneFile);
        }
        log.info("Updated tags: {}", book);
    }

    private List<FFChapter> chapters;
    private FFFormat format;
    private List<Map<String, Object>> programs;
    private List<FFStream> streams;

    @JsonIgnore
    public BookModel asBookModel() {
        final String description = getTag("description");
        final String catNumber = getTag("CATALOGNUMBER");
        final String isbn = catNumber == null || catNumber.length() < 10 ? null : catNumber;
        final String sku = catNumber == null || catNumber.length() != 4 ? null : catNumber;
        return BookModel.builder()
            .authorName(getTag("artist", "ARTISTS"))
            .title(getTag("album"))
            .narratorName(getTag("Performer", "compose", "album_artist"))
            // .datePublish(Optional.ofNullable(getTag("ALBUM_YEAR")).map((s) -> LocalDate.of(Integer.valueOf(s, 10), 1, 1)).orElse(null))
            .publisherName(getTag("copyright"))
            .audiobookStoreSku(sku)
            .isbn(isbn)
            .publisherDescription(Optional.ofNullable(description).map(StringStuff::unescape).orElse(null))
            .build();
    }

    public String getTag(final String... tagNames) {
        if (format == null || format.tags == null) {
            return null;
        }
        for (final String tag : tagNames) {
            final String value = format.tags.get(tag);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    public void importFromBook(final BookModel book, final boolean overwrite) {
        if (format == null) {
            format = new FFFormat();
        }
        if (format.tags == null) {
            format.tags = new HashMap<>();
        }
        final Map<String, String> tags = format.tags;
        for (final AttrMeta attrMeta : AttrMeta.values()) {
            final String incoming = attrMeta.formatter.apply(book);
            if (incoming == null || incoming.isBlank()) {
                continue;
            }
            final String tagName = attrMeta.metaKey.replaceFirst("^TAG:", "");
            final String existing = tags.get(tagName);
            if (existing != null && !existing.isBlank() && !overwrite) {
                continue;
            }
            tags.put(tagName, incoming);
        }
    }

    @JsonIgnore
    public String toFFText() {
        final StringBuilder sb = new StringBuilder();
        if (format != null) {
            sb.append(format.toFFText());
        }
        if (chapters != null) {
            chapters.forEach((c) -> sb.append(c.toFFText()));
        }
        return sb.toString();
    }

    enum AttrMeta {
        title("TAG:title", BookModel::getTitle),
        album("TAG:album", BookModel::getTitle),
        author("TAG:artist", BookModel::getAuthorName),
        authors("TAG:ARTISTS", BookModel::getAuthorName, (name) -> splitNames(name).collect(Collectors.joining("/"))),
        narrator("TAG:Performer", BookModel::getNarratorName),
        albumYear("TAG:ALBUM_YEAR", BookModel::getDatePublish, (pub) -> String.valueOf(pub.getYear())),
        year("TAG:year", BookModel::getDatePublish, (pub) -> String.valueOf(pub.getYear())),
        date("TAG:date", BookModel::getDatePublish, DateTimeFormatter.ISO_LOCAL_DATE::format),
        group("TAG:GROUP", BookModel::getSeriesName),
        partNumber("TAG:PARTNUMBER", BookModel::getSeriesPart),
        publisher("TAG:publisher", BookModel::getPublisherName),
        isbn("TAG:CATALOGNUMBER", BookModel::getIsbn),
        description("TAG:description", BookModel::getPublisherDescription),
        ;
        private final Function<BookModel, String> formatter;
        private final String metaKey;

        AttrMeta(@NonNull final String metaKey, @NonNull final Function<BookModel, String> formatter) {
            this.formatter = formatter;
            this.metaKey = metaKey;
        }

        <T> AttrMeta(@NonNull final String metaKey, @NonNull final Function<BookModel, T> accessor, @NonNull final Function<T, String> formatter) {
            this.formatter = (b) -> {
                final T value = accessor.apply(b);
                return value == null ? null : formatter.apply(value);
            };
            this.metaKey = metaKey;
        }
    }

    enum TagsOnlyMeta {
        album("album", BookModel::getTitle),
        artist("artist", BookModel::getAuthorName),
        copyright("copyright", BookModel::getPublisherName),
        date("date", BookModel::getDatePublish, DateTimeFormatter.ISO_LOCAL_DATE::format),
        narrator("performer", BookModel::getNarratorName),
        publisher("publisher", BookModel::getPublisherName),
        title("title", BookModel::getTitle),

        authors("ARTISTS", BookModel::getAuthorName, (name) -> splitNames(name).collect(Collectors.joining("/"))),
        albumYear("ALBUM_YEAR", BookModel::getDatePublish, (pub) -> String.valueOf(pub.getYear())),
        year("year", BookModel::getDatePublish, (pub) -> String.valueOf(pub.getYear())),
        group("GROUP", BookModel::getSeriesName),
        partNumber("PARTNUMBER", BookModel::getSeriesPart),
        isbn("CATALOGNUMBER", BookModel::getIsbn),
        description("description", (b) -> Optional.ofNullable(b.getPublisherDescription()).map((s) -> s.replaceAll("[\\t\\r\\n]+", "   ")).orElse(null)),
        ;
        private final Function<BookModel, String> formatter;
        private final String metaKey;

        TagsOnlyMeta(@NonNull final String metaKey, @NonNull final Function<BookModel, String> formatter) {
            this.formatter = formatter;
            this.metaKey = metaKey;
        }

        <T> TagsOnlyMeta(@NonNull final String metaKey, @NonNull final Function<BookModel, T> accessor, @NonNull final Function<T, String> formatter) {
            this.formatter = (b) -> {
                final T value = accessor.apply(b);
                return value == null ? null : formatter.apply(value);
            };
            this.metaKey = metaKey;
        }
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class FFChapter {
        Integer end;
        @JsonProperty("end_time")
        private String endTime;
        private int id;
        Integer start;
        @JsonProperty("start_time")
        private String startTime;
        private Map<String, String> tags;
        @JsonProperty("time_base")
        private String timeBase;

        public String toFFText() {
            final StringBuilder sb = new StringBuilder()
                .append("[CHAPTER]\nid=").append(id).append("\n");
            if (timeBase != null) {
                sb.append("time_base=").append(timeBase).append("\n");
            }
            if (start != null) {
                sb.append("start=").append(start).append("\n");
            }
            if (end != null) {
                sb.append("end=").append(end).append("\n");
            }
            if (startTime != null) {
                sb.append("start_time=").append(startTime).append("\n");
            }
            if (endTime != null) {
                sb.append("end_time=").append(endTime).append("\n");
            }
            if (tags != null) {
                tags.forEach((tag, value) -> sb.append("TAG:").append(tag).append("=").append(value).append("\n"));
            }
            return sb.append("[/CHAPTER]\n").toString();
        }

        public String toString() {
            String s = tags == null ? ("#" + id) : tags.get("title");
            if (start != null) {
                s += " " + formatMS(start) + "-";
                if (end != null) {
                    s += formatMS(end);
                }
            } else if (end != null) {
                s += " 00:00-" + formatMS(end);
            }
            return s;
        }
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class FFDisposition {
        @JsonProperty("attached_pic")
        private Integer attachedPic;
        private Integer captions;
        @JsonProperty("clean_effects")
        private Integer cleanEffects;
        private Integer comment;
        private Integer dependent;
        private Integer descriptions;
        private Integer dub;
        private Integer forced;
        @JsonProperty("hearing_impaired")
        private Integer hearingImpaired;
        @JsonProperty("default")
        private Integer isDefault;
        private Integer karaoke;
        private Integer lyrics;
        private Integer metadata;
        private Integer multilayer;
        @JsonProperty("non_diegetic")
        private Integer nonDiegetic;
        private Integer original;
        @JsonProperty("still_image")
        private Integer stillImage;
        @JsonProperty("timed_thumbnails")
        private Integer timedThumbnails;
        @JsonProperty("visual_impaired")
        private Integer visualImpaired;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class FFFormat {
        @JsonProperty("bit_rate")
        private String bitRate;
        @JsonProperty("duration")
        private String duration;
        @JsonProperty("filename")
        private String fileName;
        @JsonProperty("format_long_name")
        private String formatLongName;
        @JsonProperty("format_name")
        private String formatName;
        @JsonProperty("nb_programs")
        private Integer nbPrograms;
        @JsonProperty("nb_stream_groups")
        private Integer nbStreamGroups;
        @JsonProperty("nb_streams")
        private Integer nbStreams;
        @JsonProperty("probe_score")
        private Integer probeScore;
        private String size;
        @JsonProperty("start_time")
        private String startTime;
        private Map<String, String> tags;

        public String toFFText() {
            final StringBuilder sb = new StringBuilder()
                .append("[FORMAT]\n");
            tags.forEach((tag, value) -> {
                sb.append("TAG:").append(tag).append("=").append(value).append("\n");
            });
            sb.append("[/FORMAT]\n");
            return sb.toString();
        }
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class FFStream {
        @JsonProperty("avg_frame_rate")
        private String avgFrameRate;
        @JsonProperty("bit_rate")
        private String bitRate;
        @JsonProperty("bits_per_sample")
        private Integer bitsPerSample;
        @JsonProperty("channel_layout")
        private String channelLayout;
        private Integer channels;
        @JsonProperty("codec_long_name")
        private String codecLongName;
        @JsonProperty("codec_name")
        private String codecName;
        @JsonProperty("codec_tag")
        private String codecTag;
        @JsonProperty("codec_tag_string")
        private String codecTagString;
        @JsonProperty("codec_type")
        private String codecType;
        @JsonProperty("coded_height")
        private Integer codedHeight;
        @JsonProperty("coded_width")
        private Integer codedWidth;
        private FFDisposition disposition;
        /**
         * Seconds.
         */
        @JsonProperty("duration")
        private String duration;
        @JsonProperty("duration_ts")
        private Long durationTs;
        @SuppressWarnings("SpellCheckingInspection")
        @JsonProperty("extradata_size")
        private Integer extraDataSize;
        private Integer height;
        @JsonProperty("id")
        private String id;
        private int index;
        @JsonProperty("initial_padding")
        private Integer initialPadding;
        @JsonProperty("nb_frames")
        private String nbFrames;
        @JsonProperty("pix_fmt")
        private String pixelFormat;
        private String profile;
        @JsonProperty("r_frame_rate")
        private String rFrameRate;
        @JsonProperty("sample_fmt")
        private String sampleFormat;
        @JsonProperty("sample_rate")
        private String sampleRate;
        @JsonProperty("start_pts")
        private Integer startPTS;
        @JsonProperty("start_time")
        private String startTime;
        private Map<String, String> tags;
        @JsonProperty("time_base")
        private String timeBase;
        private Integer width;
    }
}
