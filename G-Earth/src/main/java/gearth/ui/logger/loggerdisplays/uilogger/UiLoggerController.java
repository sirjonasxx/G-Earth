package gearth.ui.logger.loggerdisplays.uilogger;

import gearth.misc.harble_api.HarbleAPI;
import gearth.misc.harble_api.HarbleAPIFetcher;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.ui.logger.loggerdisplays.PacketLogger;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.net.URL;
import java.util.*;

public class UiLoggerController implements Initializable {
    public FlowPane flowPane;
    public BorderPane borderPane;
    public Label lblViewIncoming;
    public Label lblViewOutgoing;
    public CheckMenuItem chkViewIncoming;
    public CheckMenuItem chkViewOutgoing;
    public CheckMenuItem chkDisplayStructure;
    public Label lblAutoScrolll;
    public CheckMenuItem chkAutoscroll;
    public CheckMenuItem chkSkipBigPackets;
    public CheckMenuItem chkMessageName;
    public CheckMenuItem chkMessageHash;
    public Label lblHarbleAPI;

    private WebView webView;

    private Stage stage;

    private boolean viewIncoming = true;
    private boolean viewOutgoing = true;
    private boolean displayStructure = true;
    private boolean autoScroll = true;
    private boolean skiphugepackets = true;
    private boolean viewMessageName = true;
    private boolean viewMessageHash = false;
    private boolean alwaysOnTop = false;

    private volatile boolean initialized = false;
    private final List<String> appendLater = new LinkedList<>();

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        webView = new WebView();

        borderPane.setCenter(webView);

        webView.getEngine().getLoadWorker().stateProperty().addListener((observableValue, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                initialized = true;
                webView.prefHeightProperty().bind(stage.heightProperty());
                webView.prefWidthProperty().bind(stage.widthProperty());
                appendLog(appendLater);
            }
        });

        webView.getEngine().load(getClass().getResource("/gearth/ui/logger/uilogger/logger.html").toString());
    }


    private static String cleanTextContent(String text) {
//        // strips off all non-ASCII characters
//        text = text.replaceAll("[^\\x00-\\x7F]", "");
//
//        // erases all the ASCII control characters
        text = text.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");

        // removes non-printable characters from Unicode
//        text = text.replaceAll("\\p{C}", "");

        return text.trim();
    }

    public synchronized void appendMessage(HPacket packet, int types) {
        boolean isBlocked = (types & PacketLogger.MESSAGE_TYPE.BLOCKED.getValue()) != 0;
        boolean isReplaced = (types & PacketLogger.MESSAGE_TYPE.REPLACED.getValue()) != 0;
        boolean isIncoming = (types & PacketLogger.MESSAGE_TYPE.INCOMING.getValue()) != 0;

        if (isIncoming && !viewIncoming) return;
        if (!isIncoming && !viewOutgoing) return;

        StringBuilder packetHtml = new StringBuilder("<div>");

        String expr = packet.toExpression(isIncoming ? HMessage.Direction.TOCLIENT : HMessage.Direction.TOSERVER);

        lblHarbleAPI.setText("Messages: " + (HarbleAPIFetcher.HARBLEAPI == null ? "False" : "True"));
        if ((viewMessageName || viewMessageHash) && HarbleAPIFetcher.HARBLEAPI != null) {
            HarbleAPI api = HarbleAPIFetcher.HARBLEAPI;
            HarbleAPI.HarbleMessage message = api.getHarbleMessageFromHeaderId(
                    (isIncoming ? HMessage.Direction.TOCLIENT : HMessage.Direction.TOSERVER),
                    packet.headerId()
            );

            if (message != null && !(viewMessageName && !viewMessageHash && message.getName() == null)) {
                if (viewMessageName && message.getName() != null) {
                    packetHtml.append(divWithClass("[" + message.getName() + "]", "messageinfo"));
                }
                if (viewMessageHash) {
                    packetHtml.append(divWithClass("[" + message.getHash() + "]", "messageinfo"));
                }
            }
        }

        if (isBlocked) packetHtml.append(divWithClass("[Blocked]", "blocked"));
        else if (isReplaced) packetHtml.append(divWithClass("[Replaced]", "replaced"));


        packetHtml
                .append("<div>")
                .append(isIncoming ? spanWithClass("Incoming[", "incoming") :
                        spanWithClass("Outgoing[", "outgoing"))
                .append(packet.headerId())
                .append(spanWithClass("]", isIncoming ? "incoming" : "outgoing"))
                .append(isIncoming ? " <- " : " -> ")
                .append(skiphugepackets && packet.length() > 4000 ?
                        divWithClass("<packet skipped>", "skipped") :
                        divWithClass(packet.toString(), isIncoming ? "incoming" : "outgoing"));


        String cleaned = cleanTextContent(expr);
        if (cleaned.equals(expr)) {
            if (!expr.equals("") && displayStructure && packet.length() <= 2000)
                packetHtml.append(divWithClass(cleanTextContent(expr), "structure"));
        }


        packetHtml.append(divWithClass("--------------------", ""));

        synchronized (appendLater) {
            if (initialized) {
                appendLog(Collections.singletonList(packetHtml.toString()));
            } else {
                appendLater.add(packetHtml.toString());
            }
        }
    }

    private synchronized void appendLog(List<String> html) {
        Platform.runLater(() -> {
            String script = "document.getElementById('output').innerHTML += '" + String.join("", html) + "';";
            webView.getEngine().executeScript(script);

            if (autoScroll) {
                executejQuery(webView.getEngine(), "$('html, body').animate({scrollTop:$(document).height()}, 'slow');");
            }
        });
    }

    // escapes logger text so that there are no javascript errors
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

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void toggleViewIncoming() {
        viewIncoming = !viewIncoming;
        lblViewIncoming.setText("View Incoming: " + (viewIncoming ? "True" : "False"));
//        chkViewIncoming.setSelected(viewIncoming);
    }

    public void toggleViewOutgoing() {
        viewOutgoing = !viewOutgoing;
        lblViewOutgoing.setText("View Outgoing: " + (viewOutgoing ? "True" : "False"));
//        chkViewOutgoing.setSelected(viewOutgoing);
    }

    public void toggleDisplayStructure() {
        displayStructure = !displayStructure;
//        chkDisplayStructure.setSelected(displayStructure);
    }

    public void toggleAutoscroll(ActionEvent actionEvent) {
        autoScroll = !autoScroll;
        lblAutoScrolll.setText("Autoscroll: " + (autoScroll ? "True" : "False"));
    }

    public void toggleSkipPackets(ActionEvent actionEvent) {
        skiphugepackets = !skiphugepackets;
    }

    public void toggleMessageName(ActionEvent actionEvent) {
        viewMessageName = !viewMessageName;
    }

    public void toggleMessageHash(ActionEvent actionEvent) {
        viewMessageHash = !viewMessageHash;
    }

    public void toggleAlwaysOnTop(ActionEvent actionEvent) {
        stage.setAlwaysOnTop(!alwaysOnTop);
        alwaysOnTop = !alwaysOnTop;
    }

    public void clearText(ActionEvent actionEvent) {
        executejQuery(webView.getEngine(), "$('#output').empty();");
    }

    private String divWithClass(String content, String klass) {
        return escapeMessage("<div class=\"" + klass + "\">" + content + "</div>");
    }

    private String spanWithClass(String content, String klass) {
        return escapeMessage("<span class=\"" + klass + "\">" + content + "</span>");
    }
}
