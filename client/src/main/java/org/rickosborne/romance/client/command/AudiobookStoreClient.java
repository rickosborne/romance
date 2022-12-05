package org.rickosborne.romance.client.command;

import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.logging.LogManager;

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
        FillFromTABSCommand.class,
        StoryGraphFromSheetCommand.class,
        InferCommand.class,
        MoreFromAuthorsCommand.class,
        RedditWDYRCommand.class,
        BookWyrmCoversCommand.class,
        BookWyrmImportCsvFromSheetCommand.class,
        BookWyrmPGSyncCommand.class,
    }
)
public class AudiobookStoreClient implements Callable<Integer> {
    private static final File loggingPropertiesFile = Path.of("logging.properties").toFile();

    public static void main(String[] args) throws IOException {
        if (loggingPropertiesFile.isFile()) {
            LogManager.getLogManager().readConfiguration(new FileInputStream(loggingPropertiesFile));
        }
        System.exit(new CommandLine(new AudiobookStoreClient()).execute(args));
    }

    @Override
    public Integer call() {
        return 0;
    }
}
