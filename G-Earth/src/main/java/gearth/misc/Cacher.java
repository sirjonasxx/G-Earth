package gearth.misc;

import gearth.GEarth;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by Jonas on 28/09/18.
 */
public class Cacher {

    private static final String DEFAULT_CACHE_FILENAME = "cache.json";
    private static String cacheDir;

    static {
        File GEarthDir = null;
        try {
            GEarthDir = new File(GEarth.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
            if (GEarthDir.getName().equals("Extensions")) {
                GEarthDir = GEarthDir.getParentFile();
            }

        } catch (URISyntaxException e) { }

        cacheDir = GEarthDir
                + File.separator
                + "Cache";
    }

    public static void setCacheDir(String s) {
        cacheDir = s;
    }

    public static String getCacheDir() {
        return cacheDir;
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
                e.printStackTrace();
            }
        }
        return new JSONObject();
    }
    private static void updateCache(JSONObject contents, String cache_filename) {
        updateCache(contents.toString(), cache_filename);
    }
    public static void updateCache(String content, String cache_filename){
        File parent_dir = new File(getCacheDir());
        parent_dir.mkdirs();

        try (FileWriter file = new FileWriter(new File(getCacheDir(), cache_filename))) {

            file.write(content);
            file.flush();

        } catch (IOException e) {
            e.printStackTrace();
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

    public static <E extends Enum<E>> void ifEnumPresent(String key, Class<E> enumClass, Consumer<E> consumer) {
        if (getCacheContents().has(key)) {
            final E value = getCacheContents().getEnum(enumClass, key);
            consumer.accept(value);
        }
    }
    public static <E extends Enum<E>> void bindEnum(String key, Class<E> enumClass, ObjectProperty<E> valueProperty) {
        if (getCacheContents().has(key)) {
            final E value = getCacheContents().getEnum(enumClass, key);
            valueProperty.set(value);
        }
        valueProperty.addListener((observable, oldValue, newValue) -> put(key, newValue));
    }

    public static void bindString(String key, StringProperty valueProperty) {
        bind(key, valueProperty, getCacheContents()::getString);
    }
    public static void bindNumber(String key, Property<Number> valueProperty) {
        bind(key, valueProperty, getCacheContents()::getNumber);
    }
    public static void bindBoolean(String key, BooleanProperty valueProperty) {
        bind(key, valueProperty, getCacheContents()::getBoolean);
    }
    public static void bindJSONObject(String key, ObjectProperty<JSONObject> valueProperty) {
        bind(key, valueProperty, getCacheContents()::getJSONObject);
    }
    private static <T> void bind(String key, Property<T> valueProperty, Function<String, T> reader) {
        if (getCacheContents().has(key))
            valueProperty.setValue(reader.apply(key));
        valueProperty.addListener((observable, oldValue, newValue) -> put(key, newValue));
    }
}
