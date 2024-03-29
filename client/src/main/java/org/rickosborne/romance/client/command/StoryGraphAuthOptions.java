package org.rickosborne.romance.client.command;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

@Slf4j
@Getter
public class StoryGraphAuthOptions {
    @CommandLine.Option(names = "sg-password", description = "SG Password", defaultValue = "${env:STORYGRAPH_PASSWORD:-}")
    private String sgPassword;
    @Setter
    @CommandLine.Option(names = "sg-email", description = "SG Email", defaultValue = "${env:STORYGRAPH_EMAIL:-}")
    private String sgEmail;
}
