package gearth.services.internal_extensions.uilogger;

 import gearth.misc.packet_info.PacketInfo;
 import gearth.misc.packet_info.PacketInfoManager;
 import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.ui.logger.loggerdisplays.PacketLogger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
 import javafx.scene.control.RadioMenuItem;
 import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.net.URL;
import java.util.*;
 import java.util.stream.Collectors;

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
    public Label lblPacketInfo;
    public CheckMenuItem chkUseNewStructures;

    public CheckMenuItem chkOpenOnConnect;
    public CheckMenuItem chkResetOnConnect;
    public CheckMenuItem chkHideOnDisconnect;
    public CheckMenuItem chkResetOnDisconnect;

    private final static int FILTER_AMOUNT_THRESHOLD_S = 15;
    private final static int FILTER_AMOUNT_THRESHOLD_M = 9;
    private final static int FILTER_AMOUNT_THRESHOLD_H = 4;
    private final static int FILTER_TIME_THRESHOLD = 5000;

    public RadioMenuItem chkAntiSpam_none;
    public RadioMenuItem chkAntiSpam_low;
    public RadioMenuItem chkAntiSpam_medium;
    public RadioMenuItem chkAntiSpam_high;

    private Map<Integer, LinkedList<Long>> filterTimestamps = new HashMap<>();

    private StyleClassedTextArea area;

    private Stage stage;
    private PacketInfoManager packetInfoManager = null;

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


    private boolean checkFilter(HPacket packet) {
        int headerId = packet.headerId();

        int threshold = chkAntiSpam_none.isSelected() ? 100000000 : (
                chkAntiSpam_low.isSelected() ? FILTER_AMOUNT_THRESHOLD_S : (
                        chkAntiSpam_medium.isSelected() ? FILTER_AMOUNT_THRESHOLD_M : FILTER_AMOUNT_THRESHOLD_H
                )
        );

        if (!filterTimestamps.containsKey(headerId)) {
            filterTimestamps.put(headerId, new LinkedList<>());
        }

        long queueRemoveThreshold = System.currentTimeMillis() - FILTER_TIME_THRESHOLD;
        LinkedList<Long> list = filterTimestamps.get(headerId);
        while (!list.isEmpty() && list.get(0) < queueRemoveThreshold) list.removeFirst();

        if (list.size() == threshold) list.removeFirst();
        list.add(System.currentTimeMillis());

        return list.size() >= threshold;
    }

    public void appendMessage(HPacket packet, int types) {
        boolean isBlocked = (types & PacketLogger.MESSAGE_TYPE.BLOCKED.getValue()) != 0;
        boolean isReplaced = (types & PacketLogger.MESSAGE_TYPE.REPLACED.getValue()) != 0;
        boolean isIncoming = (types & PacketLogger.MESSAGE_TYPE.INCOMING.getValue()) != 0;

        if (isIncoming && !isBlocked && !isReplaced) {
            boolean filter = checkFilter(packet);
            if (filter) return;
        }

        if (isIncoming && !viewIncoming) return;
        if (!isIncoming && !viewOutgoing) return;

        ArrayList<Element> elements = new ArrayList<>();


        boolean packetInfoAvailable = packetInfoManager.getPacketInfoList().size() > 0;

        if ((viewMessageName || viewMessageHash) && packetInfoAvailable) {
            List<PacketInfo> messages = packetInfoManager.getAllPacketInfoFromHeaderId(
                    (isIncoming ? HMessage.Direction.TOCLIENT : HMessage.Direction.TOSERVER),
                    packet.headerId()
            );
            List<String> names = messages.stream().map(PacketInfo::getName)
                    .filter(Objects::nonNull).distinct().collect(Collectors.toList());
            List<String> hashes = messages.stream().map(PacketInfo::getHash)
                    .filter(Objects::nonNull).distinct().collect(Collectors.toList());

            boolean addedSomething = false;
            if (viewMessageName && names.size() > 0) {
                for (String name : names) {elements.add(new Element("["+name+"]", "messageinfo")); }
                addedSomething = true;
            }
            if (viewMessageHash && hashes.size() > 0) {
                for (String hash : hashes) {elements.add(new Element("["+hash+"]", "messageinfo")); }
                addedSomething = true;
            }

            if (addedSomething) {
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
            if (skiphugepackets && packet.length() > 4000) {
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

            if (skiphugepackets && packet.length() > 4000) {
                elements.add(new Element("<packet skipped>", "skipped"));
            }
            else {
                elements.add(new Element(packet.toString(), "outgoing"));
            }
        }

        if (packet.length() <= 2000) {
            String expr = packet.toExpression(isIncoming ? HMessage.Direction.TOCLIENT : HMessage.Direction.TOSERVER, packetInfoManager, chkUseNewStructures.isSelected());
            String cleaned = cleanTextContent(expr);
            if (cleaned.equals(expr)) {
                if (!expr.equals("") && displayStructure)
                    elements.add(new Element("\n" + cleanTextContent(expr), "structure"));
            }
        }


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
//                area.moveTo(area.getLength());
                area.requestFollowCaret();
            }
        });
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setPacketInfoManager(PacketInfoManager packetInfoManager) {
        this.packetInfoManager = packetInfoManager;
        Platform.runLater(() -> {
            boolean packetInfoAvailable = packetInfoManager.getPacketInfoList().size() > 0;
            lblPacketInfo.setText("Packet info: " + (packetInfoAvailable ? "True" : "False"));
        });
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

    public void onDisconnect() {
        Platform.runLater(() -> {
            if (chkHideOnDisconnect.isSelected()) {
                stage.hide();
            }
            if (chkResetOnDisconnect.isSelected()) {
                clearText(null);
            }
        });
    }

    public void onConnect() {
        Platform.runLater(() -> {
            if (chkResetOnConnect.isSelected()) {
                clearText(null);
            }
            if (chkOpenOnConnect.isSelected()) {
                stage.show();
            }
        });
    }
}
