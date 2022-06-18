package org.rickosborne.romance.db.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.net.URL;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class WatchModel {
    private String bookTitle;
    private String authorName;
    private URL goodreadsUrl;
}
