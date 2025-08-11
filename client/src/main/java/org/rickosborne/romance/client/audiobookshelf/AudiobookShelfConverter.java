package org.rickosborne.romance.client.audiobookshelf;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.rickosborne.romance.db.model.BookModel;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.rickosborne.romance.util.StringStuff.splitNames;
import static org.rickosborne.romance.util.StringStuff.unescape;

@UtilityClass
public class AudiobookShelfConverter {
    public static AudiobookShelfMetadataJson metadataFromBook(@NonNull final BookModel model) {
        final AudiobookShelfMetadataJson metadata = AudiobookShelfMetadataJson.builder()
            .authors(splitNames(model.getAuthorName()).collect(Collectors.toList()))
            .chapters(new LinkedList<>())
            .description(unescape(model.getPublisherDescription()))
            .genres(new LinkedList<>())
            .isbn(model.getIsbn())
            .narrators(splitNames(model.getNarratorName()).collect(Collectors.toList()))
            .publisher(model.getPublisherName())
            .series(Optional.ofNullable(model.getSeriesName()).map(n -> {
                final String part = model.getSeriesPart();
                if (part != null && !part.isBlank()) {
                    return List.of(n + " #" + part);
                }
                return List.of(n);
            }).orElse(null))
            .tags(new ArrayList<>(model.getTags()))
            .title(model.getTitle())
            .build();
        metadata.setPublishedDateFromLocal(model.getDatePublish());
        return metadata;
    }
}
