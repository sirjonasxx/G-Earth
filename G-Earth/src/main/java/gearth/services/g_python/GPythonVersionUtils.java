package gearth.services.g_python;

import org.apache.maven.artifact.versioning.ComparableVersion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class GPythonVersionUtils {

    private static final String MIN_GPYTHON_VERSION = "0.1.6";
    private static final String MIN_PYTHON_VERSION = "3.2";

    // returns null if python not installed
    public static String pythonVersion() {
        List<String> commandOutput = executeCommand("python", "--version");
        if (commandOutput.size() == 0) {
            return null;
        }
        String maybeVersion = commandOutput.get(0);
        if (!maybeVersion.contains("Python")) {
            return null;
        }

        return maybeVersion.split(" ")[1];
    }

    public static boolean validInstallation() throws Exception {
        // validates if user has all dependencies installed
        String pythonVersion = pythonVersion();
        if (pythonVersion == null) {
            throw new Exception("Python is not installed.");
        }

        ComparableVersion version = new ComparableVersion(pythonVersion);
        if (version.compareTo(new ComparableVersion(MIN_PYTHON_VERSION)) < 0) {
            throw new Exception("Python version must be at least "+MIN_PYTHON_VERSION+", but only "+version+" was found.");
        }

        List<String> allPackages = executeCommand("python", "-m", "pip", "list");
        allPackages = allPackages.subList(2, allPackages.size());

        String qtconsole = getPackageVersion(allPackages, "qtconsole");
        String pyqt5 = getPackageVersion(allPackages, "pyqt5");
        String jupyterConsole = getPackageVersion(allPackages, "jupyter-console");
        String gPython = getPackageVersion(allPackages, "g-python");

        if (qtconsole == null || pyqt5 == null || jupyterConsole == null || gPython == null) {
            throw new Exception("One or more of these dependencies was not found: 'qtconsole', 'pyqt5', 'jupyter-console', 'g-python'.");
        }

        ComparableVersion gVersion = new ComparableVersion(gPython);
        if (gVersion.compareTo(new ComparableVersion(MIN_GPYTHON_VERSION)) < 0) {
            throw new Exception("G-Python version must be at least "+MIN_GPYTHON_VERSION+", but only "+gVersion+" was found.");
        }

        return true;
    }

    // null if not found
    private static String getPackageVersion(List<String> allPackages, String pkg) {
        pkg = pkg.toLowerCase();

        for (String maybePkg : allPackages) {
            String[] split = maybePkg.split(" +");
            if (split[0].toLowerCase().equals(pkg)) {
                return split[1];
            }
        }
        return null;
    }

    private static List<String> executeCommand(String... command) {
        List<String> output = new ArrayList<>();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                output.add(line);
            }

            process.waitFor();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return output;
    }

//    public static void main(String[] args) {
//        System.out.println(validInstallation());
//    }

}
