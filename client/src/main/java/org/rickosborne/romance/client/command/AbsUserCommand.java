package org.rickosborne.romance.client.command;

import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.client.AudiobookStoreService;
import org.rickosborne.romance.client.response.UserInformation2;
import org.rickosborne.romance.db.DbJsonWriter;
import picocli.CommandLine;

import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(
    name = "absuser",
    description = "Fetch and display ABS user info"
)
public class AbsUserCommand implements Callable<Integer> {
    @CommandLine.Mixin
    AudiobookStoreAuthOptions auth;

    @CommandLine.Option(names = "--out-path")
    Path outPath;

    @Override
    public Integer call() throws Exception {
        final AudiobookStoreService storeService = AudiobookStoreService.build();
        auth.ensureAuthGuid(storeService);
        final UserInformation2 userInfo = storeService.userInformation2(auth.getAbsUserGuid().toString()).execute().body();
        final String json = DbJsonWriter.getJsonWriter().writeValueAsString(userInfo);
        System.out.println(json);
        if (outPath != null) {
            try (final FileWriter fileWriter = new FileWriter(outPath.toFile(), StandardCharsets.UTF_8)) {
                fileWriter.write(json);
            }
        }
        return 0;
    }
}
