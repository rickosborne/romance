package org.rickosborne.romance.db.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbJsonWriter;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.ModelStore;
import org.rickosborne.romance.db.model.ModelSchema;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
@Getter(value = AccessLevel.PROTECTED)
@RequiredArgsConstructor
public class JsonStore<M> implements ModelStore<M>, Iterable<M> {
    public static final String FILE_EXT = ".json";
    @Getter
    private final DbModel dbModel;
    @Getter(lazy = true, value = AccessLevel.PROTECTED)
    private final ObjectMapper jsonMapper = DbJsonWriter.getJsonMapper();
    @Getter(lazy = true, value = AccessLevel.PROTECTED)
    private final ObjectWriter jsonWriter = DbJsonWriter.getJsonWriter();
    @Getter
    private final ModelSchema<M> modelSchema;
    @Getter
    private final Class<M> modelType;
    private final NamingConvention namingConvention;
    private final Path typePath;

    protected File fileForId(@NonNull final String id) {
        final String fileName = namingConvention.fileNameFromTexts(id) + FILE_EXT;
        return typePath.resolve(fileName).toFile();
    }

    protected File fileForModel(@NonNull final M model) {
        final String id = idFromModel(model);
        if (id == null) {
            return null;
        }
        return fileForId(id);
    }

    @Override
    public M findById(final String id) {
        return findByIdFromCache(id);
    }

    @Override
    public M findByIdFromCache(final String id) {
        final File file = fileForId(id);
        return loadFromFile(file);
    }

    @Override
    public String idFromModel(final M model) {
        final List<String> parts = getModelSchema().idValuesFromModel(model);
        if (parts.isEmpty()) {
            return null;
        }
        return getNamingConvention().fileNameFromTexts(
            parts.stream()
        );
    }

    @Override
    public Iterator<M> iterator() {
        int[] at = {0};
        final File[] files = listFiles();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return at[0] < files.length;
            }

            @Override
            public M next() {
                return loadFromFile(files[at[0]++]);
            }
        };
    }

    private File[] listFiles() {
        return typePath.toFile().listFiles(fn -> fn.isFile() && fn.getName().endsWith(FILE_EXT));
    }

    private M loadFromFile(@NonNull final File file) {
        if (file.isFile()) {
            try {
                return getJsonMapper().readValue(file, modelType);
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not load " + modelType.getSimpleName() + "#" + file, e);
            }
        }
        return null;
    }

    @Override
    public M save(final M model) {
        final File modelFile = fileForModel(model);
        if (modelFile == null) {
            throw new NullPointerException("Cannot save incomplete " + getModelType().getSimpleName() + ": " + model);
        }
        try {
            if (!typePath.toFile().exists()) {
                if (typePath.toFile().mkdirs()) {
                    log.debug("Created directory: " + typePath);
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

    @Override
    public Stream<M> stream() {
        final File[] files = listFiles();
        if (files == null || files.length == 0) {
            return Stream.empty();
        }
        return Stream.of(files).map(this::loadFromFile).filter(Objects::nonNull);
    }
}
