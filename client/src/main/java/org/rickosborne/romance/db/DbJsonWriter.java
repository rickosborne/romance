package org.rickosborne.romance.db;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;

public class DbJsonWriter {
    @Getter(lazy = true)
    private final static JsonMapper jsonMapper = JsonMapper.builder()
        .addModule(new JavaTimeModule())
        .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
        .build();

    @Getter(lazy = true)
    private final static ObjectWriter jsonWriter = getJsonMapper()
        .writerWithDefaultPrettyPrinter();
}
