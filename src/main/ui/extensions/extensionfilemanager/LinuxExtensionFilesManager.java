package main.ui.extensions.extensionfilemanager;

import main.ui.extensions.extensionfilemanager.extensionfile.ExtensionFile;

import java.io.File;
import java.util.List;

/**
 * Created by Jonas on 21/06/18.
 */
public class LinuxExtensionFilesManager implements ExtensionFilesManager {

    @Override
    public List<ExtensionFile> getAllExtensions() {
        return null;
    }

    @Override
    public ExtensionFile addExtension(File file) {
        return null;
    }

    @Override
    public boolean removeExtension(ExtensionFile file) {
        return false;
    }
}
