package org.rickosborne.audiobookstore.client;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.junit.jupiter.api.Test;
import org.rickosborne.romance.client.client.AudiobookStoreService;
import org.rickosborne.romance.client.client.response.Login;
import org.rickosborne.romance.client.client.response.UserInformation2;

import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Log
class AudiobookStoreServiceTest {
    @Getter(lazy = true)
    private final AudiobookStoreService service = AudiobookStoreService.build();
    @SuppressWarnings("Convert2Lambda")
    @Getter(lazy = true)
    private final UUID userGuid = (new Supplier<UUID>() {
        @Override
        @SneakyThrows
        public UUID get() {
            final String userName = System.getenv("AUDIOBOOKSTORE_USERNAME");
            assertNotNull(userName, "Required: username");
            final String password = System.getenv("AUDIOBOOKSTORE_PASSWORD");
            assertNotNull(password, "Required: password");
            final Login login = getService().checkLogin(userName, password).execute().body();
            assertNotNull(login, "CheckLogin");
            final UUID userGuid = login.getUserGuid();
            assertNotNull(userGuid, "UserGuid");
            return userGuid;
        }
    }).get();

    @Test
    void checkLogin() {
        final UUID userGuid = getUserGuid();
        assertNotNull(userGuid, "UserGuid");
        log.info(userGuid.toString());
    }

    @Test
    @SneakyThrows
    void userInformation2() {
        final UUID userGuid = getUserGuid();
        final UserInformation2 ui2 = getService().userInformation2(userGuid.toString()).execute().body();
        assertNotNull(ui2, "userInformation2");
        log.info(ui2.toString());
    }
}
