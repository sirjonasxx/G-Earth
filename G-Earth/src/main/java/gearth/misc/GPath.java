package gearth.misc;

import java.io.File;

public class GPath {

    /**
     * Get the path from a system property.
     * Expands $HOME to the user's home directory.
     */
    public static File getPathFromProperty(final String property) {
        String overridePath = System.getProperty(property);

        if (overridePath == null) {
            return null;
        }

        if (overridePath.startsWith("$HOME")) {
            overridePath = overridePath.replaceFirst("\\$HOME", System.getProperty("user.home"));
        }

        return new File(overridePath);
    }

}
