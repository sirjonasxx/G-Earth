package gearth.app.protocol.portchecker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Uses macOS' built-in {@code lsof} to identify a TCP listener. */
public class MacOsPortChecker implements PortChecker {

    private static final String LSOF = "/usr/sbin/lsof";

    @Override
    public String getProcessUsingPort(int port) throws IOException {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }

        Process process = new ProcessBuilder(
                LSOF,
                "-nP",
                "-iTCP:" + port,
                "-sTCP:LISTEN",
                "-Fpc"
        ).redirectErrorStream(true).start();

        String processName = null;
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
                if (processName == null && line.length() > 1 && line.charAt(0) == 'c') {
                    processName = line.substring(1);
                }
            }
        }

        final int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while checking port " + port, e);
        }

        // lsof returns 1 when the query has no matches.
        if (exitCode > 1) {
            throw new IOException("lsof failed with exit code " + exitCode + ": " + output.toString().trim());
        }

        return processName;
    }
}
