package gearth.misc;

public final class StringUtils {

    public static boolean isInteger(String string) {
        try {
            Integer.parseInt(string);
            return true;
        }
        catch (NumberFormatException e){
            return false;
        }
    }

    public static boolean isUShort(String string) {
        try {
            int res = Integer.parseInt(string);
            return res >= 0 && res < (256 * 256);
        } catch (NumberFormatException e) {
           return false;
        }
    }

    public static String cleanTextContent(String text)
    {
        text = text.replaceAll("\\p{Cntrl}&&[^\n\t]", "");
        return text.trim();
    }
}
