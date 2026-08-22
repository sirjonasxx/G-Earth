package gearth.app.protocol.memory.habboclient.external;

import gearth.app.encoding.HexEncoding;
import gearth.app.misc.OSValidator;
import gearth.app.protocol.HConnection;
import gearth.protocol.connection.HClient;
import gearth.app.protocol.memory.habboclient.HabboClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class MemoryClient implements HabboClient {

    private static final Logger logger = LoggerFactory.getLogger(MemoryClient.class);
    private static final String GMEM_PATH_PROPERTY = "gearth.gmem.path";
    private static final String GMEM_PATH_ENVIRONMENT = "GEARTH_GMEM_PATH";

    private final HConnection connection;

    public MemoryClient(HConnection connection) {
        this.connection = connection;
    }

    @Override
    public List<byte[]> getRC4Tables() {
        final List<byte[]> result = new ArrayList<>();

        try {
            final HashSet<String> potentialTables = dumpTables();

            for (String potentialTable : potentialTables) {
                result.add(HexEncoding.toBytes(potentialTable));
            }
        } catch (IOException | URISyntaxException e) {
            logger.error("Failed to read RC4 possibilities from the client", e);
        }

        // Reverse the list so that the most likely keys are at the top.
        Collections.reverse(result);

        return result;
    }

    private HashSet<String> dumpTables() throws IOException, URISyntaxException {
        File applicationDirectory = new File(
                this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
        ).getParentFile();
        if ("Java".equals(applicationDirectory.getName())
                && applicationDirectory.getParentFile() != null) {
            applicationDirectory = applicationDirectory.getParentFile();
        }
        File executable;

        if (OSValidator.isWindows()) {
            // Detect Windows 32 or 64 bit
            if (System.getProperty("os.arch").contains("64")) {
                executable = new File(applicationDirectory, "G-MemZ-x64.exe");
            } else {
                executable = new File(applicationDirectory, "G-MemZ-x32.exe");
            }
        } else {
            String configuredPath = System.getProperty(GMEM_PATH_PROPERTY);
            if (configuredPath == null || configuredPath.trim().isEmpty()) {
                configuredPath = System.getenv(GMEM_PATH_ENVIRONMENT);
            }
            executable = configuredPath == null || configuredPath.trim().isEmpty()
                    ? new File(applicationDirectory, "G-MemZ")
                    : new File(configuredPath);
        }
        ensureExecutable(executable);

        final String hotelType = connection.getClientType() == HClient.SHOCKWAVE ? "shockwave" : "flash";
        final ProcessBuilder pb = OSValidator.isMac()
                ? new ProcessBuilder(
                        executable.getAbsolutePath(),
                        connection.getClientHost(),
                        Integer.toString(connection.getClientPort())
                )
                : new ProcessBuilder(executable.getAbsolutePath(), hotelType);
        pb.directory(applicationDirectory);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        final Process p = pb.start();

        final HashSet<String> possibleData = new HashSet<>();

        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;

            while ((line = reader.readLine()) != null) {
                logger.info("G-MemZ: {}", line);

                // 2048 = Flash     (256 * 4 * 2)
                // 4096 = Shockwave (512 * 4 * 2)
                final int lineLength = line.length();

                if (lineLength == 2048 || lineLength == 4096) {
                    possibleData.add(line);
                }
            }
            final int exitCode;
            try {
                exitCode = p.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for G-MemZ", e);
            }
            if (exitCode != 0) {
                throw new IOException("G-MemZ exited with code " + exitCode
                        + ". On macOS, verify the helper signature and Habbo's get-task-allow entitlement.");
            }
        } finally {
            p.destroy();
        }

        return possibleData;
    }

    private static void ensureExecutable(File executable) throws IOException {
        if (!executable.isFile()) {
            throw new IOException("G-MemZ was not found: " + executable.getAbsolutePath());
        }
        if (!executable.canExecute() && !executable.setExecutable(true, true)) {
            throw new IOException("G-MemZ is not executable: " + executable.getAbsolutePath());
        }
    }
}
