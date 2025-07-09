package gearth.app.ui.translations;

import gearth.app.misc.Cacher;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

public class LanguageBundle extends ResourceBundle {

    private static final String LANGUAGE_CACHE_KEY = "language";
    private static Language current;
    private static final Set<TranslatableString> requireUpdate = new HashSet<>();

    static {
        try {
            current = Language.valueOf((String) Cacher.get(LANGUAGE_CACHE_KEY));
        } catch (Exception e) {
            current = Language.getSystemLanguage();
            Cacher.put(LANGUAGE_CACHE_KEY, current.toString());
        }
    }

    public static void addTranslatableString(TranslatableString translatableString) {
        requireUpdate.add(translatableString);
    }

    @Override
    protected Object handleGetObject(String key) {
        return current.messages.getObject(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        return current.messages.getKeys();
    }

    public static void setLanguage(Language lang) {
        current = lang;
        requireUpdate.forEach(TranslatableString::trigger);
        Cacher.put(LANGUAGE_CACHE_KEY, current.toString());
    }

    public static Language getLanguage() {
        return current;
    }

    public static String get(String key) {
        return current.messages.getString(key);
    }
}
