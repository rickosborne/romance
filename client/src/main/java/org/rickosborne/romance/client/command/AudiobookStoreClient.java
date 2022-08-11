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
        AbsUserCommand.class,
        DataFromAudiobookStoreCommand.class,
        PreordersCommand.class,
        WishlistCommand.class,
        FillFromTABSCommand.class
    }
)
public class AudiobookStoreClient implements Callable<Integer> {

    public static void main(String[] args) {
        System.exit(new CommandLine(new AudiobookStoreClient()).execute(args));
    }

    @Override
    public Integer call() {
        return 0;
    }
}
