package org.rickosborne.romance.client.audiobookshelf;

import lombok.Data;

import java.util.List;

@Data
public class AudiobookShelfLibrarySettings {
    boolean audiobooksOnly;
    String autoScanCronExpression;
    int coverAspectRatio;
    boolean disableWatcher;
    boolean epubsAllowScriptedContent;
    boolean hideSingleBookSeries;
    Double markAsFinishedPercentComplete;
    Integer markAsFinishedTimeRemaining;
    List<String> metadataPrecedence;
    boolean onlyShowLaterBooksInContinueSeries;
    String podcastSearchRegion;
    boolean skipMatchingMediaWithAsin;
    boolean skipMatchingMediaWithIsbn;
}
