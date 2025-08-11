package org.rickosborne.romance.util;

import lombok.NonNull;
import org.rickosborne.romance.client.response.AudiobookStoreSuggestion;
import org.rickosborne.romance.db.model.AuthorAttributes;
import org.rickosborne.romance.db.model.AuthorModel;
import org.rickosborne.romance.db.model.BookModel;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class AuthorMerger extends AMerger {
    public static Predicate<AuthorModel> authorLikeFilter(final AuthorModel like) {
        return b -> StringStuff.fuzzyMatch(b.getName(), like.getName());
    }

    public static Predicate<BookModel> bookWithAuthorLikeFilter(@NonNull final AuthorModel like) {
        final String authorName = like.getName();
        return b -> StringStuff.fuzzyMatch(b.getAuthorName(), authorName);
    }

    public static AuthorModel fromAudiobookStoreSuggestion(@NonNull final AudiobookStoreSuggestion suggestion) {
        return AuthorModel.builder()
            .name(suggestion.getCleanTitle())
            .audiobookStoreUrl(suggestion.getUrl())
            .build();
    }

    public static AuthorModel merge(final AuthorModel... models) {
        final AuthorModel model = AuthorModel.build();
        for (final AuthorAttributes attr : AuthorAttributes.values()) {
            final Function<AuthorModel, ?> accessor = attr.getAccessor();
            final BiConsumer<AuthorModel, Object> mutator = attr.getMutator();
            if (accessor != null && mutator != null) {
                final Class<Object> type = attr.getAttributeType();
                final Object value = coalesce(accessor, models);
                if (type.isInstance(value)) {
                    mutator.accept(model, value);
                }
            }
        }
        final Map<BookRating, Double> ratings = coalesce(AuthorModel::getRatings, r -> !r.isEmpty(), models);
        if (ratings != null) {
            model.getRatings().putAll(ratings);
        }
        return model;
    }
}
