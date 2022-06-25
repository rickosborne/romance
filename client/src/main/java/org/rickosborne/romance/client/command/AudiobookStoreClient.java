package org.rickosborne.romance.client.command;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
    name = "absc",
    mixinStandardHelpOptions = true,
    version = "1.0",
    subcommands = {
        LastCommand.class,
        SearchCommand.class,
        AuthCommand.class,
        DataFromSheetCommand.class,
        AbsUserCommand.class
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
