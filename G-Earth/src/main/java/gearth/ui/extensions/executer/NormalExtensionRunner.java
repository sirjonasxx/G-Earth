package gearth.ui.extensions.executer;

import gearth.Main;
import gearth.ui.extensions.authentication.Authenticator;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by Jonas on 22/09/18.
 */
public class NormalExtensionRunner implements ExtensionRunner {

    public static final String JARPATH;

    static {
        String value;
        try {
            value = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
        } catch (URISyntaxException e) {
            value = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
            e.printStackTrace();
        }
        JARPATH = value;
    }

    @Override
    public void runAllExtensions(int port) {
        if (dirExists(ExecutionInfo.EXTENSIONSDIRECTORY)){
            File folder =
                    new File(JARPATH +
                            FileSystems.getDefault().getSeparator()+
                            ExecutionInfo.EXTENSIONSDIRECTORY);

            File[] childs = folder.listFiles();
            for (File file : childs) {
                tryRunExtension(file.getPath(), port);
            }
        }
    }

    @Override
    public void installAndRunExtension(String path, int port) {
        if (!dirExists(ExecutionInfo.EXTENSIONSDIRECTORY)) {
            createDirectory(ExecutionInfo.EXTENSIONSDIRECTORY);
        }


        String name = Paths.get(path).getFileName().toString();
        String[] split = name.split("\\.");
        String ext = "*." + split[split.length - 1];

        String realname = String.join(".",Arrays.copyOf(split, split.length-1));
        String newname = realname + "-" + getRandomString() + ext.substring(1);

        Path originalPath = Paths.get(path);
        Path newPath = Paths.get(
                JARPATH,
                ExecutionInfo.EXTENSIONSDIRECTORY,
                newname
        );

        try {
            Files.copy(
                    originalPath,
                    newPath
            );

            addExecPermission(newPath.toString());
            tryRunExtension(newPath.toString(), port);

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void tryRunExtension(String path, int port) {
        try {
            String filename = Paths.get(path).getFileName().toString();
            String execCommand = ExecutionInfo.getExecutionCommand(getFileExtension(path))
                    .replace("{path}", path)
                    .replace("{port}", port+"")
                    .replace("{filename}", filename)
                    .replace("{cookie}", Authenticator.generateCookieForExtension(filename));
            Runtime.getRuntime().exec(execCommand);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void uninstallExtension(String filename) {
        try {
            Files.delete(Paths.get(
                    JARPATH,
                    ExecutionInfo.EXTENSIONSDIRECTORY,
                    filename
            ));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addExecPermission(String path) {
        //not needed at first sight
    }

    private String getFileExtension(String path) {
        String name = Paths.get(path).getFileName().toString();
        String[] split = name.split("\\.");
        return "*." + split[split.length - 1];
    }

    private boolean dirExists(String dir) {
        return Files.isDirectory(Paths.get(JARPATH, dir));
    }
    private void createDirectory(String dir) {
        if (!dirExists(dir)) {
            try {
                Files.createDirectories(Paths.get(JARPATH, dir));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private String getRandomString() {
        StringBuilder builder = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < 12; i++) {
            builder.append(r.nextInt(10));
        }

        return builder.toString();
    }
}
