package gearth.misc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public final class RuntimeUtil {

    public static String getCommandOutput(String[] command) throws IOException {
        final Runtime rt = Runtime.getRuntime();
        final Process proc = rt.exec(command);

        final StringBuilder result = new StringBuilder();

        readStream(result, proc.getInputStream());
        readStream(result, proc.getErrorStream());

        return result.toString();
    }

    public static void readStream(final StringBuilder builder, final InputStream stream) throws IOException {
        final BufferedReader stdInput = new BufferedReader(new InputStreamReader(stream));

        String line;

        while ((line = stdInput.readLine()) != null) {
            builder.append(line);
            builder.append("\n");
        }
    }

}