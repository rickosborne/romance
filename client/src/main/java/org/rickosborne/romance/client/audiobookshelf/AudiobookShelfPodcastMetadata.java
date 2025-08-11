package org.rickosborne.romance.client.audiobookshelf;

import lombok.Data;

import java.util.List;

@Data
public class AudiobookShelfPodcastMetadata {
    String author;
    String description;
    boolean explicit;
    String feedUrl;
    List<String> genres;
    String imageUrl;
    String itunesArtistId;
    String itunesId;
    String itunesPageUrl;
    String language;
    String releaseDate;
    String title;
    String type;
}
