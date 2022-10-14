package org.rickosborne.romance.client.reddit;

import lombok.Getter;
import lombok.Value;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.PublicContribution;
import net.dean.jraw.models.SearchSort;
import net.dean.jraw.models.Submission;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.OAuthHelper;
import net.dean.jraw.pagination.SearchPaginator;
import net.dean.jraw.references.CommentsRequest;
import net.dean.jraw.references.SubredditReference;
import net.dean.jraw.tree.CommentNode;
import net.dean.jraw.tree.RootCommentNode;
import org.rickosborne.romance.Romance;
import org.rickosborne.romance.db.DbJsonWriter;
import org.rickosborne.romance.util.Once;

import java.net.URI;
import java.nio.file.Path;

public class Reddit {
    public static final Path CREDENTIALS_PATH = Path.of(".credentials", "reddit.json");
    public static final String ROMANCE_BOOKS_SUBREDDIT = "RomanceBooks";
    public static final CommentsRequest TOP_LEVEL_COMMENTS = new CommentsRequest.Builder()
        .depth(1)
        .build();
    @Getter(lazy = true)
    private final RedditCredentialsConfig config = DbJsonWriter.readFile(CREDENTIALS_PATH.toFile(), RedditCredentialsConfig.class);
    @Getter(lazy = true)
    private final Credentials credentials = Once.supply(() -> {
        final RedditCredentialsConfig config = getConfig();
        return Credentials.script(config.userName, config.password, config.appId, config.secret);
    });
    @Getter(lazy = true)
    private final UserAgent userAgent = Once.supply(() -> new UserAgent("romancebooks-ratings", "org.rickosborne.romance", Romance.VERSION, "RickOsborne"));
    @Getter(lazy = true)
    private final RedditClient redditClient = Once.supply(() -> OAuthHelper.automatic(
        new OkHttpNetworkAdapter(getUserAgent()), getCredentials()
    ));
    @Getter(lazy = true)
    private final SubredditReference romanceBooksSubreddit = Once.supply(() -> getRedditClient().subreddit(ROMANCE_BOOKS_SUBREDDIT));

    public void romanceSubreddit() {
        final SearchPaginator search = getRomanceBooksSubreddit().search()
            .query("flair:WDYR")
            .sorting(SearchSort.NEW)
            .build();
        for (final Listing<Submission> submissions : search) {
            for (final Submission submission : submissions) {
                final String postId = submission.getId();
                final RootCommentNode comments = getRedditClient().submission(postId).comments(TOP_LEVEL_COMMENTS);
                for (final CommentNode<?> comment : comments) {
                    final PublicContribution<?> contribution = comment.getSubject();
                    final String body = contribution.getBody();
                    final String author = contribution.getAuthor();
                }
            }
        }
    }

    @Value
    private static class RedditCredentialsConfig {
        String appId;
        String password;
        URI redirectUri;
        String secret;
        String userName;
    }
}
