package org.rickosborne.romance.client.command;

import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.client.AudiobookStoreService;
import org.rickosborne.romance.client.response.UserInformation2;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(
    name = "absuser",
    description = "Fetch and display ABS user info"
)
public class AbsUserCommand implements Callable<Integer> {
    @CommandLine.Mixin
    AudiobookStoreAuthOptions auth;

    @Override
    public Integer call() throws Exception {
        final AudiobookStoreService storeService = AudiobookStoreService.build();
        auth.ensureAuthGuid(storeService);
        final UserInformation2 userInfo = storeService.userInformation2(auth.getAbsUserGuid().toString()).execute().body();
        System.out.println(userInfo);
        return 0;
    }
}
