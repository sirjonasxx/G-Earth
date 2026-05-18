package gearth.app.services.internal_extensions.extensionstore.application;

import gearth.app.ui.translations.LanguageBundle;
import org.w3c.dom.Element;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class WebUtils {


    private static HashMap<String, String> entityMap = new HashMap<>();
    static {
        entityMap.put("&", "&amp;");
        entityMap.put("<", "&lt;");
        entityMap.put(">", "&gt;");
//        entityMap.put("\"", "&quot;");
        entityMap.put("'", "&#39;");
//        entityMap.put("/", "&#x2F;");
        entityMap.put("`", "&#x60;");
        entityMap.put("=", "&#x3D;");
    }

    public static String escapeHtml(String s) {
        s = escapeHtmlNoBackslash(s);
        return s.replace("/", "&#x2F;");
    }

    public static String escapeHtmlNoBackslash(String s) {
        for(String key : entityMap.keySet()) {
            s = s.replace(key, entityMap.get(key));
        }
        return s;
    }

    public static String elapsedSince(LocalDateTime time) {
        return elapsedTime(System.currentTimeMillis()/1000 - time.atZone(ZoneId.systemDefault()).toEpochSecond());
    }

    public static String elapsedTime(long time) {
        if (time < 60) return time + " " + LanguageBundle.get("ext.store.elapsedtime.second." + (time == 1 ? "single": "multiple"));
        time = time/60;
        if (time < 60) return time + " " + LanguageBundle.get("ext.store.elapsedtime.minute." + (time == 1 ? "single": "multiple"));
        time = time/60;
        if (time < 24) return time + " " + LanguageBundle.get("ext.store.elapsedtime.hour." + (time == 1 ? "single": "multiple"));
        long days = time/24;
        if (days < 7) return days + " " + LanguageBundle.get("ext.store.elapsedtime.day." + (days == 1 ? "single": "multiple"));
        long weeks = days/7;
        if (weeks < 6) return weeks + " " + LanguageBundle.get("ext.store.elapsedtime.week." + (weeks == 1 ? "single": "multiple"));
        long months = days/31;
        if (months < 12) return months + " " + LanguageBundle.get("ext.store.elapsedtime.month." + (months == 1 ? "single": "multiple"));
        long years = days/365;
        return years + " " + LanguageBundle.get("ext.store.elapsedtime.year." + (years == 1 ? "single": "multiple"));
    }

    public static String escapeMessage(String text) {
        text = escapeHtml(text);
        return text
                .replace("\n\r", "<br>")
                .replace("\n", "<br>")
                .replace("\r", "<br>");
    }

    public static String escapeMessageAndQuotes(String text) {
        text = escapeMessage(text);
        return text.replace("\"", "&quot;");
    }

    public static void clearElement(Element node) {
        while (node.hasChildNodes())
            node.removeChild(node.getFirstChild());
    }

    public static void removeClass(Element node, String classname) {
        String[] classes = node.getAttribute("class").split(" ");
        List<String> classesFiltered = new ArrayList<>(Arrays.asList(classes));
        classesFiltered = classesFiltered.stream().filter(s -> !s.toLowerCase().equals(classname.toLowerCase())).collect(Collectors.toList());
        node.setAttribute("class", String.join(" ", classesFiltered));
    }

    public static void addClass(Element node, String classname) {
        removeClass(node, classname);
        node.setAttribute("class", node.getAttribute("class") + " " + classname);
    }

}
