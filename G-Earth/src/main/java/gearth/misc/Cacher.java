package gearth.misc;

import gearth.GEarth;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;

/**
 * Created by Jonas on 28/09/18.
 */
public class Cacher {

    private static final Logger LOG = LoggerFactory.getLogger(Cacher.class);

    private static final String DEFAULT_CACHE_FILENAME = "cache.json";
    private static final File CACHE_DIR;

    static {
        final String overrideCacheDir = System.getProperty("gearth.cache.dir");

        if (overrideCacheDir != null) {
            CACHE_DIR = new File(overrideCacheDir);
        } else {
            try {
                File appDir = new File(GEarth.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();

                if (appDir.getName().equals("Extensions")) {
                    appDir = appDir.getParentFile();
                }

                CACHE_DIR = new File(appDir, "Cache");
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        if (!CACHE_DIR.exists()) {
            CACHE_DIR.mkdirs();
        }
    }

    public static File getCacheDir() {
        return CACHE_DIR;
    }

    public static boolean cacheFileExists(String cache_filename) {
        File f = new File(getCacheDir(), cache_filename);
        return (f.exists() && !f.isDirectory());
    }

    public static JSONObject getCacheContents(String cache_filename) {
        if (cacheFileExists(cache_filename)) {
            try {
                File f = new File(getCacheDir(), cache_filename);
                String contents = String.join("\n", Files.readAllLines(f.toPath()));

                return new JSONObject(contents);
            } catch (IOException e) {
                LOG.error("Error reading cache file", e);
            }
        }
        return new JSONObject();
    }

    private static void updateCache(JSONObject contents, String cache_filename) {
        updateCache(contents.toString(), cache_filename);
    }

    public static void updateCache(String content, String cache_filename){
        getCacheDir().mkdirs();

        try (FileWriter file = new FileWriter(new File(getCacheDir(), cache_filename))) {
            file.write(content);
            file.flush();
        } catch (IOException e) {
            LOG.error("Error writing cache file", e);
        }
    }

    public static void put(String key, Object val, String cache_filename) {
        JSONObject object = getCacheContents(cache_filename);
        if (object.has(key)) object.remove(key);

        object.put(key, val);
        updateCache(object, cache_filename);
    }

    public static void remove(String key, String cache_filename) {
        JSONObject object = getCacheContents(cache_filename);
        if (object.has(key)) object.remove(key);
        updateCache(object, cache_filename);
    }

    public static Object get(String key, String cache_filename) {
        JSONObject object = getCacheContents(cache_filename);
        if (object.has(key)) return object.get(key);
        else return null;
    }

    public static List<Object> getList(String key, String cache_filename) {
        JSONObject object = getCacheContents(cache_filename);
        if (object.has(key)) return ((JSONArray)object.get(key)).toList();
        else return null;
    }

    public static void clear(String cache_filename) {
        updateCache(new JSONObject(), cache_filename);
    }

    public static boolean cacheFileExists() {
        return cacheFileExists(DEFAULT_CACHE_FILENAME);
    }

    public static JSONObject getCacheContents() {
        return getCacheContents(DEFAULT_CACHE_FILENAME);
    }

    private static void updateCache(JSONObject contents) {
        updateCache(contents, DEFAULT_CACHE_FILENAME);
    }

    public static void put(String key, Object val) {
        put(key, val, DEFAULT_CACHE_FILENAME);
    }

    public static void remove(String key) {
        remove(key, DEFAULT_CACHE_FILENAME);
    }

    public static Object get(String key) {
        return get(key, DEFAULT_CACHE_FILENAME);
    }

    public static List<Object> getList(String key) {
        return getList(key, DEFAULT_CACHE_FILENAME);
    }

    public static void clear() {
         clear(DEFAULT_CACHE_FILENAME);
    }
}
