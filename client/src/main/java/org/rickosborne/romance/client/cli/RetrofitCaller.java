package org.rickosborne.romance.client.cli;

import lombok.extern.java.Log;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

@Log
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
