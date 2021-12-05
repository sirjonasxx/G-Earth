package gearth.misc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class RuntimeUtil {

    public static String getCommandOutput(String[] command) throws IOException {
        try {
            final Runtime rt = Runtime.getRuntime();
            final Process proc = rt.exec(command);

            final BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            final StringBuilder result = new StringBuilder();

            String line;

            while ((line = stdInput.readLine()) != null) {
                result.append(line);
                result.append("\n");
            }

            return result.toString();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

}
