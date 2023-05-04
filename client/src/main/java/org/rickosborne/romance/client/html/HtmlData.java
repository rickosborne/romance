package org.rickosborne.romance.client.html;

import java.util.function.BiConsumer;

interface HtmlData<M> {
    BiConsumer<M, HtmlScraper> getSetter();
}
