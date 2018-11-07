package gearth.misc;

import java.io.PrintStream;
import java.util.prefs.Preferences;

/**
 * Created by Jonas on 5/11/2018.
 */
public class AdminValidator {

    //https://stackoverflow.com/questions/4350356/detect-if-java-application-was-run-as-a-windows-admin

    private static Boolean isAdmin = null;

    public static boolean isAdmin() {
        if (isAdmin == null) {
            Preferences prefs = Preferences.systemRoot();
            PrintStream systemErr = System.err;
            synchronized(systemErr){    // better synchroize to avoid problems with other threads that access System.err
                System.setErr(null);
                try{
                    prefs.put("foo", "bar"); // SecurityException on Windows
                    prefs.remove("foo");
                    prefs.flush(); // BackingStoreException on Linux
                    isAdmin = true;
                }catch(Exception e){
                    isAdmin = false;
                }finally{
                    System.setErr(systemErr);
                }
            }
        }

        return isAdmin;
    }

}
