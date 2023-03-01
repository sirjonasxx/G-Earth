package gearth.services.extension_handler.extensions.implementations.network.executer;

import gearth.GEarth;
import gearth.services.extension_handler.extensions.implementations.network.NetworkExtensionAuthenticator;
import gearth.services.internal_extensions.extensionstore.tools.StoreExtensionTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by Jonas on 22/09/18.
 */
public final class NormalExtensionRunner implements ExtensionRunner {

    private final static Logger LOGGER = LoggerFactory.getLogger(NormalExtensionRunner.class);

    public static final String JAR_PATH;

    static {
        final URL url = getLocation();
        String value;
        try {
            value = new File(url.toURI()).getParent();
        } catch (URISyntaxException e) {
            value = new File(url.getPath()).getParent();
            LOGGER.warn("Failed to load JAR_PATH from url {} as URI, using Path instead", url, e);
        }
        JAR_PATH = value;
        LOGGER.debug("Set JAR_PATH as {}", JAR_PATH);
    }

    @Override
    public void runAllExtensions(int port) {

        if (dirExists(ExecutionInfo.EXTENSIONS_DIRECTORY)) {

            final File extensionsDirectory = Paths.get(JAR_PATH, ExecutionInfo.EXTENSIONS_DIRECTORY).toFile();
            final File[] extensionFiles = extensionsDirectory.listFiles();

            if (extensionFiles == null) {
                LOGGER.error("Provided extensionsDirectory does not exist (extensionsDirectory={})", extensionsDirectory);
                return;
            }

            for (File file : extensionFiles)
                tryRunExtension(file.getPath(), port);
        } else
            LOGGER.warn("Did not run extensions because extensions directory does not exist at {}", ExecutionInfo.EXTENSIONS_DIRECTORY);
    }

    @Override
    public void installAndRunExtension(String stringPath, int port) {

        if (!dirExists(ExecutionInfo.EXTENSIONS_DIRECTORY))
            tryCreateDirectory(ExecutionInfo.EXTENSIONS_DIRECTORY);

        final Path path = Paths.get(stringPath);
        final String name = path.getFileName().toString();
        final String[] split = name.split("\\.");
        final String ext = "*." + split[split.length - 1];

        final String realName = String.join(".", Arrays.copyOf(split, split.length - 1));
        final String newName = realName + "-" + getRandomString() + ext.substring(1);

        final Path newPath = Paths.get(JAR_PATH, ExecutionInfo.EXTENSIONS_DIRECTORY, newName);

        try {

            Files.copy(path, newPath);

            tryRunExtension(newPath.toString(), port);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryRunExtension(String path, int port) {
        try {

            if (new File(path).isDirectory()) {
                // this extension is installed from the extension store and requires different behavior
                StoreExtensionTools.executeExtension(path, port);
                return;
            }

            final String filename = Paths.get(path).getFileName().toString();
            final String[] execCommand = ExecutionInfo
                    .getExecutionCommand(getFileExtension(path))
                    .clone();
            final String cookie = NetworkExtensionAuthenticator.generateCookieForExtension(filename);
            for (int i = 0; i < execCommand.length; i++) {
                execCommand[i] = execCommand[i]
                        .replace("{path}", path)
                        .replace("{port}", port + "")
                        .replace("{filename}", filename)
                        .replace("{cookie}", cookie);
            }

            final ProcessBuilder processBuilder = new ProcessBuilder(execCommand);
            final Process process = processBuilder.start();

            maybeLogExtension(path, process);

        } catch (IOException e) {
            LOGGER.error("Failed to run extension at path {} using port {}", path, port, e);
        }
    }


    public static void maybeLogExtension(String path, Process process) {
        if (GEarth.hasFlag(ExtensionRunner.SHOW_EXTENSIONS_LOG)) {

            final Logger logger = LoggerFactory.getLogger(path);

            logger.info("Launching...");

            final BufferedReader processInputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            new Thread(() -> {
                try {
                    String line;
                    while ((line = processInputReader.readLine()) != null)
                        logger.info(line);
                } catch (IOException e) {
                    LOGGER.error("Failed to read input line from process {}", process, e);
                }
            }, path+"-input").start();

            final BufferedReader processErrorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            new Thread(() -> {
                try {
                    String line;
                    while ((line = processErrorReader.readLine()) != null)
                        logger.error(line);
                } catch (IOException e) {
                    LOGGER.error("Failed to read error line from process {}", process, e);
                }
            }).start();
        }
    }

    @Override
    public void uninstallExtension(String filename) {
        try {
            final Path path = Paths.get(JAR_PATH, ExecutionInfo.EXTENSIONS_DIRECTORY, filename);
            if (new File(path.toString()).isDirectory()) {
                // is installed through extension store
                StoreExtensionTools.removeExtension(path.toString());
            } else
                Files.delete(path);
        } catch (IOException e) {
            LOGGER.error("Failed to uninstall extension at {}", filename, e);
        }
    }

    private static void tryCreateDirectory(String path) {
        if (!dirExists(path)) {
            try {
                Files.createDirectories(Paths.get(JAR_PATH, path));
            } catch (IOException e) {
                LOGGER.error("Failed to create directory at {}", path, e);
            }
        }
    }

    private static boolean dirExists(String dir) {
        return Files.isDirectory(Paths.get(JAR_PATH, dir));
    }

    private static URL getLocation() {
        return GEarth.class.getProtectionDomain().getCodeSource().getLocation();
    }

    private static String getFileExtension(String path) {
        final String name = Paths.get(path).getFileName().toString();
        final String[] split = name.split("\\.");
        return "*." + split[split.length - 1];
    }

    private static String getRandomString() {
        final StringBuilder builder = new StringBuilder();
        final Random random = new Random();
        for (int i = 0; i < 12; i++)
            builder.append(random.nextInt(10));
        return builder.toString();
    }
}
