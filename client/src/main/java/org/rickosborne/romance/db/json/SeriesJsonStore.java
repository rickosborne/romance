package org.rickosborne.romance.db.json;

import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.model.SeriesModel;
import org.rickosborne.romance.db.model.SeriesSchema;

import java.nio.file.Path;

public class SeriesJsonStore extends JsonStore<SeriesModel> {
    public SeriesJsonStore(
        final NamingConvention namingConvention,
        final Path dbPath
    ) {
        super(DbModel.Series, new SeriesSchema(), SeriesModel.class, namingConvention, dbPath.resolve(DbModel.Series.getTypeName()));
    }
}
