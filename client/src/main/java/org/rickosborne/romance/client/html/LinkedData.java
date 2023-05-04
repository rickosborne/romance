package org.rickosborne.romance.client.html;

import java.util.function.BiConsumer;

interface LinkedData<M> {
    String getLdPath();

    BiConsumer<M, String> getSetter();
}
