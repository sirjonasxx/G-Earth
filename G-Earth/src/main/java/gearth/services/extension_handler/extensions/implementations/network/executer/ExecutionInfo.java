package gearth.services.extension_handler.extensions.implementations.network.executer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Jonas on 22/09/18.
 */
public final class ExecutionInfo {

    private static final Map<String, String[]> EXTENSION_TYPE_TO_EXECUTION_COMMAND;

    public final static List<String> ALLOWED_EXTENSION_TYPES;
    public final static String EXTENSIONS_DIRECTORY = "Extensions";

    static {

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
