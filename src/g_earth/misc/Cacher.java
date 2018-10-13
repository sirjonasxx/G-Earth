package g_earth.misc;

import g_earth.Main;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Created by Jonas on 28/09/18.
 */
public class Cacher {

    private static final String CACHEFILENAME = "cache.json";

    private static String getCacheDir() {
        return new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
    }

    private static boolean cacheFileExists() {
        File f = new File(getCacheDir(), CACHEFILENAME);
        return (f.exists() && !f.isDirectory());
    }

    private static JSONObject getCacheContents() {
        if (cacheFileExists()) {
            try {
                File f = new File(getCacheDir(), CACHEFILENAME);
                String contents = String.join("\n", Files.readAllLines(f.toPath()));

                JSONParser parser = new JSONParser();
                return (JSONObject) parser.parse(contents);
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        }
        return new JSONObject();
    }
    private static void updateCache(JSONObject contents) {
        try (FileWriter file = new FileWriter(new File(getCacheDir(), CACHEFILENAME))) {

            file.write(contents.toJSONString());
            file.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void put(String key, Object val) {
        JSONObject object = getCacheContents();
        if (object.containsKey(key)) object.remove(key);

        object.put(key, val);
        updateCache(object);
    }
    public static Object get(String key) {
        JSONObject object = getCacheContents();

        return object.get(key);
    }
    public static void clear() {
        updateCache(new JSONObject());
    }
}
