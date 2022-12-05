package org.rickosborne.romance.db.postgresql;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.client.bookwyrm.BookWyrmConfig;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.ModelStore;
import org.rickosborne.romance.db.model.ModelSchema;
import org.rickosborne.romance.db.model.ModelSchemas;
import org.rickosborne.romance.util.ThrowingConsumer;
import org.rickosborne.romance.util.ThrowingFunction;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

@Slf4j
@Getter(value = AccessLevel.PROTECTED)
public abstract class BookWyrmPGStore<M> implements ModelStore<M>, Closeable {
    private final BookWyrmConfig config;
    private Connection connection;
    @Getter(value = AccessLevel.PUBLIC)
    private final DbModel dbModel;
    private final ModelSchema<M> modelSchema;
    @Getter(value = AccessLevel.PUBLIC)
    private final Class<M> modelType;
    private final NamingConvention namingConvention = new NamingConvention();
    private final Map<String, PreparedStatement> preparedStatements = new HashMap<>();


    protected BookWyrmPGStore(
        @NonNull final BookWyrmConfig config,
        @NonNull final Class<M> modelType
    ) {
        this.config = config;
        this.modelType = modelType;
        this.modelSchema = ModelSchemas.schemaForModelType(modelType);
        this.dbModel = DbModel.forModelType(modelType);
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.warn("Could not close connection: {}", getClass().getSimpleName(), e);
            }
            connection = null;
        }
    }

    @SneakyThrows
    private Connection connect() {
        if (connection == null) {
            log.debug("Connecting: {}", getModelType().getSimpleName());
            final Properties props = new Properties();
            props.setProperty("ssl", "false");
            connection = DriverManager.getConnection(config.getJdbcUrl(), props);
        }
        return connection;
    }

    @Override
    public M findByIdFromCache(final String id) {
        return findById(id);
    }

    protected <T> List<T> query(
        @NonNull final String sql,
        final ThrowingConsumer<PreparedStatement, SQLException> params,
        final ThrowingFunction<ResultSet, T, SQLException> block
    ) {
        // log.debug(sql);
        try {
            @SuppressWarnings("resource") final Connection db = connect();
            final PreparedStatement st = preparedStatements.computeIfAbsent(sql, s -> {
                try {
                    return db.prepareStatement(s);
                } catch (SQLException e) {
                    return null;
                }
            });
            if (st == null) {
                throw new IllegalArgumentException("Bad SQL:\n" + sql);
            }
            if (params != null) {
                params.accept(st);
            }
            final List<T> list = new LinkedList<>();
            final ResultSet rs;
            if (block == null) {
                rs = null;
                final int updated = st.executeUpdate();
                log.trace("Updated {}", updated);
            } else {
                rs = st.executeQuery();
                while (rs.next()) {
                    list.add(block.apply(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(String.format("Could not query:\n%s", sql), e);
        }
    }

    protected <T> T queryOne(
        @NonNull final String sql,
        final ThrowingConsumer<PreparedStatement, SQLException> params,
        final ThrowingFunction<ResultSet, T, SQLException> block
    ) {
        final List<T> list = query(sql, params, block);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    @Override
    public Stream<M> stream() {
        throw new NotImplementedException();
    }

}
