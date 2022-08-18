package org.rickosborne.romance.client.command;

import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.client.html.StoryGraphHtml;
import picocli.CommandLine;

import java.util.Objects;

@Slf4j
@CommandLine.Command(
    name = "sg-from-sheet",
    description = "Synchronize StoryGraph from Google Sheet"
)
public class StoryGraphFromSheetCommand extends ASheetCommand {
    @Override
    protected Integer doWithSheets() {
        final String sgEmail = getSgAuth().getSgEmail();
        final String sgPassword = getSgAuth().getSgPassword();
        Objects.requireNonNull(sgEmail, "SG email is required");
        Objects.requireNonNull(sgPassword, "SG password is required");
        final StoryGraphHtml.Session session = getStoryGraphHtml().ensureSignedIn(null, sgEmail, sgPassword);
        log.info("Signed into StoryGraph");
        return 0;
    }
}
