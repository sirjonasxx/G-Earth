package gearth.ui.translations;

import gearth.misc.StringifyAble;
import javafx.beans.value.ObservableValueBase;
import org.json.JSONObject;

import java.util.Arrays;

public class TranslatableString extends ObservableValueBase<String> implements StringifyAble {
    private String format;
    private String[] keys;

    public TranslatableString(String format, String... keys) {
        this.format = format;
        this.keys = keys;
        this.fireValueChangedEvent();
        LanguageBundle.addTranslatableString(this);
    }

    private TranslatableString(String fromString) {
        constructFromString(fromString);
    }

    public String getFormat() {
        return format;
    }

    @Override
    public String getValue() {
        return String.format(format, Arrays.stream(keys).map(LanguageBundle::get).toArray());
    }

    public void setKey(int index, String key) {
        keys[index] = key;
        fireValueChangedEvent();
    }

    public void setKeys(String... keys) {
        this.keys = keys;
        fireValueChangedEvent();
    }

    public void setFormat(String format) {
        this.format = format;
        fireValueChangedEvent();
    }

    public void setFormatAndKeys(String format, String... keys) {
        this.format = format;
        this.keys = keys;
        fireValueChangedEvent();
    }

    protected void trigger() {
        fireValueChangedEvent();
    }

    @Override
    public String stringify() {
        return new JSONObject()
                .put("format", format)
                .put("keys", keys)
                .toString();
    }

    @Override
    public void constructFromString(String str) {
        JSONObject jsonObject = new JSONObject(str);
        this.format = jsonObject.getString("format");
        this.keys = jsonObject.getJSONArray("keys")
                .toList().stream()
                .map(k -> (String) k)
                .toArray(String[]::new);
    }

    public static TranslatableString fromString(String str) {
        return new TranslatableString(str);
    }
}
