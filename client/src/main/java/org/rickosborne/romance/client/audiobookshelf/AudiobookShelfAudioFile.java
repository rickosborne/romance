package org.rickosborne.romance.client.audiobookshelf;

import lombok.Data;

import java.util.List;

@Data
public class AudiobookShelfAudioFile implements IAudiobookShelfAddedAt, IAudiobookShelfUpdatedAt {
    Long addedAt;
    /**
     * In bits/s.
     */
    Integer bitRate;
    String channelLayout;
    Integer channels;
    List<AudiobookShelfBookChapter> chapters;
    String codec;
    Integer discNumFromFilename;
    Integer discNumFromMeta;
    /**
     * In seconds.
     */
    Double duration;
    String embeddedCoverArt;
    String error;
    Boolean exclude;
    String format;
    Integer index;
    String ino;
    Boolean invalid;
    String language;
    Boolean manuallyVerified;
    AudiobookShelfAudioMetaTags metaTags;
    AudiobookShelfFileMetadata metadata;
    String mimeType;
    String timeBase;
    Integer trackNumFromFilename;
    Integer trackNumFromMeta;
    Long updatedAt;
}
