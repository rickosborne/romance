package org.rickosborne.romance.client.reddit;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dean.jraw.RedditClient;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.PublicContribution;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.json.JsonStore;
import org.rickosborne.romance.db.model.RedditPostModel;
import org.rickosborne.romance.db.model.RedditPostSchema;

import java.nio.file.Path;
import java.util.function.BiFunction;

@Getter
@Slf4j
public class RedditPostStore extends JsonStore<RedditPostModel> {
    public static final int MAX_COMMENT_DEPTH = 3;
    public static final Path STORE_PATH = Path.of("book-data");

    public static Comment fetchCommentById(final String id, @NonNull final RedditClient redditClient) {
        if (id == null) {
            return null;
        }
        final String fullName = redditClient.comment(id).getFullName();
        final Listing<Object> listing = redditClient.lookup(fullName);
        final int count = listing.size();
        if (count > 1) {
            throw new IllegalArgumentException("Expected at most 1 comment for id " + id + " but found " + count);
        } else if (count == 0) {
            return null;
        }
        final Object item = listing.get(0);
        if (!(item instanceof Comment)) {
            throw new IllegalArgumentException("Expected Comment for " + id + " but found " + item.getClass().getSimpleName());
        }
        return (Comment) item;
    }

    @Getter(lazy = true)
    private final Reddit reddit = new Reddit();
    @Getter(lazy = true)
    private final RedditClient redditClient = getReddit().getRedditClient();
    private final RedditPostType redditPostType;

    public RedditPostStore(
        @NonNull final RedditPostType redditPostType,
        @NonNull final NamingConvention namingConvention,
        @NonNull final Path typePath
    ) {
        super(DbModel.RedditPost, new RedditPostSchema(), RedditPostModel.class, namingConvention, typePath);
        this.redditPostType = redditPostType;
    }

    @Override
    public RedditPostModel findById(final String id) {
        final RedditPostModel cached = findByIdFromCache(id);
        if (cached != null) {
            return cached;
        }
        final PublicContribution<?> contribution = redditPostType.fetcher.apply(getRedditClient(), id);
        if (contribution == null) {
            return null;
        }
        final RedditPostModel post = getReddit().redditPostModelFromContribution(contribution, MAX_COMMENT_DEPTH, getRedditClient());
        save(post);
        return post;
    }

    @Getter
    @RequiredArgsConstructor
    public enum RedditPostType {
        Submission((c, id) -> c.submission(id).comments().getSubject()),
        Comment((c, id) -> fetchCommentById(id, c)),
        ;
        private final BiFunction<RedditClient, String, PublicContribution<?>> fetcher;
    }
}
