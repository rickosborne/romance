package org.rickosborne.romance.db.sheet;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.model.ModelSchema;
import org.rickosborne.romance.db.model.ModelSchemas;
import org.rickosborne.romance.sheet.AdapterFactory;
import org.rickosborne.romance.sheet.ModelSheetAdapter;

import java.util.stream.Stream;

@RequiredArgsConstructor
public class SheetStoreFactory {
    private final AdapterFactory adapterFactory = new AdapterFactory();
    private final NamingConvention namingConvention;
    private final String userId;

    public <M> SheetStore<M> buildSheetStore(@NonNull final Class<M> modelType) {
        final DbModel dbModel = Stream.of(DbModel.values()).filter(dbm -> modelType == dbm.getModelType()).findAny().orElse(null);
        if (dbModel == null) {
            throw new IllegalArgumentException("Unknown model type: " + modelType.getSimpleName());
        }
        final ModelSchema<M> schema = Stream.of(ModelSchemas.values())
            .filter(ms -> modelType == ms.getModelClass())
            .map(ms -> {
                @SuppressWarnings("UnnecessaryLocalVariable") final ModelSchema<M> typed = ms.getModelSchema();
                return typed;
            })
            .findAny()
            .orElse(null);
        if (schema == null) {
            throw new IllegalArgumentException("No schema for model type: " + modelType.getSimpleName());
        }
        final ModelSheetAdapter<M> mModelSheetAdapter = adapterFactory.adapterForType(modelType);
        return new SheetStore<>(dbModel, schema, mModelSheetAdapter, namingConvention, userId);
    }
}
