package org.rickosborne.audiobookstore.client.cli;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
    name = "absc",
    mixinStandardHelpOptions = true,
    version = "1.0",
    subcommands = {
        LastCommand.class,
        SearchCommand.class,
        AuthCommand.class
    }
)
public class AudiobookStoreClient implements Callable<Integer> {

    @Override
    public Integer call() {
        return 0;
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new AudiobookStoreClient()).execute(args));
    }
}
