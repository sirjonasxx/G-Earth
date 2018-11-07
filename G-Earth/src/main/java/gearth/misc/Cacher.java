package gearth.misc;

import gearth.Main;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Created by Jonas on 28/09/18.
 */
public class Cacher {

    private static final String CACHE_FILENAME = "cache.json";

    private static String getCacheDir() {
        return new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
    }

    private static boolean cacheFileExists() {
        File f = new File(getCacheDir(), CACHE_FILENAME);
        return (f.exists() && !f.isDirectory());
    }

    private static JSONObject getCacheContents() {
        if (cacheFileExists()) {
            try {
                File f = new File(getCacheDir(), CACHE_FILENAME);
                String contents = String.join("\n", Files.readAllLines(f.toPath()));

                return new JSONObject(contents);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new JSONObject();
    }
    private static void updateCache(JSONObject contents) {
        try (FileWriter file = new FileWriter(new File(getCacheDir(), CACHE_FILENAME))) {

            file.write(contents.toString());
            file.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void put(String key, Object val) {
        JSONObject object = getCacheContents();
        if (object.has(key)) object.remove(key);

        object.put(key, val);
        updateCache(object);
    }

    public static Object get(String key) {
        JSONObject object = getCacheContents();
        if (object.has(key)) return object.get(key);
        else return null;
    }

    public static List<Object> getList(String key) {
        JSONObject object = getCacheContents();
        if (object.has(key)) return ((JSONArray)object.get(key)).toList();
        else return null;
    }

    public static void clear() {
        updateCache(new JSONObject());
    }
}
