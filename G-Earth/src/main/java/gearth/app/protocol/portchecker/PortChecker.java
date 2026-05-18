package gearth.app.protocol.portchecker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * This interface wraps the OS-dependant operation of checking if a specific port is used by any program.
 * Some programs like McAfee TrueKey run on port 30000. This will hopefully save the user some time troubleshooting.
 */
public interface PortChecker {
    /**
     * @param port port to check
     * @return process name or null if none
     */
    String getProcessUsingPort(int port) throws IOException;

    /** Runs a command and reads the first line of output
     * @param command Command to run
     * @return {@link String} containing the output
     * @throws IOException If an I/O error occurs
     */
    default String getCommandOutput(String[] command) throws IOException {
        try {
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(command);

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));

            return stdInput.readLine();

        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }
}
