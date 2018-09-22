package main.ui.extensions.executer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Jonas on 22/09/18.
 */
public class ExecutionInfo {

    private static Map<String, String> extensionTypeToExecutionCommand;
    public final static List<String> ALLOWEDEXTENSIONTYPES;
    public final static String EXTENSIONSDIRECTORY = "Extensions";

    static {
        extensionTypeToExecutionCommand = new HashMap<>();
        extensionTypeToExecutionCommand.put("*.jar","java -jar {path} -p {port}");
        extensionTypeToExecutionCommand.put("*.py","python {path} -p {port}");
        extensionTypeToExecutionCommand.put("*.py3","python3 {path} -p {port}");
        extensionTypeToExecutionCommand.put("*.sh","{path} -p {port}");
        extensionTypeToExecutionCommand.put("*.exe","{path} -p {port}");

        ALLOWEDEXTENSIONTYPES = new ArrayList<>(extensionTypeToExecutionCommand.keySet());
    }

    public static String getExecutionCommand(String type) {
        return extensionTypeToExecutionCommand.get(type);
    }

}
