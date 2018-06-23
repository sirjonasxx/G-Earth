package main.ui.extensions.extensionfilemanager;

import main.ui.extensions.extensionfilemanager.extensionfile.ExtensionFile;

import java.io.File;
import java.util.List;

/**
 * Created by Jonas on 21/06/18.
 */
public interface ExtensionFilesManager {

    List<ExtensionFile> getAllExtensions();

    ExtensionFile addExtension(File file); //returns g-earth extension file, returns null if failure

    boolean removeExtension(ExtensionFile file); //returns false if not done

}
