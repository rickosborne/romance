package org.rickosborne.romance.db.postgresql;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.client.bookwyrm.BookWyrmConfig;
import org.rickosborne.romance.client.bookwyrm.Shelf;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.util.BookRating;
import org.rickosborne.romance.util.Once;
import org.rickosborne.romance.util.Pair;
import org.rickosborne.romance.util.StringStuff;
import org.rickosborne.romance.util.ThrowingBiConsumer;
import org.rickosborne.romance.util.UrlRank;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.rickosborne.romance.client.bookwyrm.BookWyrm.BOOK_SUBTITLE_DELIMITER;
import static org.rickosborne.romance.client.bookwyrm.BookWyrm.likeExpression;
import static org.rickosborne.romance.util.StringStuff.alphaOnly;
import static org.rickosborne.romance.util.StringStuff.urlFromString;

@Slf4j
@Getter
public class BookWyrmPGBookStore extends BookWyrmPGStore<BookModel> {
    protected static String idFromAuthorTitle(final String author, final String title) {
        if (author == null || title == null) {
            return null;
        }
        final String aa = alphaOnly(author);
        final String at = alphaOnly(title);
        if (aa.isBlank() || at.isBlank()) {
            return null;
        }
        return aa + ":" + at;
    }

    @Getter(lazy = true)
    private final IdCache idCache = Once.supply(() -> {
        final IdCache cache = new IdCache();
        fetchDbIds(cache);
        return cache;
    });

    public BookWyrmPGBookStore(final @NonNull BookWyrmConfig config) {
        super(config, BookModel.class);
    }

    public List<Integer> allIdsFor(final int dbId) {
        final Set<Integer> set = new HashSet<>();
        set.add(dbId);
        final Integer maybeParentId = queryOne(
            "SELECT parent_work_id FROM bookwyrm_edition WHERE book_ptr_id = ?",
            ps -> ps.setInt(1, dbId),
            rs -> rs.getInt("parent_work_id")
        );
        final int parentId = maybeParentId == null ? dbId : maybeParentId;
        set.add(parentId);
        final List<Integer> childIds = query(
            "SELECT book_ptr_id FROM bookwyrm_edition WHERE parent_work_id = ?",
            ps -> ps.setInt(1, parentId),
            rs -> rs.getInt("book_ptr_id")
        );
        set.addAll(childIds);
        return new ArrayList<>(set);
    }

    public boolean exists(final BookModel bookModel) {
        return getIdCache().findDbId(bookModel) != null;
    }

    @SneakyThrows
    private void fetchDbIds(final IdCache cache) {
        query(
            "SELECT b.id, b.title, a.name, COUNT(bs.id) AS shelfCount " +
                "FROM bookwyrm_book AS b " +
                "INNER JOIN bookwyrm_book_authors AS ba ON (b.id = ba.book_id) " +
                "INNER JOIN bookwyrm_author AS a ON (ba.author_id = a.id) " +
                "LEFT JOIN bookwyrm_shelfbook AS bs ON (b.id = bs.book_id) " +
                "GROUP BY b.id, b.title, a.name ",
            null,
            rs -> {
                final int id = rs.getInt("id");
                final String title = rs.getString("title");
                final String author = rs.getString("name");
                final int shelfCount = rs.getInt("shelfCount");
                cache.add(id, title, author, shelfCount);
                return null;
            }
        );
    }

    public List<String> findAuthorNamesForDbId(final int dbId) {
        return query(
            "SELECT " +
                "  a.name " +
                "FROM bookwyrm_book_authors AS ba " +
                "  INNER JOIN bookwyrm_author AS a ON (ba.book_id = ?) AND (ba.author_id = a.id) " +
                "ORDER BY name ",
            ps -> ps.setInt(1, dbId),
            rs -> rs.getString("name")
        );
    }

    public Shelf findBookShelf(final int dbId, final int userId) {
        return queryOne(
            "SELECT s.id, s.name, s.identifier " +
                "FROM bookwyrm_shelfbook AS sb " +
                "  INNER JOIN bookwyrm_shelf AS s ON (s.id = sb.shelf_id) AND (sb.book_id = ?) AND (sb.user_id = ?) " +
                "LIMIT 1",
            ps -> {
                ps.setInt(1, dbId);
                ps.setInt(2, userId);
            },
            rs -> new Shelf(
                rs.getInt("id"),
                rs.getString("identifier"),
                rs.getString("name")
            )
        );
    }

    public BookModel findByDbIdAndUser(final int dbId, final Integer userId) {
        final Pair<BookModel, Integer> pair = queryOne(
            "SELECT * " +
                "FROM bookwyrm_book " +
                "WHERE id = ? ",
            ps -> ps.setInt(1, dbId),
            this::fromResultSet
        );
        final BookModel book = pair == null ? null : pair.getLeft();
        if (book == null) {
            return null;
        }
        final List<String> authorNames = findAuthorNamesForDbId(dbId);
        if (authorNames != null && !authorNames.isEmpty()) {
            book.setAuthorName(String.join(", ", authorNames));
        }
        for (final Pair<String, URL> linkPair : findLinksForDbId(dbId)) {
            final String domain = linkPair.getLeft();
            final URL url = linkPair.getRight();
            if ("audiobookstore.com".equals(domain)) {
                book.setAudiobookStoreUrl(url);
            } else if ("goodreads.com".equals(domain)) {
                book.setGoodreadsUrl(url);
            } else if ("thestorygraph.com".equals(domain)) {
                book.setStorygraphUrl(url);
            }
        }
        if (userId != null) {
            final Double rating = findRating(dbId, userId);
            if (rating != null) {
                book.getRatings().put(BookRating.Overall, rating);
            }
            final ReadDates readDates = findReadDatesForDbId(dbId, userId);
            if (readDates != null && readDates.finishDate != null) {
                book.setDateRead(readDates.finishDate);
            }
        }
        return book;
    }

    @Override
    public BookModel findById(final String id) {
        final Integer dbId = getIdCache().findDbId(id);
        if (dbId == null) {
            return null;
        }
        return findByDbIdAndUser(dbId, null);
    }

    @Override
    public BookModel findLike(final BookModel model) {
        return Optional.ofNullable(findLikeForUser(model, null)).map(Pair::getLeft).orElse(null);
    }

    public Pair<BookModel, Integer> findLikeForUser(final BookModel model, final Integer userId) {
        if (model == null) {
            return null;
        }
        final List<String> authorNames = List.of(model.getAuthorName().split(",\\s+"));
        final String aq = authorNames.stream().map(a -> "LOWER(a.name) LIKE ?").collect(Collectors.joining(" OR "));
        final String title = Optional.ofNullable(model.getTitle())
            .map(t -> t.replaceAll("(" + BOOK_SUBTITLE_DELIMITER + ").*", ""))
            .orElse(null);
        final Integer bookId = queryOne(
            "SELECT DISTINCT b.id, COALESCE(e.edition_rank, 0) as e_edition_rank " +
                "FROM bookwyrm_book AS b " +
                "  INNER JOIN bookwyrm_book_authors AS ba ON (b.id = ba.book_id) " +
                "  INNER JOIN bookwyrm_author AS a ON (ba.author_id = a.id) " +
                "  LEFT JOIN bookwyrm_edition AS e ON (b.id = e.book_ptr_id) " +
                "WHERE LOWER(b.title) LIKE ? " +
                "  AND (" + aq + ") " +
                "ORDER BY e_edition_rank DESC, b.id " +
                "LIMIT 1",
            ps -> {
                ps.setString(1, likeExpression(title));
                int index = 2;
                for (final String authorName : authorNames) {
                    ps.setString(index, likeExpression(authorName));
                    index++;
                }
            },
            rs -> rs.getInt("id")
        );
        if (bookId == null) {
            return null;
        }
        return Pair.build(findByDbIdAndUser(bookId, userId), bookId);
    }

    public List<Pair<String, URL>> findLinksForDbId(final int dbId) {
        return Optional.ofNullable(query(
            "SELECT l.url, ld.name " +
                "FROM bookwyrm_filelink AS fl " +
                "  INNER JOIN bookwyrm_link AS l ON (fl.book_id = ?) AND (fl.link_ptr_id = l.id) " +
                "  INNER JOIN bookwyrm_linkdomain AS ld ON (l.domain_id = ld.id) ",
            ps -> ps.setInt(1, dbId),
            rs -> {
                final String domain = rs.getString("name");
                final URL url = urlFromString(rs.getString("url"));
                return Pair.build(domain, url);
            }
        )).orElseGet(List::of);
    }

    public Double findRating(
        final int bookId,
        final int userId
    ) {
        return queryOne(
            "SELECT r.rating " +
                "FROM bookwyrm_review AS r " +
                "  INNER JOIN bookwyrm_status AS s ON (r.book_id = ?) AND (r.status_ptr_id = s.id) AND (s.user_id = ?) " +
                "LIMIT 1",
            ps -> {
                ps.setInt(1, bookId);
                ps.setInt(2, userId);
            },
            rs -> rs.getDouble("rating")
        );
    }

    public ReadDates findReadDatesForDbId(final int dbId, final int userId) {
        return queryOne("" +
            "SELECT start_date, finish_date " +
            "FROM bookwyrm_readthrough " +
            "WHERE (book_id = ?) AND (user_id = ?)", ps -> {
            ps.setInt(1, dbId);
            ps.setInt(2, userId);
        }, rs -> ReadDates.builder()
            .finishDate(asLocalDate(rs, "finish_date"))
            .startDate(asLocalDate(rs, "start_date"))
            .build());
    }

    public static LocalDate asLocalDate(final ResultSet rs, final String columnName) throws SQLException {
        final OffsetDateTime offsetDateTime = rs.getObject(columnName, OffsetDateTime.class);
        if (offsetDateTime != null) {
            return offsetDateTime.toLocalDate();
        }
        return null;
    }

    private Pair<BookModel, Integer> fromResultSet(final ResultSet rs) throws SQLException {
        final BookModel book = BookModel.build();
        book.setTitle(rs.getString("title"));
        book.setDatePublish(asLocalDate(rs, "published_date"));
        final String remoteId = rs.getString("remote_id");
        book.setMastodonUrl(StringStuff.urlFromString(remoteId));
        final String cover = rs.getString("cover");
        if (cover != null && !cover.isBlank()) {
            book.setImageUrl(StringStuff.urlFromString(UrlRank.fixup(remoteId.replaceAll("book/.+$", cover))));
        }
        book.setPublisherDescription(rs.getString("description"));
        book.setSeriesName(rs.getString("series"));
        book.setSeriesPart(rs.getString("series_number"));
        final String grKey = rs.getString("goodreads_key");
        if (grKey != null && !grKey.isBlank()) {
            book.setGoodreadsUrl(urlFromString("https://www.goodreads.com/book/show/" + grKey));
        }
        return Pair.build(book, rs.getInt("id"));
    }

    @Override
    public String idFromModel(final BookModel model) {
        return idFromAuthorTitle(model.getAuthorName(), model.getTitle());
    }

    public boolean isShelved(final BookModel bookModel) {
        return getIdCache().isShelved(bookModel);
    }

    @Override
    public BookModel save(final BookModel updated) {
        final BookModel existing = findLike(updated);
        final Integer dbId = getIdCache().findDbId(updated);
        if (existing == null || dbId == null) {
            throw new IllegalStateException("Not implemented: save new");
        }
        final List<String> setStatements = new LinkedList<>();
        final List<ThrowingBiConsumer<PreparedStatement, Integer, SQLException>> paramHandlers = new LinkedList<>();
        class Helper {
            <T> void ifChanged(
                final Function<BookModel, T> getter,
                final String setStatement,
                final Function<T, ThrowingBiConsumer<PreparedStatement, Integer, SQLException>> paramHandler
            ) {
                final T before = getter.apply(existing);
                final T after = getter.apply(updated);
                if (before == null && after != null) {
                    setStatements.add(setStatement + " = ?");
                    paramHandlers.add(paramHandler.apply(after));
                }
            }
        }
        final Helper helper = new Helper();
        helper.ifChanged(BookModel::getDatePublish, "published_date", v -> (ps, n) -> ps.setObject(n, OffsetDateTime.of(v, LocalTime.MIDNIGHT, ZoneOffset.UTC)));
        helper.ifChanged(BookModel::getSeriesName, "series", v -> (ps, n) -> ps.setString(n, v));
        helper.ifChanged(BookModel::getSeriesPart, "series_number", v -> (ps, n) -> ps.setString(n, v));
        helper.ifChanged(BookModel::getPublisherDescription, "description", v -> (ps, n) -> ps.setString(n, v));
        if (setStatements.size() > 0) {
            final Connection db = getConnection();
            try {
                final String sql = "UPDATE bookwyrm_book SET " +
                    String.join(", ", setStatements) +
                    " WHERE (id = ?)";
                final PreparedStatement ps = db.prepareStatement(sql);
                for (int i = 0; i < paramHandlers.size(); i++) {
                    paramHandlers.get(i).apply(ps, i + 1);
                }
                ps.setInt(paramHandlers.size() + 1, dbId);
                log.info("Would execute:\n{}", sql);
                //                 final int updateCount = ps.executeUpdate();
                //                if (updateCount != 1) {
                //                    log.info("Expected 1 update, got {}:\n{}", updateCount, sql);
                //                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } else {
            log.debug("No need to update: {}", updated);
        }
        return null;
    }

    public void updateDescription(
        final String description,
        final List<Integer> bookIds
    ) {
        updateMany(bookIds, Map.of("description", (ps, n) -> ps.setString(n, description)));
    }

    public void updateGoodreadsKey(
        final String goodreadsKey,
        final List<Integer> bookIds
    ) {
        updateMany(bookIds, Map.of("goodreads_key", (ps, n) -> ps.setString(n, goodreadsKey)));
    }

    protected void updateMany(
        @NonNull final List<Integer> bookIds,
        @NonNull final Map<String, ThrowingBiConsumer<PreparedStatement, Integer, SQLException>> fields
    ) {
        final List<String> fieldNames = fields.keySet().stream().sorted().collect(Collectors.toList());
        final String setStatements = fieldNames.stream().map(fn -> fn + " = ?").collect(Collectors.joining(", "));
        final String qs = bookIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        log.trace("updateMany {}: {}", bookIds, fieldNames);
        query(
            "UPDATE bookwyrm_book " +
                "SET " + setStatements + " " +
                "WHERE (id IN (" + qs + "))",
            ps -> {
                int index = 1;
                for (final String fieldName : fieldNames) {
                    fields.get(fieldName).apply(ps, index);
                    index++;
                }
                for (final Integer bookId : bookIds) {
                    ps.setInt(index, bookId);
                    index++;
                }
            },
            null
        );
    }

    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class IdCache {
        private final Map<String, Integer> cache = new HashMap<>();
        @Getter
        private int count = 0;
        private final Set<String> shelved = new HashSet<>();

        public void add(final int dbId, final String title, final String author, final int shelfCount) {
            final String id = idFromAuthorTitle(author, title);
            if (id != null) {
                final int countBefore = cache.size();
                if (shelfCount > 0) {
                    cache.put(id, dbId);
                } else {
                    cache.putIfAbsent(id, dbId);
                }
                if (cache.size() > countBefore) {
                    count++;
                }
                if (shelfCount > 0) {
                    shelved.add(id);
                }
            }
        }

        public Integer findDbId(final BookModel book) {
            return cache.get(getId(book));
        }

        public Integer findDbId(final String id) {
            return cache.get(id);
        }

        public void forceId(final String id, final int finalDbId) {
            cache.put(id, finalDbId);
        }

        public String getId(final BookModel book) {
            return idFromAuthorTitle(book.getAuthorName(), book.getTitle());
        }

        public boolean isShelved(final BookModel bookModel) {
            final String id = getId(bookModel);
            return id != null && shelved.contains(id);
        }
    }

    @Builder
    @Value
    static class ReadDates {
        LocalDate finishDate;
        LocalDate startDate;
    }
}
