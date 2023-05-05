package gearth.misc;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.util.StringConverter;

import java.util.function.Function;

/**
 * Provides utility methods for bindings.
 *
 * @author Dorving
 */
public final class BindingsUtil {

    /**
     * Creates a binding that is true if the function returns true for the applied string.
     */
    public static BooleanBinding stringMatches(ObservableValue<String> stringValue, Function<String, Boolean> function) {
        return Bindings.createBooleanBinding(() -> function.apply(stringValue.getValue()), stringValue);
    }

    /**
     * Creates a binding that is true if the string is an integer.
     */
    public static BooleanBinding isInteger(ObservableValue<String> stringValue) {
        return stringMatches(stringValue, StringUtils::isInteger);
    }

    /**
     * Creates a binding that is true if the string is an UShort.
     */
    public static BooleanBinding isUShort(ObservableValue<String> stringValue) {
        return stringMatches(stringValue, StringUtils::isUShort);
    }

    /**
     * Ensures the list always contains the value of the binding.
     */
    public static <T> void addAndBindContent(ObservableList<T> list, ObjectBinding<T> binding) {
        binding.addListener((observable, oldValue, newValue) -> {
            list.remove(oldValue);
            list.add(newValue);
        });
        list.add(binding.get());
    }

    /**
     * Sets the value of a property and binds it to another observable value.
     */
    public static<T> void setAndBind(Property<T> a, ObservableValue<T> b, boolean unbindPrevious) {
        if (unbindPrevious)
            a.unbind();
        a.setValue(b.getValue());
        a.bind(b);
    }

    public static<T> void setAndBind(Property<T> a, ObservableValue<T> b) {
        setAndBind(a, b, false);
    }

    /**
     * Sets the value of a property and binds it bidirectionally to another property.
     */
    public static<T> void setAndBindBiDirectional(Property<T> a, Property<T> b) {
        a.setValue(b.getValue());
        a.bindBidirectional(b);
    }

    /**
     * Sets the value of a string property and binds it bidirectionally to another property using a converter.
     */
    public static<T> void setAndBindBiDirectional(StringProperty a, Property<T> b, StringConverter<T> converter) {
        a.setValue(converter.toString(b.getValue()));
        a.bindBidirectional(b, converter);
    }

    /**
     * Sets the value of a string property and binds it bidirectionally to an integer property.
     */
    public static<T> void setAndBindBiDirectional(StringProperty a, IntegerProperty b) {
        setAndBindBiDirectional(a, b, STRING_INT_CONVERTER);
    }

    private static final StringConverter<Number> STRING_INT_CONVERTER = new StringConverter<Number>() {
        @Override
        public String toString(Number object) {
            return Integer.toString(object.intValue());
        }

        @Override
        public Number fromString(String string) {
            return Integer.parseInt(string);
        }
    };
}
