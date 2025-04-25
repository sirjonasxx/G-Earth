package gearth.services.extension_handler.extensions.implementations.network.executer;

import gearth.GEarth;
import gearth.misc.Cacher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Jonas on 22/09/18.
 */
public final class ExecutionInfo {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionInfo.class);

    private static final Map<String, String[]> EXTENSION_TYPE_TO_EXECUTION_COMMAND;

    public final static List<String> ALLOWED_EXTENSION_TYPES;
    public final static File EXTENSIONS_DIRECTORY;

    static {
        try {
            final String overrideDataDir = System.getProperty("gearth.data.dir");
            final File dataDir = overrideDataDir != null
                    ? new File(overrideDataDir)
                    : new File(GEarth.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();

            EXTENSIONS_DIRECTORY = new File(dataDir, "Extensions");

            if (!EXTENSIONS_DIRECTORY.exists()) {
                if (!EXTENSIONS_DIRECTORY.mkdirs()) {
                    LOG.error("Could not create extensions directory");
                }
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        EXTENSION_TYPE_TO_EXECUTION_COMMAND = new HashMap<>();
        EXTENSION_TYPE_TO_EXECUTION_COMMAND.put("*.jar", new String[]{"java", "-jar", "{path}"});
        EXTENSION_TYPE_TO_EXECUTION_COMMAND.put("*.py", new String[]{"python", "{path}"});
        EXTENSION_TYPE_TO_EXECUTION_COMMAND.put("*.py3", new String[]{"python3", "{path}"});
        EXTENSION_TYPE_TO_EXECUTION_COMMAND.put("*.sh", new String[]{"{path}"});
        EXTENSION_TYPE_TO_EXECUTION_COMMAND.put("*.exe", new String[]{"{path}"});
        EXTENSION_TYPE_TO_EXECUTION_COMMAND.put("*.js", new String[]{"node", "{path}"});

        final String[] extraArgs = {"-p", "{port}", "-f", "{filename}", "-c", "{cookie}"};

        for(String type : EXTENSION_TYPE_TO_EXECUTION_COMMAND.keySet()) {

            final String[] commandShort = EXTENSION_TYPE_TO_EXECUTION_COMMAND.get(type);
            final String[] combined = new String[extraArgs.length + commandShort.length];
            System.arraycopy(commandShort, 0, combined, 0, commandShort.length);
            System.arraycopy(extraArgs, 0, combined, commandShort.length, extraArgs.length);

            EXTENSION_TYPE_TO_EXECUTION_COMMAND.put(type, combined);
        }

        ALLOWED_EXTENSION_TYPES = new ArrayList<>(EXTENSION_TYPE_TO_EXECUTION_COMMAND.keySet());
    }

    public static String[] getExecutionCommand(String type) {
        return EXTENSION_TYPE_TO_EXECUTION_COMMAND.get(type);
    }
}
