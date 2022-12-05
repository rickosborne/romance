package org.rickosborne.romance.db.postgresql;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.rickosborne.romance.client.bookwyrm.BookWyrmConfig;
import org.rickosborne.romance.db.model.AuthorModel;
import org.rickosborne.romance.util.Pair;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.rickosborne.romance.client.bookwyrm.BookWyrm.likeExpression;
import static org.rickosborne.romance.util.StringStuff.alphaOnly;
import static org.rickosborne.romance.util.StringStuff.urlFromString;

@Slf4j
public class BookWyrmPGAuthorStore extends BookWyrmPGStore<AuthorModel> {
    public BookWyrmPGAuthorStore(
        final @NonNull BookWyrmConfig config
    ) {
        super(config, AuthorModel.class);
    }

    @Override
    public AuthorModel findById(final String id) {
        throw new NotImplementedException();
    }

    public Pair<AuthorModel, Integer> findByName(@NonNull final String name) {
        final List<Pair<AuthorModel, Integer>> authors = query(
            "SELECT * " +
                "FROM bookwyrm_author " +
                "WHERE LOWER(name) LIKE ? " +
                "ORDER BY id " +
                "LIMIT 1 ",
            ps -> ps.setString(1, likeExpression(name)),
            this::fromResultSet
        );
        if (authors.isEmpty()) {
            return null;
        } else if (authors.size() > 1) {
            throw new IllegalStateException("Multiple authors named \"" + name + "\": " + authors);
        }
        return authors.get(0);
    }

    protected Pair<AuthorModel, Integer> fromResultSet(final ResultSet rs) throws SQLException {
        if (rs == null) {
            return null;
        }
        return Pair.build(AuthorModel.builder()
            .name(rs.getString("name"))
            .goodreadsUrl(Optional.ofNullable(rs.getString("goodreads_key")).map(grk -> urlFromString("https://www.goodreads.com/author/show/" + grk)).orElse(null))
            .build(), rs.getInt("id"));
    }

    @Override
    public String idFromModel(final AuthorModel model) {
        return alphaOnly(model.getName());
    }

    @Override
    public AuthorModel save(final AuthorModel model) {
        throw new NotImplementedException();
    }
}
