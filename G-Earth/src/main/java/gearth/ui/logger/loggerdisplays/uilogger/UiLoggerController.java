package gearth.ui.logger.loggerdisplays.uilogger;

import gearth.misc.harble_api.HarbleAPI;
import gearth.misc.harble_api.HarbleAPIFetcher;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.ui.logger.loggerdisplays.PacketLogger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.StyleSpansBuilder;

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

    private StyleClassedTextArea area;

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
    private final List<Element> appendLater = new ArrayList<>();

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        area = new StyleClassedTextArea();
        area.getStyleClass().add("dark");
        area.setWrapText(true);

        VirtualizedScrollPane<StyleClassedTextArea> vsPane = new VirtualizedScrollPane<>(area);
        borderPane.setCenter(vsPane);

        synchronized (appendLater) {
            initialized = true;
            if (!appendLater.isEmpty()) {
                appendLog(appendLater);
                appendLater.clear();
            }
        }


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

    public void appendMessage(HPacket packet, int types) {
        boolean isBlocked = (types & PacketLogger.MESSAGE_TYPE.BLOCKED.getValue()) != 0;
        boolean isReplaced = (types & PacketLogger.MESSAGE_TYPE.REPLACED.getValue()) != 0;
        boolean isIncoming = (types & PacketLogger.MESSAGE_TYPE.INCOMING.getValue()) != 0;

        if (isIncoming && !viewIncoming) return;
        if (!isIncoming && !viewOutgoing) return;

        ArrayList<Element> elements = new ArrayList<>();

        String expr = packet.toExpression(isIncoming ? HMessage.Side.TOCLIENT : HMessage.Side.TOSERVER);

        lblHarbleAPI.setText("HarbleAPI: " + (HarbleAPIFetcher.HARBLEAPI == null ? "False" : "True"));
        if ((viewMessageName || viewMessageHash) && HarbleAPIFetcher.HARBLEAPI != null) {
            HarbleAPI api = HarbleAPIFetcher.HARBLEAPI;
            HarbleAPI.HarbleMessage message = api.getHarbleMessageFromHeaderId(
                    (isIncoming ? HMessage.Side.TOCLIENT : HMessage.Side.TOSERVER),
                    packet.headerId()
            );

            if ( message != null && !(viewMessageName && !viewMessageHash && message.getName() == null)) {
                if (viewMessageName && message.getName() != null) {
                    elements.add(new Element("["+message.getName()+"]", "messageinfo"));
                }
                if (viewMessageHash) {
                    elements.add(new Element("["+message.getHash()+"]", "messageinfo"));
                }
                elements.add(new Element("\n", ""));
            }
        }

        if (isBlocked) elements.add(new Element("[Blocked]\n", "blocked"));
        else if (isReplaced) elements.add(new Element("[Replaced]\n", "replaced"));

        if (isIncoming) {
            // handle skipped eventually
            elements.add(new Element("Incoming[", "incoming"));
            elements.add(new Element(String.valueOf(packet.headerId()), ""));
            elements.add(new Element("]", "incoming"));

            elements.add(new Element(" <- ", ""));
            if (skiphugepackets && packet.length() > 8000) {
                elements.add(new Element("<packet skipped>", "skipped"));
            }
            else {
                elements.add(new Element(packet.toString(), "incoming"));
            }
        } else {
            elements.add(new Element("Outgoing[", "outgoing"));
            elements.add(new Element(String.valueOf(packet.headerId()), ""));
            elements.add(new Element("]", "outgoing"));

            elements.add(new Element(" -> ", ""));

            if (skiphugepackets && packet.length() > 8000) {
                elements.add(new Element("<packet skipped>", "skipped"));
            }
            else {
                elements.add(new Element(packet.toString(), "outgoing"));
            }
        }
        if (!expr.equals("") && displayStructure && (!skiphugepackets || packet.length() <= 8000))
            elements.add(new Element("\n" + cleanTextContent(expr), "structure"));

        elements.add(new Element("\n--------------------\n", ""));

        synchronized (appendLater) {
            if (initialized) {
                appendLog(elements);
            }
            else {
                appendLater.addAll(elements);
            }
        }

    }

    private synchronized void appendLog(List<Element> elements) {
        Platform.runLater(() -> {
            StringBuilder sb = new StringBuilder();
            StyleSpansBuilder<Collection<String>> styleSpansBuilder = new StyleSpansBuilder<>(0);

            for (Element element : elements) {
                sb.append(element.text);

                styleSpansBuilder.add(Collections.singleton(element.className), element.text.length());
            }

            int oldLen = area.getLength();
            area.appendText(sb.toString());
//            System.out.println(sb.toString());
            area.setStyleSpans(oldLen, styleSpansBuilder.create());

            if (autoScroll) {
                area.moveTo(area.getLength());
                area.requestFollowCaret();
            }
        });
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
        area.clear();
    }
}
