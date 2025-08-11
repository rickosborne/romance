package org.rickosborne.romance.client.audiobookshelf;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.Objects;

@Slf4j
@Getter
public class AudiobookShelfAuthOptions {
    @CommandLine.Option(names = {"--shelf-token"})
    private String apiToken;
    @CommandLine.Option(names = {"--shelf-host"})
    private String hostName;
    @CommandLine.Option(names = {"--shelf-port"}, defaultValue = "13378")
    private int port = 13378;
    @CommandLine.Option(names = {"--shelf-secure"}, defaultValue = "false")
    private boolean secure = false;

    public AudiobookShelfConfig toConfig() {
        return new AudiobookShelfConfig(apiToken, hostName, port, secure);
    }

    public void validate() {
        Objects.requireNonNull(apiToken, "--shelf-token");
        Objects.requireNonNull(hostName, "--shelf-host");
    }
}
