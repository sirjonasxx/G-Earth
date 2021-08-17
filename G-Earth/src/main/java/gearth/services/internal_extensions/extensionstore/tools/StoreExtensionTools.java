package gearth.services.internal_extensions.extensionstore.tools;

import gearth.services.extension_handler.extensions.implementations.network.executer.ExecutionInfo;
import gearth.services.extension_handler.extensions.implementations.network.executer.NormalExtensionRunner;
import gearth.services.internal_extensions.extensionstore.repository.StoreRepository;
import gearth.services.internal_extensions.extensionstore.repository.models.StoreExtension;
import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class StoreExtensionTools {

    public interface InstallExtListener {

        void success(String installationFolder);
        void fail(String reason);

    }


    public static void executeExtension(String folderName) {

    }

    private static void unzipInto(InputStream inputStream, File directory) throws IOException {
        inputStream = new BufferedInputStream(inputStream);
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);

        for (ZipEntry entry = null; (entry = zipInputStream.getNextEntry()) != null;) {

            File file = new File(Paths.get(directory.getPath(), entry.getName()).toString());

            if (entry.isDirectory()) {
                file.mkdirs();
            }
            else {
                FileUtils.copyInputStreamToFile(inputStream, file);
//                StreamUtil.write(pathBuilder, inputStream, false);
            }

        }
    }

    public static void installExtension(String name, StoreRepository storeRepository, InstallExtListener listener) {


        new Thread(() -> {

            String downloadUrl = String.format("https://github.com/sirjonasxx/G-ExtensionStore/raw/repo/%s/store/extensions/%s/extension.zip", storeRepository.getRepoVersion(), name);
            Optional<StoreExtension> maybeExt = storeRepository.getExtensions().stream().filter(e -> e.getTitle().equals(name)).findFirst();
            if (maybeExt.isPresent()) {
                StoreExtension ext = maybeExt.get();
                String version = ext.getVersion();

                String folderName = name + "_" + version;
                String path = Paths.get(NormalExtensionRunner.JARPATH, ExecutionInfo.EXTENSIONSDIRECTORY, folderName).toString();

                File dir = new File(path);
                File extensionPath = new File(Paths.get(path, "extension").toString());

                if (extensionPath.mkdirs()) {
                    try {
                        URL url = new URL(downloadUrl);
                        InputStream inputStream = url.openStream();
                        unzipInto(inputStream, extensionPath);
                        // todo command file


                    } catch (MalformedURLException e) {
                        listener.fail("Invalid extension URL");
                    } catch (IOException e) {
                        listener.fail("Extension not available in repository");
                    }
                }
                else {
                    listener.fail("Something went wrong creating the extension directory");
                }
            }
            else {
                listener.fail("Extension wasn't found");
            }

        }).start();

    }

    public static void removeExtension(String folderName) {

    }


    public static void updateExtension() {

    }


}
