package org.rickosborne.romance.client.audiobookshelf;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class AudiobookShelfPodcastEpisode implements IAudiobookShelfAddedAt, IAudiobookShelfUpdatedAt {
    Long addedAt;
    AudiobookShelfAudioFile audioFile;
    AudiobookShelfAudioTrack audioTrack;
    List<Object> chapters;
    String description;
    Integer duration;
    Map<String, Object> enclosure;
    String episode;
    String episodeType;
    String guid;
    UUID id;
    Integer index;
    UUID libraryItemId;
    String oldEpisodeId;
    UUID podcastId;
    String pubDate;
    Long publishedAt;
    String season;
    Long size;
    String subtitle;
    String title;
    Long updatedAt;
}
