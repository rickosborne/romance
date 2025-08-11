package org.rickosborne.romance.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

@Slf4j
public abstract class ACLIRunner {
    protected static String runCLI(final File cwd, final String... args) {
        final Process probe;
        try {
            probe = new ProcessBuilder(args)
                .directory(cwd)
                .start();
        } catch (IOException e) {
            log.error("Failed to start: {}", String.join(" ", args));
            return null;
        }
        final StringWriter outWriter = new StringWriter();
        final StringWriter errorWriter = new StringWriter();
        try (
            final InputStream probeOut = probe.getInputStream();
            final InputStreamReader reader = new InputStreamReader(probeOut, StandardCharsets.UTF_8)
        ) {
            reader.transferTo(outWriter);
        } catch (IOException e) {
            log.error("Failed to read stdout: {}", String.join(" ", args));
            return null;
        }
        try (
            final InputStream errorStream = probe.getErrorStream();
            final InputStreamReader errorReader = new InputStreamReader(errorStream, StandardCharsets.UTF_8)
        ) {
            errorReader.transferTo(errorWriter);
        } catch (IOException e) {
            log.error("Failed to read stderr: {}", String.join(" ", args));
            return null;
        }
        final String stdOut = outWriter.toString();
        final String stdErr = errorWriter.toString();
        int exitCode = 1;
        try {
            exitCode = probe.waitFor();
        } catch (InterruptedException e) {
            log.error("Failed to wait: {}", String.join(" ", args));
        }
        if (exitCode == 0) {
            return stdOut;
        }
        System.out.println(stdOut);
        System.out.println(stdErr);
        log.warn("Failed with code {}: {}", exitCode, String.join(" ", args));
        return null;
    }
}
