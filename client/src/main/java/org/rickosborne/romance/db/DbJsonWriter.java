package org.rickosborne.romance.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.NonNull;

import java.io.File;
import java.io.IOException;

public class DbJsonWriter {
    @Getter(lazy = true)
    private final static JsonMapper jsonMapper = JsonMapper.builder()
        .addModule(new JavaTimeModule())
        .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
        .build();

    @Getter(lazy = true)
    private final static ObjectWriter jsonWriter = getJsonMapper()
        .writerWithDefaultPrettyPrinter();

    public static <T> T readFile(@NonNull final File file, @NonNull final Class<T> type) {
        try {
            return getJsonMapper().readValue(file, type);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not load " + type.getSimpleName() + "#" + file, e);
        }
    }

    public static JsonNode readTree(@NonNull final String json) {
        try {
            return getJsonMapper().readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
