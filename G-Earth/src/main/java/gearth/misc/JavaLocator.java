package gearth.misc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class to find Java 8 installations with build 441 or lower.<br>
 * <br>
 * Java 8u441 is the last Java 8 version that has JavaFX.
 * G-Earth was updated to a newer Java version so users need to
 * install Java 8 separately for older extensions that require JavaFX.<br>
 * <br>
 * <a href="https://www.oracle.com/java/technologies/javase/8u441-relnotes.html">8u441 release notes</a>
 */
public class JavaLocator {

    private final static Logger LOG = LoggerFactory.getLogger(JavaLocator.class);

    private final static String JAVA_MAJOR = "1.8.0";
    private final static int JAVA_BUILD_MAX = 441;

    private static boolean initialized;
    private static JavaInstall javaInstall;

    public static JavaInstall findJava8() {
        if (!initialized) {
            try {
                // Find java installs.
                List<JavaInstall> javaInstalls;

                if (OSValidator.isWindows()) {
                    javaInstalls = findOnWindows();
                } else if (OSValidator.isMac()) {
                    javaInstalls = findOnMac();
                } else {
                    javaInstalls = new ArrayList<>();
                }

                // Filter java installs.
                javaInstall = filterJavaInstalls(javaInstalls);
            } catch (IOException e) {
                LOG.error("Error while searching for Java installations", e);
            }

            initialized = true;
        }

        return javaInstall;
    }

    private static JavaInstall filterJavaInstalls(List<JavaInstall> javaInstalls) {
        final Map<Integer, JavaInstall> buildMap = javaInstalls
                .stream()
                .filter(install -> install.version().startsWith(JAVA_MAJOR))
                .filter(install -> install.version().contains("_"))
                .filter(install -> {
                    try {
                        final int buildNumber = Integer.parseInt(install.version().split("_")[1]);
                        return buildNumber <= JAVA_BUILD_MAX;
                    } catch (NumberFormatException e) {
                        LOG.warn("Java version {} does not contain a valid build number", install.version());
                        return false;
                    }
                })
                // jre before jdk.
                .sorted((o1, o2) -> {
                    final String folderName1 = o1.path().getFileName().toString();
                    final String folderName2 = o2.path().getFileName().toString();

                    if (folderName1.contains("jre") && folderName2.contains("jdk")) {
                        return 1;
                    } else if (folderName1.contains("jdk") && folderName2.contains("jre")) {
                        return -1;
                    } else {
                        return 0;
                    }
                })
                .collect(Collectors.toMap(
                        install -> Integer.parseInt(install.version().split("_")[1]),
                        install -> install,
                        (existing, replacement) -> existing));

        // Get the highest build number.
        final Optional<Integer> maxBuild = buildMap.keySet()
                .stream()
                .max(Integer::compareTo);

        if (maxBuild.isEmpty()) {
            LOG.warn("No valid Java installation found");
            return null;
        }

        return buildMap.get(maxBuild.get());
    }

    private static List<JavaInstall> findOnWindows() throws IOException {
        final List<JavaInstall> results = new ArrayList<>();

        final String osArch = System.getProperty("os.arch");
        final String systemDrive = System.getenv("SystemDrive");

        final Path programFiles = osArch.contains("64")
                ? Path.of(systemDrive, "Program Files", "Java")
                : Path.of(systemDrive, "Program Files (x86)", "Java");

        LOG.info("Searching for Java installations on Windows in {}", programFiles);

        if (!Files.exists(programFiles)) {
            LOG.warn("No java installation not found in {}", programFiles);
            return results;
        }

        try (final Stream<Path> files = Files.list(programFiles)) {
            final Iterator<Path> dirIt = files
                    .filter(Files::isDirectory)
                    .filter(path -> !path.endsWith("latest"))
                    .iterator();

            while (dirIt.hasNext()) {
                final Path path = dirIt.next();

                // Read "release" file.
                final Path releaseFile = path.resolve("release");

                if (!Files.exists(releaseFile)) {
                    LOG.warn("No release file found in {}", path);
                    continue;
                }

                final List<String> releaseData = Files.readAllLines(releaseFile);

                // Find JAVA_VERSION.
                final Optional<String> javaVersion = releaseData
                        .stream()
                        .filter(x -> x.startsWith("JAVA_VERSION"))
                        .findFirst();

                if (javaVersion.isEmpty()) {
                    LOG.warn("No JAVA_VERSION found in {}", releaseFile);
                    continue;
                }

                // i.e. JAVA_VERSION="17.0.12"
                String parsedVersion = javaVersion.get().replace("JAVA_VERSION=", "");

                // Remove quotes.
                parsedVersion = parsedVersion.substring(1, parsedVersion.length() - 1);

                LOG.info("Found Java installation {} at {}", parsedVersion, path);

                results.add(new JavaInstall(path, parsedVersion));
            }
        }

        return results;
    }

    private static List<JavaInstall> findOnMac() throws IOException {
        LOG.info("Searching for Java installations on MacOS");

        return new ArrayList<>();
    }

}
