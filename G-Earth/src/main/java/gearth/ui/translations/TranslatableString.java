package gearth.ui.translations;

import javafx.beans.value.ObservableValueBase;

public class TranslatableString extends ObservableValueBase<String> {
    private final String key;

    public TranslatableString(String key) {
        this.key = key;
        this.fireValueChangedEvent();
        LanguageBundle.addTranslatableString(this);
    }

    @Override
    public String getValue() {
        return LanguageBundle.get(key);
    }

    protected void trigger() {
        fireValueChangedEvent();
    }
}
