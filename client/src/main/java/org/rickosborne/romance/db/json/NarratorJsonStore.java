package org.rickosborne.romance.db.json;

import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.JsonStore;
import org.rickosborne.romance.db.model.NarratorModel;
import org.rickosborne.romance.db.model.NarratorSchema;

import java.nio.file.Path;

public class NarratorJsonStore extends JsonStore<NarratorModel> {
    public NarratorJsonStore(
        final NamingConvention namingConvention,
        final Path dbPath
    ) {
        super(DbModel.Narrator, new NarratorSchema(), NarratorModel.class, namingConvention, dbPath.resolve(DbModel.Narrator.getTypeName()));
    }
}
