package org.rickosborne.romance.db.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URL;
import java.util.Date;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@NoArgsConstructor
@AllArgsConstructor
public class RedditPostModel {
    String authorName;
    String body;
    List<RedditPostModel> comments;
    String id;
    String parentFullName;
    int score;
    Date updated;
    URL url;
}
