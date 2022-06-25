package org.rickosborne.romance.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.model.ModelSchema;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@Log
@Getter(value = AccessLevel.PROTECTED)
@RequiredArgsConstructor
public abstract class JsonStore<M> implements ModelStore<M> {
    @Getter
    private final DbModel dbModel;
    @Getter(lazy = true, value = AccessLevel.PROTECTED)
    private final ObjectMapper jsonMapper = DbJsonWriter.getJsonMapper();
    @Getter(lazy = true, value = AccessLevel.PROTECTED)
    private final ObjectWriter jsonWriter = DbJsonWriter.getJsonWriter();
    private final ModelSchema<M> modelSchema;
    @Getter
    private final Class<M> modelType;
    private final NamingConvention namingConvention;
    private final Path typePath;

    protected File fileForId(@NonNull final String id) {
        final String fileName = namingConvention.fileNameFromTexts(id) + ".json";
        return typePath.resolve(fileName).toFile();
    }

    protected File fileForModel(@NonNull final M model) {
        return fileForId(idFromModel(model));
    }

    @Override
    public M findById(final String id) {
        return findByIdFromCache(id);
    }

    @Override
    public M findByIdFromCache(final String id) {
        try {
            final File file = fileForId(id);
            if (file.isFile()) {
                return getJsonMapper().readValue(file, modelType);
            }
            return null;
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not load " + modelType.getSimpleName() + "#" + id, e);
        }
    }

    @Override
    public String idFromModel(final M model) {
        return getNamingConvention().fileNameFromTexts(
            getModelSchema().idValuesFromModel(model).stream()
        );
    }

    @Override
    public M save(final M model) {
        final File modelFile = fileForModel(model);
        try {
            if (!typePath.toFile().exists()) {
                if (typePath.toFile().mkdirs()) {
                    log.fine("Created directory: " + typePath);
                }
            }
            getJsonWriter().writeValue(modelFile, model);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not serialize or save " + model.getClass().getSimpleName(), e);
        }
        return model;
    }

    protected String serializeModel(@NonNull final M model) {
        try {
            return getJsonWriter().writeValueAsString(model);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize " + model.getClass().getSimpleName(), e);
        }
    }
}
