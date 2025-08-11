package org.rickosborne.romance.client.audiobookshelf;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AudiobookShelfPodcast {
    boolean autoDownloadEpisodes;
    String autoDownloadSchedule;
    String coverPath;
    int duration;
    List<AudiobookShelfPodcastEpisode> episodes;
    UUID id;
    long lastCoverSearch;
    String lastCoverSearchQuery;
    long lastEpisodeCheck;
    long latestEpisodePublished;
    UUID libraryItemId;
    int maxEpisodesToKeep;
    int maxNewEpisodesToDownload;
    AudiobookShelfPodcastMetadata metadata;
    int numTracks;
    long size;
    List<String> tags;
}
