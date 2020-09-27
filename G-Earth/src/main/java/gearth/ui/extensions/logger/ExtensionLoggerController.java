package gearth.ui.extensions.logger;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.net.URL;
import java.util.*;

public class ExtensionLoggerController implements Initializable {
    public BorderPane borderPane;

    private Stage stage = null;
    private WebView webView;

    private volatile boolean initialized = false;
    private final List<String> appendOnLoad = new LinkedList<>();


    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        webView = new WebView();

        borderPane.setCenter(webView);

        webView.getEngine().getLoadWorker().stateProperty().addListener((observableValue, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                initialized = true;
                webView.prefHeightProperty().bind(stage.heightProperty());
                webView.prefWidthProperty().bind(stage.widthProperty());
                appendLog(appendOnLoad);
            }
        });

        webView.getEngine().load(getClass().getResource("/gearth/ui/logger/uilogger/logger.html").toString());
    }

    private synchronized void appendLog(List<String> html) {
        Platform.runLater(() -> {
            String script = "document.getElementById('output').innerHTML += '" + String.join("", html) + "';";
            webView.getEngine().executeScript(script);

            executejQuery(webView.getEngine(), "$('html, body').animate({scrollTop:$(document).height()}, 'slow');");
        });
    }

    void log(String s) {
        s = cleanTextContent(s);
        List<String> elements = new LinkedList<>();

        String classname, text;
        if (s.startsWith("[") && s.contains("]")) {
            classname = s.substring(1, s.indexOf("]"));
            text = s.substring(s.indexOf("]") + 1);
        }
        else {
            classname = "black";
            text = s;
        }

        if (text.contains(" --> ")) {
            int index = text.indexOf(" --> ") + 5;
            String extensionAnnouncement = text.substring(0, index);
            text = text.substring(index);
            elements.add(divWithClass(extensionAnnouncement, "black"));
        }
        elements.add(divWithClass(text, classname.toLowerCase()));

        synchronized (appendOnLoad) {
            if (initialized) {
                appendLog(elements);
            }
            else {
                appendOnLoad.addAll(elements);
            }
        }
    }

    void setStage(Stage stage) {
        this.stage = stage;
    }

    private static String cleanTextContent(String text)
    {
//        // strips off all non-ASCII characters
//        text = text.replaceAll("[^\\x00-\\x7F]", "");
//
//        // erases all the ASCII control characters
        text = text.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");

        // removes non-printable characters from Unicode
//        text = text.replaceAll("\\p{C}", "");

        return text.trim();
    }

    private String divWithClass(String content, String klass) {
        return escapeMessage("<div class=\"" + klass + "\">" + content + "</div>");
    }

    private String spanWithClass(String content, String klass) {
        return escapeMessage("<span class=\"" + klass + "\">" + content + "</span>");
    }

    private String escapeMessage(String text) {
        return text
                .replace("\n\r", "<br />")
                .replace("\n", "<br />")
                .replace("\r", "<br />")
                .replace("'", "\\'");
    }

    private static Object executejQuery(final WebEngine engine, String script) {
        return engine.executeScript(
                "(function(window, document, version, callback) { "
                        + "var j, d;"
                        + "var loaded = false;"
                        + "if (!(j = window.jQuery) || version > j.fn.jquery || callback(j, loaded)) {"
                        + " var script = document.createElement(\"script\");"
                        + " script.type = \"text/javascript\";"
                        + " script.src = \"http://code.jquery.com/jquery-1.7.2.min.js\";"
                        + " script.onload = script.onreadystatechange = function() {"
                        + " if (!loaded && (!(d = this.readyState) || d == \"loaded\" || d == \"complete\")) {"
                        + " callback((j = window.jQuery).noConflict(1), loaded = true);"
                        + " j(script).remove();"
                        + " }"
                        + " };"
                        + " document.documentElement.childNodes[0].appendChild(script) "
                        + "} "
                        + "})(window, document, \"1.7.2\", function($, jquery_loaded) {" + script + "});"
        );
    }


}
