package org.rickosborne.romance.db.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.net.URL;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RedditPostModel {
    String authorName;
    String body;
    String id;
    URL url;
}
