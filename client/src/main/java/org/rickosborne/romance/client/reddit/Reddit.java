package org.rickosborne.romance.client.reddit;

import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentSort;
import net.dean.jraw.models.PublicContribution;
import net.dean.jraw.models.SearchSort;
import net.dean.jraw.models.Submission;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.OAuthHelper;
import net.dean.jraw.pagination.Paginator;
import net.dean.jraw.references.CommentsRequest;
import net.dean.jraw.references.SubredditReference;
import net.dean.jraw.tree.CommentNode;
import org.jetbrains.annotations.NotNull;
import org.rickosborne.romance.Romance;
import org.rickosborne.romance.db.DbJsonWriter;
import org.rickosborne.romance.db.model.RedditPostModel;
import org.rickosborne.romance.util.Once;
import org.rickosborne.romance.util.StringStuff;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class Reddit {
    public static final Path CREDENTIALS_PATH = Path.of(".credentials", "reddit.json");
    public static final String FLAIR_WDYR_QUERY = "flair:WDYR";
    public static final String ROMANCE_BOOKS_SUBREDDIT = "RomanceBooks";
    public static final CommentsRequest TOP_LEVEL_COMMENTS = new CommentsRequest.Builder()
        .depth(2)
        .sort(CommentSort.NEW)
        .build();

    public static List<RedditPostModel> flatten(@NonNull final RedditPostModel post, @NonNull final List<RedditPostModel> result) {
        result.add(post);
        final List<RedditPostModel> comments = post.getComments();
        if (comments != null && !comments.isEmpty()) {
            for (final RedditPostModel comment : comments) {
                flatten(comment, result);
            }
        }
        return result;
    }

    public static URL urlFor(final PublicContribution<?> contribution) {
        if (contribution instanceof Comment) {
            final Comment comment = (Comment) contribution;
            return StringStuff.urlFromString(comment.getUrl());
        } else if (contribution instanceof Submission) {
            final Submission submission = (Submission) contribution;
            return StringStuff.urlFromString(submission.getUrl());
        } else {
            log.error("Cannot find a URL for contribution: " + contribution);
        }
        return null;
    }

    @Getter(lazy = true)
    private final RedditCredentialsConfig config = DbJsonWriter.readFile(CREDENTIALS_PATH.toFile(), RedditCredentialsConfig.class);
    @Getter(lazy = true)
    private final Credentials credentials = buildCredentials();
    @Getter(lazy = true)
    private final UserAgent userAgent = Once.supply(() -> new UserAgent("romancebooks-ratings", "org.rickosborne.romance", Romance.VERSION, "RickOsborne"));
    @Getter(lazy = true)
    private final RedditClient redditClient = buildRedditClient();
    @Getter(lazy = true)
    private final SubredditReference romanceBooksSubreddit = buildRomanceBooksSubreddit();

    @NotNull
    private Credentials buildCredentials() {
        final RedditCredentialsConfig config = getConfig();
        return Credentials.userless(config.appId, config.secret, config.deviceId);
//        return Credentials.script(config.userName, config.password, config.appId, config.secret);
    }

    @NotNull
    private RedditClient buildRedditClient() {
        final Credentials creds = getCredentials();
        return OAuthHelper.automatic(new OkHttpNetworkAdapter(getUserAgent()), creds);
    }

    @NotNull
    private SubredditReference buildRomanceBooksSubreddit() {
        final RedditClient client = getRedditClient();
        return client.subreddit(ROMANCE_BOOKS_SUBREDDIT);
    }

    public Stream<RedditPostModel> fetchWDYR(
        final int maxPages,
        final int maxCommentDepth
    ) {
        return getRomanceBooksSubreddit().search()
            .query(FLAIR_WDYR_QUERY)
            .sorting(SearchSort.NEW)
            .limit(Paginator.RECOMMENDED_MAX_LIMIT)
            .build()
            .accumulate(maxPages)
            .stream()
            .flatMap(List::stream)
            .map(s -> redditPostModelFromContribution(s, maxCommentDepth, getRedditClient()));
    }

    public RedditPostModel redditPostModelFromContribution(
        final PublicContribution<?> contribution,
        final int maxCommentDepth,
        final RedditClient redditClient
    ) {
        final String parentFullName;
        final List<RedditPostModel> comments;
        if (contribution == null) {
            return null;
        }
        if (contribution instanceof Comment) {
            final Comment comment = (Comment) contribution;
            parentFullName = comment.getParentFullName();
            if (maxCommentDepth < 1) {
                comments = null;
            } else {
                comments = comment.getReplies().stream()
                    .filter(Comment.class::isInstance)
                    .map(Comment.class::cast)
                    .map(c -> redditPostModelFromContribution(c, maxCommentDepth - 1, redditClient))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            }
        } else if (contribution instanceof Submission) {
            parentFullName = null;
            final Submission submission = (Submission) contribution;
            if (redditClient == null) {
                comments = null;
            } else {
                comments = redditClient
                    .submission(submission.getId())
                    .comments(TOP_LEVEL_COMMENTS)
                    .getReplies().stream()
                    .map(CommentNode::getSubject)
                    .map(c -> redditPostModelFromContribution(c, maxCommentDepth - 1, redditClient))
                    .collect(Collectors.toList());
            }
        } else {
            throw new IllegalArgumentException("Unknown contribution type: " + contribution.getClass().getSimpleName());
        }
        return new RedditPostModel(
            contribution.getAuthor(),
            contribution.getBody(),
            comments,
            contribution.getId(),
            parentFullName,
            contribution.getScore(),
            Optional.ofNullable(contribution.getEdited()).orElseGet(contribution::getCreated),
            urlFor(contribution)
        );
    }

    @Value
    private static class RedditCredentialsConfig {
        String appId;
        UUID deviceId;
        String password;
        URI redirectUri;
        String secret;
        String userName;
    }
}
