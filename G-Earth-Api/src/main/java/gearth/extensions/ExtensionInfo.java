package gearth.extensions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Jonas on 25/09/18.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ExtensionInfo {
    String Title();
    String Description();
    String Version();
    String Author();
}