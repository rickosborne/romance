package org.rickosborne.romance.client.command;

import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

@Slf4j
public class RetrofitCaller {
    public static <T> T fetchOrNull(final Call<T> callable) {
        try {
            final Response<T> response = callable.execute();
            return response.body();
        } catch (IOException e) {
            return null;
        }
    }
}
