package gearth.services.internal_extensions.extensionstore.tools;

import gearth.GEarth;
import gearth.misc.OSValidator;
import gearth.services.extension_handler.extensions.implementations.network.NetworkExtensionAuthenticator;
import gearth.services.extension_handler.extensions.implementations.network.executer.ExecutionInfo;
import gearth.services.extension_handler.extensions.implementations.network.executer.NormalExtensionRunner;
import gearth.services.internal_extensions.extensionstore.repository.StoreFetch;
import gearth.services.internal_extensions.extensionstore.repository.StoreRepository;
import gearth.services.internal_extensions.extensionstore.repository.models.StoreExtension;
import gearth.ui.translations.LanguageBundle;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class StoreExtensionTools {

    private final static Logger LOG = LoggerFactory.getLogger(StoreExtensionTools.class);

    public interface InstallExtListener {

        void success(File installationFolder);
        void fail(String reason);

    }

    public static void executeExtension(final File extensionPath, int port) {
        try {
            String installedExtensionId = extensionPath.getName();

            String commandPath = new File(extensionPath, "command.txt").getAbsolutePath();
            String cookie = NetworkExtensionAuthenticator.generateCookieForExtension(installedExtensionId);
            List<String> command = new JSONArray(FileUtils.readFileToString(new File(commandPath), "UTF-8"))
                    .toList().stream().map(o -> (String)o).map(s -> s
                            .replace("{port}", port+"")
                            .replace("{filename}", installedExtensionId)
                            .replace("{cookie}", cookie))
                    .collect(Collectors.toList());

            NormalExtensionRunner.locateJavaForJar(command, new File(extensionPath, "extension"), null);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(extensionPath, "extension"));
            Process p = NormalExtensionRunner.startProcess(pb);
            NormalExtensionRunner.logExtension(installedExtensionId, extensionPath, p);
        } catch (IOException e) {
            LOG.error("Failed to execute extension", e);
        }
    }

    private static void unzipInto(InputStream inputStream, File directory) throws IOException {
        byte[] buffer = new byte[1024];
        inputStream = new BufferedInputStream(inputStream);
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);


        ZipEntry entry = zipInputStream.getNextEntry();
        while (entry != null) {

            File file = new File(Paths.get(directory.getPath(), entry.getName()).toString());

            if (entry.isDirectory()) {
                if (!file.isDirectory() && !file.mkdirs()) {
                    throw new IOException("Failed to create directory " + file);
                }
            }
            else {
                File parent = file.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                // write file content
                FileOutputStream fos = new FileOutputStream(file);
                int len;
                while ((len = zipInputStream.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }

            entry = zipInputStream.getNextEntry();
        }
        zipInputStream.closeEntry();
        zipInputStream.close();
        inputStream.close();

    }

    public static void installExtension(String name, StoreRepository storeRepository, InstallExtListener listener) {
        new Thread(() -> {

            String downloadUrl = String.format("https://github.com/sirjonasxx/G-ExtensionStore/raw/repo/%s/store/extensions/%s/extension.zip", storeRepository.getRepoVersion(),
                    EncodingUtil.encodeURIComponent(name));
            Optional<StoreExtension> maybeExt = storeRepository.getExtensions().stream().filter(e -> e.getTitle().equals(name)).findFirst();
            if (maybeExt.isPresent()) {
                StoreExtension ext = maybeExt.get();
                String version = ext.getVersion();

                String folderName = name + "_" + version;
                File path = new File(ExecutionInfo.EXTENSIONS_DIRECTORY, folderName);

                File extensionPath = new File(path, "extension");

                if (extensionPath.mkdirs()) {
                    try {
                        URL url = new URL(downloadUrl);
                        InputStream inputStream = url.openStream();

                        try {
                            unzipInto(inputStream, extensionPath);

                            File commandFile = new File(path, "command.txt");
                            List<String> command = OSValidator.isMac() ? ext.getCommands().getMac() : (OSValidator.isUnix() ? ext.getCommands().getLinux() :
                                            (OSValidator.isWindows() ? ext.getCommands().getWindows() : ext.getCommands().getDefault()));
                            command = command == null ? ext.getCommands().getDefault() : command;

//                            String commandRaw = new JSONArray(command).toString();
//                            PrintWriter out = new PrintWriter(Paths.get(path, "command.txt").toString());
//                            out.println(commandRaw);
//                            out.close();

                            FileUtils.writeStringToFile(commandFile, new JSONArray(command).toString(), "UTF-8");
                            listener.success(path);

                        } catch (IOException e) {
                            LOG.error("Failed to unzip extension", e);
                            listener.fail(LanguageBundle.get("ext.store.fail.unzip"));
                            removeExtension(path);
                        }

                    } catch (MalformedURLException e) {
                        listener.fail(LanguageBundle.get("ext.store.fail.invalidurl"));
                        try {
                            removeExtension(path); // cleanup
                        } catch (IOException ignore) { }
                    } catch (IOException e) {
                        listener.fail(LanguageBundle.get("ext.store.fail.notavailable"));
                        try {
                            removeExtension(path); // cleanup
                        } catch (IOException ignore) { }
                    }
                }
                else {
                    listener.fail(LanguageBundle.get("ext.store.fail.alreadyexists"));
                    // don't do cleanup since you might not want removal of current extension files
                }
            }
            else {
                listener.fail(LanguageBundle.get("ext.store.fail.notfound"));
            }

        }).start();

    }

    public static void removeExtension(final File extensionPath) throws IOException {
        FileUtils.deleteDirectory(extensionPath);
    }

    public static List<InstalledExtension> getInstalledExtension() {
        List<InstalledExtension> installedExtensions = new ArrayList<>();

        File[] existingExtensions = ExecutionInfo.EXTENSIONS_DIRECTORY.listFiles();
        if (existingExtensions != null) {
            for (File extension : existingExtensions) {

                if (extension.isDirectory()) {
                    // installed through extensions store
                    if (extension.getName().contains("_")) {
                        List<String> parts = new ArrayList<>(Arrays.asList(extension.getName().split("_")));
                        String version = parts.remove(parts.size() - 1);
                        String extensionName = String.join("_", parts);
                        installedExtensions.add(new InstalledExtension(extensionName, version));
                    }

                }
            }
        }

        return installedExtensions;
    }

    // 0 = not installed
    // 1 = installed
    // 2 = version mismatch
    public static int isExtensionInstalled(String name, String version) {
        List<InstalledExtension> installedExtensions = getInstalledExtension();
        if (installedExtensions.stream().anyMatch(p -> p.getName().equals(name)
                && new ComparableVersion(p.getVersion()).compareTo(new ComparableVersion(version)) >= 0))
            return 1;
        if (installedExtensions.stream().anyMatch(p -> p.getName().equals(name))) return 2;
        return 0;
    }

    public static void updateExtension(String name, StoreRepository storeRepository, InstallExtListener listener) {
        // remove old occurences

        try {
            File[] existingExtensions = ExecutionInfo.EXTENSIONS_DIRECTORY.listFiles();
            if (existingExtensions != null) {
                for (File extension : existingExtensions) {

                    if (extension.isDirectory()) {
                        // installed through extensions store
                        if (extension.getName().contains("_")) {
                            List<String> parts = new ArrayList<>(Arrays.asList(extension.getName().split("_")));
                            parts.remove(parts.size() - 1);
                            String extensionName = String.join("_", parts);
                            if (name.equals(extensionName)) {
                                removeExtension(extension);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            listener.fail(LanguageBundle.get("ext.store.fail.uninstall"));
            return;
        }

        installExtension(name, storeRepository, listener);
    }

    public static void main(String[] args) {
        StoreFetch.fetch(GEarth.version, new StoreFetch.StoreFetchListener() {
            @Override
            public void success(StoreRepository storeRepository) {
                installExtension("G-BuildTools", storeRepository, new InstallExtListener() {
                    @Override
                    public void success(File installationFolder) {
                        System.out.println(String.format("Installed in: %s", installationFolder));
                    }

                    @Override
                    public void fail(String reason) {
                        System.out.println(reason);
                    }
                });
            }

            @Override
            public void fail(String reason) {
                System.out.println("failed fetching repository");
            }
        });
    }


}
