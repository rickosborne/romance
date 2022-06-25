package org.rickosborne.romance.db.json;

import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.JsonStore;
import org.rickosborne.romance.db.model.AuthorModel;
import org.rickosborne.romance.db.model.AuthorSchema;

import java.nio.file.Path;

public class AuthorJsonStore extends JsonStore<AuthorModel> {
    public AuthorJsonStore(
        final NamingConvention namingConvention,
        final Path dbPath
    ) {
        super(DbModel.Author, new AuthorSchema(), AuthorModel.class, namingConvention, dbPath.resolve(DbModel.Author.getTypeName()));
    }
}
