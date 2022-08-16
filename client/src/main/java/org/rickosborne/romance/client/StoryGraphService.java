package org.rickosborne.romance.client;

import org.rickosborne.romance.StoryGraph;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public interface StoryGraphService {
    static StoryGraphService build() {
        return new Retrofit.Builder()
            .baseUrl(StoryGraph.API_BASE)
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
            .create(StoryGraphService.class);
    }
}
