package org.rickosborne.romance.client.audiobookshelf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
public class AudiobookShelfBookMinified {
    List<AudiobookShelfAudioFile> audioFiles;
    List<AudiobookShelfBookChapter> chapters;
    String coverPath;
    Double duration;
    String ebookFile;
    String ebookFormat;
    UUID id;
    UUID libraryItemId;
    AudiobookShelfBookMetadataMinified metadata;
    Integer numAudioFiles;
    Integer numChapters;
    Integer numInvalidAudioFiles;
    Integer numMissingParts;
    Integer numTracks;
    Long size;
    List<String> tags;
    List<AudiobookShelfAudioTrack> tracks;

    @JsonIgnore
    public String loggable() {
        if (metadata == null) {
            if (id == null) {
                return "?";
            }
            return "Book#" + id;
        }
        return metadata.authorName + " (" + metadata.publishedYear + ") " + metadata.title;
    }
}
