package org.rickosborne.romance.client.command;

import lombok.extern.java.Log;
import org.rickosborne.romance.client.AudiobookStoreService;
import org.rickosborne.romance.client.response.UserInformation2;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@Log
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
