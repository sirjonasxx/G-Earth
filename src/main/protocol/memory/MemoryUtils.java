package main.protocol.memory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

class MemoryUtils {

    static boolean stringIsNumeric(String str) {
        for (char c : str.toCharArray()) {
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    static boolean fileContainsString(String path, String contains) {

        try {
            List<String> lines = Files.readAllLines(new File(path).toPath());
            for (String line : lines) {
                if (line.contains(contains)) return true;
            }
        } catch (Exception e) {
            // process of specified path not running anymore
        }
        return false;

    }

}
