package org.rickosborne.romance.db.json;

import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.model.WatchModel;
import org.rickosborne.romance.db.model.WatchSchema;

import java.nio.file.Path;

public class WatchJsonStore extends JsonStore<WatchModel> {
    public WatchJsonStore(
        final NamingConvention namingConvention,
        final Path dbPath
    ) {
        super(DbModel.Watch, new WatchSchema(), WatchModel.class, namingConvention, dbPath.resolve(DbModel.Watch.getTypeName()));
    }
}
