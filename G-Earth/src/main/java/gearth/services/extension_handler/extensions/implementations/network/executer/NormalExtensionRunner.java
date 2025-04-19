package gearth.services.extension_handler.extensions.implementations.network.executer;

import gearth.GEarth;
import gearth.misc.OSValidator;
import gearth.services.extension_handler.extensions.implementations.network.NetworkExtensionAuthenticator;
import gearth.services.internal_extensions.extensionstore.tools.StoreExtensionTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Created by Jonas on 22/09/18.
 */
public final class NormalExtensionRunner implements ExtensionRunner {

    private final static Logger LOG = LoggerFactory.getLogger(NormalExtensionRunner.class);

    public static final String JAR_PATH;

    static {
        final URL url = getLocation();
        String value;
        try {
            value = new File(url.toURI()).getParent();
        } catch (URISyntaxException e) {
            value = new File(url.getPath()).getParent();
            LOG.warn("Failed to load JAR_PATH from url {} as URI, using Path instead", url, e);
        }
        JAR_PATH = value;
        LOG.debug("Set JAR_PATH as {}", JAR_PATH);
    }

    @Override
    public void runAllExtensions(int port) {

        if (dirExists(ExecutionInfo.EXTENSIONS_DIRECTORY)) {

            final File extensionsDirectory = Paths.get(JAR_PATH, ExecutionInfo.EXTENSIONS_DIRECTORY).toFile();
            final File[] extensionFiles = extensionsDirectory.listFiles();

            if (extensionFiles == null) {
                LOG.error("Provided extensionsDirectory does not exist (extensionsDirectory={})", extensionsDirectory);
                return;
            }

            for (File file : extensionFiles)
                tryRunExtension(file.getPath(), port);
        } else
            LOG.warn("Did not run extensions because extensions directory does not exist at {}", ExecutionInfo.EXTENSIONS_DIRECTORY);
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
            LOG.error("Failed to copy extension from {} to {}", path, newPath, e);
        }
    }

    public void tryRunExtension(String path, int port) {
        LOG.info("Running extension {}", path);

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

            final List<String> execCommandList = Arrays.asList(execCommand);

            NormalExtensionRunner.locateJavaForJar(execCommandList, null, new File(path));

            final ProcessBuilder processBuilder = new ProcessBuilder(execCommandList);
            final Process process = startProcess(processBuilder);

            final String installedExtensionId = Paths.get(path).getFileName().toString();

            logExtension(installedExtensionId, path, process);
        } catch (IOException e) {
            LOG.error("Failed to run extension at path {} using port {}", path, port, e);
        }
    }

    public static Process startProcess(ProcessBuilder processBuilder) throws IOException {
        return processBuilder.start();
    }

    public static void logExtension(final String name, String path, Process process) {
        final Logger logger = LoggerFactory.getLogger("gearth.extension");

        logger.info("Launching {}...", name);

        final BufferedReader processInputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        new Thread(() -> {
            MDC.put("extensionName", name);

            try {
                String line;
                while ((line = processInputReader.readLine()) != null)
                    logger.info(line);
            } catch (IOException e) {
                LOG.error("Failed to read input line from process {}", process, e);
            }
        }, path+"-input").start();

        final BufferedReader processErrorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        new Thread(() -> {
            MDC.put("extensionName", name);

            try {
                String line;
                while ((line = processErrorReader.readLine()) != null)
                    logger.error(line);
            } catch (IOException e) {
                LOG.error("Failed to read error line from process {}", process, e);
            }
        }).start();
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
            LOG.error("Failed to uninstall extension at {}", filename, e);
        }
    }

    private static void tryCreateDirectory(String path) {
        if (!dirExists(path)) {
            try {
                Files.createDirectories(Paths.get(JAR_PATH, path));
            } catch (IOException e) {
                LOG.error("Failed to create directory at {}", path, e);
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

    public static void locateJavaForJar(List<String> command, File workingDir, File jarFile) {
        if (!command.get(0).equals("java")) {
            return;
        }

        // Obtain jar file for store extension.
        if (jarFile == null) {
            if (!command.get(1).equals("-jar")) {
                return;
            }

            jarFile = new File(workingDir, command.get(2));
        }

        // Check if the jar file is a Java 8 extension.
        if (!isJava8AndJavaFX(jarFile)) {
            LOG.debug("Jar file {} is not a Java 8 extension, no need to locate Java 1.8", jarFile);
            return;
        }

        final File currentJavaPath = new File(System.getProperty("java.home"));
        final File javaInstallPath = currentJavaPath.getParentFile();

        LOG.info("Current java.home is {}", currentJavaPath);
        LOG.info("Looking for other java installs in {}", javaInstallPath);

        // Find a folder that starts with "jre1.8" or "jdk1.8".
        final File[] javaVersions = javaInstallPath.listFiles((dir, name) -> name.startsWith("jre1.8") || name.startsWith("jdk1.8"));

        if (javaVersions == null || javaVersions.length == 0) {
            LOG.warn("No java 1.8 installation to run extension jar file {}", jarFile);
            return;
        }

        // Prefer jre1.8 over jdk1.8.
        final File javaPath = Arrays.stream(javaVersions).min((o1, o2) -> {
            if (o1.getName().startsWith("jre1.8")) {
                return -1;
            } else if (o2.getName().startsWith("jre1.8")) {
                return 1;
            } else {
                return 0;
            }
        }).orElse(javaVersions[0]);

        // Change command to use the java executable from the found folder.
        LOG.info("Using java at {} to run extension jar file {}", javaPath, jarFile);

        command.set(0, new File(javaPath, OSValidator.isWindows() ? "bin/java.exe" : "bin/java").getAbsolutePath());
    }

    /**
     * Checks if the jar file is a Java 8 extension and requires JavaFX.
     * @param jarFile the jar file to check
     * @return true if the jar file is a Java 8 extension and requires JavaFX, false otherwise
     */
    private static boolean isJava8AndJavaFX(final File jarFile) {
        try (final InputStream inputStream = new FileInputStream(jarFile);
             final JarInputStream jarInputStream = new JarInputStream(inputStream, false)) {
            // Read "META-INF/MANIFEST.MF" file and check for "Build-Jdk" attribute.
            final Manifest manifest = jarInputStream.getManifest();
            final Attributes attributes = manifest.getMainAttributes();
            final String attribute = attributes.getValue("Build-Jdk");

            // Return false if we are not a java 8 extension.
            if (attribute != null && !attribute.startsWith("1.8")) {
                return false;
            }

            // Check for JavaFX.
            boolean hasJavaFX = false;

            // Check if jar file has any .fxml file.
            while (true) {
                final ZipEntry entry = jarInputStream.getNextEntry();

                if (entry == null) {
                    break;
                }

                if (entry.getName().endsWith(".fxml")) {
                    hasJavaFX = true;
                    break;
                }
            }

            return hasJavaFX;
        } catch (IOException e) {
            LOG.error("Failed to read jar file {}", jarFile, e);
        }

        return false;
    }
}
