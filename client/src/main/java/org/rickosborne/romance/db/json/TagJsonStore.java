package org.rickosborne.romance.db.json;

import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.JsonStore;
import org.rickosborne.romance.db.model.TagModel;
import org.rickosborne.romance.db.model.TagSchema;

import java.nio.file.Path;

public class TagJsonStore extends JsonStore<TagModel> {
    public TagJsonStore(
        final NamingConvention namingConvention,
        final Path dbPath
    ) {
        super(DbModel.Tag, new TagSchema(), TagModel.class, namingConvention, dbPath.resolve(DbModel.Tag.getTypeName()));
    }
}
