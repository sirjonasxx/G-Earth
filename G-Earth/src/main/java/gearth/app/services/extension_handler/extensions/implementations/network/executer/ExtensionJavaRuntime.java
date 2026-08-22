package gearth.app.services.extension_handler.extensions.implementations.network.executer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves the Java 8 runtime used by legacy network extensions.
 *
 * <p>G-Earth itself intentionally runs on Java 17 through 21. Extensions are
 * separate processes, so they can use Java 8 (and its bundled JavaFX 8) without
 * downgrading the main application.</p>
 */
public final class ExtensionJavaRuntime {

    public static final String CHECK_ARGUMENT = "--check-extension-java";
    public static final String ENVIRONMENT_VARIABLE = "GEARTH_EXTENSION_JAVA";
    public static final String SYSTEM_PROPERTY = "gearth.extension.java";
    public static final String RUNTIME_DIRECTORY = "ExtensionRuntime";

    private static final int REQUIRED_JAVA_MAJOR = 8;
    private static final long VERSION_TIMEOUT_SECONDS = 10;
    private static final Pattern JAVA_8_VERSION = Pattern.compile(
            "version\\s+\"1\\.8\\.0(?:[_-](\\d+))?[^\"]*\"",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern MODERN_JAVA_VERSION = Pattern.compile(
            "version\\s+\"(\\d+)(?:\\.[^\"]*)?\"",
            Pattern.CASE_INSENSITIVE
    );
    private static final Map<Path, RuntimeInfo> RUNTIME_CACHE = new ConcurrentHashMap<>();

    private ExtensionJavaRuntime() {
    }

    /**
     * Creates a process builder and replaces an unqualified {@code java} or
     * {@code javaw} command with the selected Java 8 executable.
     */
    public static ProcessBuilder createProcessBuilder(List<String> command, Path applicationDirectory)
            throws IOException {
        if (command == null || command.isEmpty()) {
            throw new IOException("The extension command is empty.");
        }

        List<String> resolvedCommand = new ArrayList<>(command);
        String requestedJavaCommand = resolvedCommand.get(0);
        if (!isUnqualifiedJavaCommand(requestedJavaCommand)) {
            return new ProcessBuilder(resolvedCommand);
        }

        RuntimeInfo runtime = resolve(applicationDirectory);
        Path executable = runtime.getJavaExecutable();
        if (isJavawCommand(requestedJavaCommand)) {
            Path javaw = executable.resolveSibling(isWindows() ? "javaw.exe" : "javaw");
            if (Files.isRegularFile(javaw)) {
                executable = javaw;
            }
        }
        resolvedCommand.set(0, executable.toString());

        ProcessBuilder processBuilder = new ProcessBuilder(resolvedCommand);
        Map<String, String> environment = processBuilder.environment();
        environment.put("JAVA_HOME", runtime.getJavaHome().toString());
        environment.put("GEARTH_EXTENSION_JAVA_SELECTED", runtime.getJavaExecutable().toString());
        String currentPath = environment.getOrDefault("PATH", "");
        String runtimeBin = runtime.getJavaExecutable().getParent().toString();
        environment.put("PATH", currentPath.isEmpty()
                ? runtimeBin
                : runtimeBin + File.pathSeparator + currentPath);
        return processBuilder;
    }

    public static ProcessBuilder createProcessBuilder(String[] command, Path applicationDirectory)
            throws IOException {
        return createProcessBuilder(Arrays.asList(command), applicationDirectory);
    }

    /** Resolves and validates a Java 8 runtime for the supplied G-Earth directory. */
    public static RuntimeInfo resolve(Path applicationDirectory) throws IOException {
        Path normalizedDirectory = applicationDirectory.toAbsolutePath().normalize();
        RuntimeInfo cached = RUNTIME_CACHE.get(normalizedDirectory);
        if (cached != null) {
            return cached;
        }

        RuntimeInfo resolved = resolveUncached(normalizedDirectory);
        RUNTIME_CACHE.put(normalizedDirectory, resolved);
        return resolved;
    }

    /** Locates the directory containing the running G-Earth JAR or classes. */
    public static Path applicationDirectory(Class<?> anchor) throws IOException {
        URL location = anchor.getProtectionDomain().getCodeSource().getLocation();
        try {
            Path path = Paths.get(location.toURI()).toAbsolutePath().normalize();
            Path directory = Files.isDirectory(path) ? path : path.getParent();
            return normalizePackagedApplicationDirectory(directory);
        } catch (URISyntaxException | IllegalArgumentException e) {
            Path path = Paths.get(location.getPath()).toAbsolutePath().normalize();
            Path directory = Files.isDirectory(path) ? path : path.getParent();
            return normalizePackagedApplicationDirectory(directory);
        }
    }

    private static Path normalizePackagedApplicationDirectory(Path directory) {
        if (directory != null
                && directory.getFileName() != null
                && "Java".equals(directory.getFileName().toString())
                && directory.getParent() != null) {
            return directory.getParent();
        }
        return directory;
    }

    private static RuntimeInfo resolveUncached(Path applicationDirectory) throws IOException {
        String propertyValue = trimToNull(System.getProperty(SYSTEM_PROPERTY));
        if (propertyValue != null) {
            return resolveExplicit(propertyValue, "system property " + SYSTEM_PROPERTY);
        }

        String environmentValue = trimToNull(System.getenv(ENVIRONMENT_VARIABLE));
        if (environmentValue != null) {
            return resolveExplicit(environmentValue, "environment variable " + ENVIRONMENT_VARIABLE);
        }

        String java8Home = trimToNull(System.getenv("JAVA8_HOME"));
        if (java8Home != null) {
            return resolveExplicit(java8Home, "environment variable JAVA8_HOME");
        }

        Path bundledRoot = applicationDirectory.resolve(RUNTIME_DIRECTORY);
        List<Path> bundledCandidates = executableCandidates(bundledRoot);
        if (!bundledCandidates.isEmpty()) {
            RuntimeInfo bundled = firstJava8(bundledCandidates, "bundled " + RUNTIME_DIRECTORY).orElse(null);
            if (bundled != null) {
                return bundled;
            }
            throw new IOException("A runtime exists in " + bundledRoot
                    + " but it is not Java 8. Replace it with a Java 8 runtime (JavaFX 8 recommended)." );
        }

        LinkedHashMap<Path, String> candidates = new LinkedHashMap<>();
        addLocation(candidates, trimToNull(System.getenv("JAVA_HOME")), "JAVA_HOME");
        addKnownInstallations(candidates);
        findJavaOnPath().ifPresent(path -> candidates.put(path, "PATH"));

        for (Map.Entry<Path, String> candidate : candidates.entrySet()) {
            Optional<RuntimeInfo> runtime = probe(candidate.getKey(), candidate.getValue());
            if (runtime.isPresent() && runtime.get().getMajorVersion() == REQUIRED_JAVA_MAJOR) {
                return runtime.get();
            }
        }

        throw new IOException(
                "No Java 8 runtime was found for extensions. G-Earth itself can remain on Java 21. "
                        + "Set " + ENVIRONMENT_VARIABLE + " to a Java 8 executable/JRE home, or place "
                        + "a Java 8 runtime in " + bundledRoot + ". JavaFX 8 is recommended for graphical extensions."
        );
    }

    private static RuntimeInfo resolveExplicit(String configuredPath, String source) throws IOException {
        List<Path> candidates = executableCandidates(expandHome(configuredPath));
        if (candidates.isEmpty()) {
            throw new IOException("The Java runtime configured by " + source + " does not contain a java executable: "
                    + configuredPath);
        }

        for (Path candidate : candidates) {
            Optional<RuntimeInfo> runtime = probe(candidate, source);
            if (runtime.isPresent()) {
                if (runtime.get().getMajorVersion() != REQUIRED_JAVA_MAJOR) {
                    throw new IOException(source + " points to " + runtime.get().getVersionLine()
                            + "; legacy extensions require Java 8.");
                }
                return runtime.get();
            }
        }
        throw new IOException("Unable to execute the Java runtime configured by " + source + ": " + configuredPath);
    }

    private static Optional<RuntimeInfo> firstJava8(List<Path> candidates, String source) {
        for (Path candidate : candidates) {
            Optional<RuntimeInfo> runtime = probe(candidate, source);
            if (runtime.isPresent() && runtime.get().getMajorVersion() == REQUIRED_JAVA_MAJOR) {
                return runtime;
            }
        }
        return Optional.empty();
    }

    private static Optional<RuntimeInfo> probe(Path executable, String source) {
        Path normalizedExecutable;
        try {
            normalizedExecutable = executable.toRealPath();
        } catch (IOException e) {
            normalizedExecutable = executable.toAbsolutePath().normalize();
        }

        Process process = null;
        try {
            process = new ProcessBuilder(normalizedExecutable.toString(), "-version")
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(VERSION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return Optional.empty();
            }
            String output = readFully(process.getInputStream()).trim();
            ParsedVersion version = parseVersion(output);
            if (process.exitValue() != 0 || version == null) {
                return Optional.empty();
            }

            Path javaHome = deriveJavaHome(normalizedExecutable);
            return Optional.of(new RuntimeInfo(
                    normalizedExecutable,
                    javaHome,
                    version.major,
                    version.update,
                    firstLine(output),
                    source,
                    hasJavaFx8(javaHome)
            ));
        } catch (IOException e) {
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    static ParsedVersion parseVersion(String output) {
        if (output == null) {
            return null;
        }
        Matcher java8 = JAVA_8_VERSION.matcher(output);
        if (java8.find()) {
            int update = java8.group(1) == null ? 0 : Integer.parseInt(java8.group(1));
            return new ParsedVersion(8, update);
        }
        Matcher modern = MODERN_JAVA_VERSION.matcher(output);
        if (modern.find()) {
            return new ParsedVersion(Integer.parseInt(modern.group(1)), 0);
        }
        return null;
    }

    static boolean isUnqualifiedJavaCommand(String command) {
        if (command == null || command.contains("/") || command.contains("\\")) {
            return false;
        }
        String normalized = command.toLowerCase(Locale.ROOT);
        return normalized.equals("java")
                || normalized.equals("java.exe")
                || normalized.equals("javaw")
                || normalized.equals("javaw.exe");
    }

    private static boolean isJavawCommand(String command) {
        return command.toLowerCase(Locale.ROOT).startsWith("javaw");
    }

    private static List<Path> executableCandidates(String configuredPath) {
        return executableCandidates(expandHome(configuredPath));
    }

    private static List<Path> executableCandidates(Path location) {
        LinkedHashMap<Path, Boolean> candidates = new LinkedHashMap<>();
        if (location == null) {
            return new ArrayList<>();
        }
        if (Files.isRegularFile(location)) {
            candidates.put(location, Boolean.TRUE);
            return new ArrayList<>(candidates.keySet());
        }
        if (!Files.isDirectory(location)) {
            return new ArrayList<>();
        }

        addJavaExecutable(candidates, location.resolve(javaExecutableRelativePath()));
        addJavaExecutable(candidates, location.resolve(Paths.get("jre")).resolve(javaExecutableRelativePath()));
        addJavaExecutable(candidates, location.resolve(Paths.get("Contents", "Home"))
                .resolve(javaExecutableRelativePath()));

        try (java.util.stream.Stream<Path> children = Files.list(location)) {
            children.filter(Files::isDirectory).forEach(child -> {
                addJavaExecutable(candidates, child.resolve(javaExecutableRelativePath()));
                addJavaExecutable(candidates, child.resolve(Paths.get("jre")).resolve(javaExecutableRelativePath()));
                addJavaExecutable(candidates, child.resolve(Paths.get("Contents", "Home"))
                        .resolve(javaExecutableRelativePath()));
            });
        } catch (IOException ignored) {
            // An unreadable installation root is simply not a candidate.
        }

        // Vendor archives sometimes add an extra bundle or distribution level.
        try (java.util.stream.Stream<Path> descendants = Files.walk(location, 6)) {
            descendants.filter(Files::isRegularFile)
                    .filter(ExtensionJavaRuntime::isJavaExecutablePath)
                    .forEach(path -> candidates.put(path.toAbsolutePath().normalize(), Boolean.TRUE));
        } catch (IOException ignored) {
            // An unreadable installation root is simply not a candidate.
        }
        return new ArrayList<>(candidates.keySet());
    }

    private static boolean isJavaExecutablePath(Path path) {
        Path parent = path.getParent();
        if (parent == null || parent.getFileName() == null || !parent.getFileName().toString().equals("bin")) {
            return false;
        }
        String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return filename.equals("java") || filename.equals("java.exe");
    }

    private static void addJavaExecutable(Map<Path, Boolean> candidates, Path path) {
        if (Files.isRegularFile(path)) {
            candidates.put(path.toAbsolutePath().normalize(), Boolean.TRUE);
        }
    }

    private static Path javaExecutableRelativePath() {
        return Paths.get("bin", isWindows() ? "java.exe" : "java");
    }

    private static void addKnownInstallations(Map<Path, String> candidates) {
        String userHome = System.getProperty("user.home", "");
        if (isMac()) {
            addInstallationRoot(candidates, Paths.get(userHome, "Library", "Java", "JavaVirtualMachines"),
                    "user Java installation");
            addInstallationRoot(candidates, Paths.get("/Library/Java/JavaVirtualMachines"),
                    "system Java installation");
            addLocation(candidates,
                    "/Library/Internet Plug-Ins/JavaAppletPlugin.plugin/Contents/Home",
                    "Java plug-in installation");
        } else if (isWindows()) {
            addWindowsInstallations(candidates, System.getenv("ProgramFiles"));
            addWindowsInstallations(candidates, System.getenv("ProgramFiles(x86)"));
            addWindowsInstallations(candidates, System.getenv("LOCALAPPDATA"));
        } else {
            addInstallationRoot(candidates, Paths.get("/usr/lib/jvm"), "system Java installation");
            addInstallationRoot(candidates, Paths.get("/usr/java"), "system Java installation");
            addInstallationRoot(candidates, Paths.get(userHome, ".jdks"), "user Java installation");
        }
    }

    private static void addWindowsInstallations(Map<Path, String> candidates, String baseDirectory) {
        String base = trimToNull(baseDirectory);
        if (base == null) {
            return;
        }
        addInstallationRoot(candidates, Paths.get(base, "Java"), "Windows Java installation");
        addInstallationRoot(candidates, Paths.get(base, "Eclipse Adoptium"), "Adoptium Java installation");
        addInstallationRoot(candidates, Paths.get(base, "Zulu"), "Azul Java installation");
    }

    private static void addInstallationRoot(Map<Path, String> candidates, Path root, String source) {
        for (Path executable : executableCandidates(root)) {
            candidates.putIfAbsent(executable, source);
        }
    }

    private static void addLocation(Map<Path, String> candidates, String location, String source) {
        if (location == null) {
            return;
        }
        for (Path executable : executableCandidates(location)) {
            candidates.putIfAbsent(executable, source);
        }
    }

    private static Optional<Path> findJavaOnPath() {
        String pathValue = trimToNull(System.getenv("PATH"));
        if (pathValue == null) {
            return Optional.empty();
        }
        String executableName = isWindows() ? "java.exe" : "java";
        for (String pathEntry : pathValue.split(Pattern.quote(File.pathSeparator))) {
            if (pathEntry.isEmpty()) {
                continue;
            }
            Path candidate = Paths.get(pathEntry, executableName);
            if (Files.isRegularFile(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static Path deriveJavaHome(Path javaExecutable) {
        Path binDirectory = Objects.requireNonNull(javaExecutable.getParent());
        return Objects.requireNonNull(binDirectory.getParent()).toAbsolutePath().normalize();
    }

    private static boolean hasJavaFx8(Path javaHome) {
        return Files.isRegularFile(javaHome.resolve(Paths.get("jre", "lib", "ext", "jfxrt.jar")))
                || Files.isRegularFile(javaHome.resolve(Paths.get("lib", "ext", "jfxrt.jar")))
                || Files.isRegularFile(javaHome.resolve(Paths.get("lib", "jfxrt.jar")));
    }

    private static Path expandHome(String value) {
        if (value.startsWith("~/") || value.startsWith("~\\")) {
            return Paths.get(System.getProperty("user.home"), value.substring(2));
        }
        return Paths.get(value);
    }

    private static String readFully(InputStream inputStream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = inputStream.read(buffer)) >= 0) {
            output.write(buffer, 0, count);
        }
        return output.toString(StandardCharsets.UTF_8);
    }

    private static String firstLine(String value) {
        int lineEnd = value.indexOf('\n');
        return lineEnd < 0 ? value : value.substring(0, lineEnd).trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    static final class ParsedVersion {
        final int major;
        final int update;

        ParsedVersion(int major, int update) {
            this.major = major;
            this.update = update;
        }
    }

    public static final class RuntimeInfo {
        private final Path javaExecutable;
        private final Path javaHome;
        private final int majorVersion;
        private final int updateVersion;
        private final String versionLine;
        private final String source;
        private final boolean javaFx8Available;

        RuntimeInfo(Path javaExecutable, Path javaHome, int majorVersion, int updateVersion,
                    String versionLine, String source, boolean javaFx8Available) {
            this.javaExecutable = javaExecutable;
            this.javaHome = javaHome;
            this.majorVersion = majorVersion;
            this.updateVersion = updateVersion;
            this.versionLine = versionLine;
            this.source = source;
            this.javaFx8Available = javaFx8Available;
        }

        public Path getJavaExecutable() {
            return javaExecutable;
        }

        public Path getJavaHome() {
            return javaHome;
        }

        public int getMajorVersion() {
            return majorVersion;
        }

        public int getUpdateVersion() {
            return updateVersion;
        }

        public String getVersionLine() {
            return versionLine;
        }

        public String getSource() {
            return source;
        }

        public boolean isJavaFx8Available() {
            return javaFx8Available;
        }

        public String describe() {
            String update = updateVersion > 0 ? "u" + updateVersion : "";
            return "Java " + majorVersion + update
                    + " (JavaFX 8 " + (javaFx8Available ? "available" : "not detected") + ") at "
                    + javaExecutable + " [" + source + "]";
        }
    }
}
