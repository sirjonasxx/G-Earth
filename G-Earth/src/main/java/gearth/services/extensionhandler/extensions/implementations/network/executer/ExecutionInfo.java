package gearth.services.extensionhandler.extensions.implementations.network.executer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Jonas on 22/09/18.
 */
public class ExecutionInfo {

    private static Map<String, String[]> extensionTypeToExecutionCommand;
    public final static List<String> ALLOWEDEXTENSIONTYPES;
    public final static String EXTENSIONSDIRECTORY = "Extensions";

    static {
        extensionTypeToExecutionCommand = new HashMap<>();
        extensionTypeToExecutionCommand.put("*.jar", new String[]{"java", "-jar", "{path}"});
        extensionTypeToExecutionCommand.put("*.py", new String[]{"python", "{path}"});
        extensionTypeToExecutionCommand.put("*.py3", new String[]{"python3", "{path}"});
        extensionTypeToExecutionCommand.put("*.sh", new String[]{"{path}"});
        extensionTypeToExecutionCommand.put("*.exe", new String[]{"{path}"});

        String[] extraArgs = {"-p", "{port}", "-f", "{filename}", "-c", "{cookie}"};
        for(String type : extensionTypeToExecutionCommand.keySet()) {
            String[] commandShort = extensionTypeToExecutionCommand.get(type);
            String[] combined = new String[extraArgs.length + commandShort.length];
            System.arraycopy(commandShort, 0, combined, 0, commandShort.length);
            System.arraycopy(extraArgs, 0, combined, commandShort.length, extraArgs.length);

            extensionTypeToExecutionCommand.put(
                    type,
                    combined
                    );
        }

        ALLOWEDEXTENSIONTYPES = new ArrayList<>(extensionTypeToExecutionCommand.keySet());
    }

    public static String[] getExecutionCommand(String type) {
        return extensionTypeToExecutionCommand.get(type);
    }

}
