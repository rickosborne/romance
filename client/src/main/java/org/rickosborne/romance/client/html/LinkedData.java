package org.rickosborne.romance.client.html;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.function.BiConsumer;

interface LinkedData<M> {
    String getLdPath();

    default BiConsumer<M, JsonNode> getNodeHandler() {
        return null;
    }

    BiConsumer<M, String> getSetter();
}
