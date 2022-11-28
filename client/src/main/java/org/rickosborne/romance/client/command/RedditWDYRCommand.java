package org.rickosborne.romance.client.command;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.client.reddit.Reddit;
import org.rickosborne.romance.client.reddit.RedditPostStore;
import org.rickosborne.romance.db.model.RedditPostModel;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@CommandLine.Command(
    name = "reddit-wdyr",
    description = "Reddit WDYR Fetch"
)
@Slf4j
public class RedditWDYRCommand implements Callable<Integer> {
    public static final String FETCH_DEFAULT = "FALSE";
    public static final String LAST_DEFAULT = "4";
    public static final String AUTHOR_NAME_PATTERN = "\\s*(?<authorName>\\w[\\w\\s-.]+)";
    public static final String BY_AUTHOR_NAME_PATTERN = "(\\s+by\\s+" + AUTHOR_NAME_PATTERN + ")?";
    public static final String DATE_PUBLISH_PATTERN = "(\\s+\\((?<datePublish>\\d+)\\))?";
    public static final String TITLE_CHARS = "(?<title>\\w[\\w\\s-.!?]+)";
    public static final String DQ_TITLE = "\"" + TITLE_CHARS + "\"";
    public static final String US_TITLE = "_+" + TITLE_CHARS + "_+";
    public static final String STAR_TITLE = "\\*+" + TITLE_CHARS + "\\*+";
    public static final String TITLE_PATTERN = "(" + DQ_TITLE + "|" + US_TITLE + "|" + STAR_TITLE + ")";

    private final List<Pattern> patterns = List.of(
        Pattern.compile(TITLE_PATTERN + DATE_PUBLISH_PATTERN + BY_AUTHOR_NAME_PATTERN),
        Pattern.compile("\\{+" + TITLE_CHARS + BY_AUTHOR_NAME_PATTERN + "}+")
    );

    @SuppressWarnings("FieldMayBeFinal")
    @Getter(value = AccessLevel.PROTECTED)
    @CommandLine.Option(names = {"--path", "-p"}, description = "Path to DB dir")
    private Path dbPath = Path.of("book-data");

    @SuppressWarnings("FieldMayBeFinal")
    @CommandLine.Option(names = "--fetch", defaultValue = FETCH_DEFAULT)
    private boolean fetchLatest = false;

    @SuppressWarnings("FieldMayBeFinal")
    @CommandLine.Option(names = "--last", defaultValue = LAST_DEFAULT)
    private int lastCount = Integer.parseInt(LAST_DEFAULT, 10);

    private final RedditPostStore postStore = new RedditPostStore(RedditPostStore.RedditPostType.Submission, new NamingConvention(), getDbPath().resolve("wdyr"));

    @Override
    public Integer call() throws Exception {
        if (fetchLatest) {
            fetchLatest();
        }
        final List<RedditPostModel> comments = postStore.stream()
            .sorted(Comparator.comparing(RedditPostModel::getUpdated).reversed())
            .limit(lastCount)
            .flatMap(post -> Reddit.flatten(post, new LinkedList<>()).stream())
            .collect(Collectors.toList());
        System.out.printf("Aggregating %d comments", comments.size());

        return 0;
    }

    private void fetchLatest() {
        postStore.getReddit().fetchWDYR(1, 2).forEach(postStore::save);
    }


}
