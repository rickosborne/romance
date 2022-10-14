package org.rickosborne.romance.db.model;

import lombok.Getter;

import java.util.List;

public class RedditPostSchema implements ModelSchema<RedditPostModel> {
    @Getter
    private final List<RedditPostAttributes> attributes = List.of(RedditPostAttributes.values());

    @Override
    public RedditPostModel buildModel() {
        return new RedditPostModel();
    }

    @Override
    public List<String> idValuesFromModel(final RedditPostModel model) {
        return List.of(model.getId());
    }
}
