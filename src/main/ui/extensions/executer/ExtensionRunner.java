package main.ui.extensions.executer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Jonas on 21/09/18.
 */
public interface ExtensionRunner {

    void runAllExtensions(int port);

    void installAndRunExtension(String path, int port);

}
