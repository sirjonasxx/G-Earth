package gearth.services.internal_extensions.uilogger;

 import gearth.misc.Cacher;
 import gearth.services.packet_info.PacketInfo;
 import gearth.services.packet_info.PacketInfoManager;
 import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.ui.logger.loggerdisplays.PacketLogger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
 import javafx.fxml.Initializable;
 import javafx.scene.control.*;
 import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
 import javafx.stage.FileChooser;
 import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.StyleSpansBuilder;

 import java.io.BufferedWriter;
 import java.io.File;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.net.URL;
import java.util.*;
 import java.util.stream.Collectors;

public class UiLoggerController implements Initializable {

    private static final String LOGGER_SETTINGS_CACHE = "LOGGER_SETTINGS";

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
    public CheckMenuItem chkAlwaysOnTop;

    public CheckMenuItem chkOpenOnConnect;
    public CheckMenuItem chkResetOnConnect;
    public CheckMenuItem chkHideOnDisconnect;
    public CheckMenuItem chkResetOnDisconnect;

    private final static int FILTER_AMOUNT_THRESHOLD_L = 15;
    private final static int FILTER_AMOUNT_THRESHOLD_M = 9;
    private final static int FILTER_AMOUNT_THRESHOLD_H = 4;
    private final static int FILTER_AMOUNT_THRESHOLD_U = 2;
    private final static int FILTER_TIME_THRESHOLD = 5000;

    public RadioMenuItem chkAntiSpam_none;
    public RadioMenuItem chkAntiSpam_low;
    public RadioMenuItem chkAntiSpam_medium;
    public RadioMenuItem chkAntiSpam_high;
    public RadioMenuItem chkAntiSpam_ultra;
    public Label lblFiltered;
    public CheckMenuItem chkTimestamp;

    private Map<Integer, LinkedList<Long>> filterTimestamps = new HashMap<>();

    private StyleClassedTextArea area;

    private Stage stage;
    private PacketInfoManager packetInfoManager = null;

    private int filteredAmount = 0;

    private volatile boolean initialized = false;
    private final List<Element> appendLater = new ArrayList<>();

    private List<MenuItem> allMenuItems = new ArrayList<>();

    private boolean isSelected(MenuItem item) {
        if (item instanceof CheckMenuItem) {
            return ((CheckMenuItem)item).isSelected();
        }
        if (item instanceof RadioMenuItem) {
            return ((RadioMenuItem)item).isSelected();
        }
        return false;
    }

    private void setSelected(MenuItem item, boolean selected) {
        if (item instanceof CheckMenuItem) {
            ((CheckMenuItem)item).setSelected(selected);
        }
        if (item instanceof RadioMenuItem) {
            ((RadioMenuItem)item).setSelected(selected);
        }
    }


    private void saveAllMenuItems() {
        List<Boolean> selection = new ArrayList<>();
        for (MenuItem menuItem : allMenuItems) {
            selection.add(isSelected(menuItem));
        }

        Cacher.put(LOGGER_SETTINGS_CACHE, selection);
    }

    private void loadAllMenuItems() {
        List<Object> selectedMenuItems = Cacher.getList(LOGGER_SETTINGS_CACHE);
        if (selectedMenuItems != null) {
            for (int i = 0; i < selectedMenuItems.size(); i++) {
                boolean isSelected = (boolean) selectedMenuItems.get(i);
                setSelected(allMenuItems.get(i), isSelected);
            }
        }
    }

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        allMenuItems.addAll(Arrays.asList(
                chkViewIncoming, chkViewOutgoing, chkDisplayStructure, chkAutoscroll,
                chkSkipBigPackets, chkMessageName, chkMessageHash, chkUseNewStructures,
                chkOpenOnConnect, chkResetOnConnect, chkHideOnDisconnect, chkResetOnDisconnect,
                chkAntiSpam_none, chkAntiSpam_low, chkAntiSpam_medium, chkAntiSpam_high, chkAntiSpam_ultra,
                chkTimestamp
        ));
        loadAllMenuItems();

        for (MenuItem item : allMenuItems) {
            item.setOnAction(event -> {
                saveAllMenuItems();
                updateLoggerInfo();
            });
        }

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
                chkAntiSpam_low.isSelected() ? FILTER_AMOUNT_THRESHOLD_L : (
                        chkAntiSpam_medium.isSelected() ? FILTER_AMOUNT_THRESHOLD_M : (
                                chkAntiSpam_high.isSelected() ? FILTER_AMOUNT_THRESHOLD_H : FILTER_AMOUNT_THRESHOLD_U
                        )
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
            if (filter) {
                filteredAmount++;
                updateLoggerInfo();
                return;
            }
        }

        if (isIncoming && !chkViewIncoming.isSelected()) return;
        if (!isIncoming && !chkViewOutgoing.isSelected()) return;

        ArrayList<Element> elements = new ArrayList<>();


        if (chkTimestamp.isSelected()) {
            elements.add(new Element(String.format("(timestamp: %d)\n", System.currentTimeMillis()), "timestamp"));
        }

        boolean packetInfoAvailable = packetInfoManager.getPacketInfoList().size() > 0;

        if ((chkMessageName.isSelected() || chkMessageHash.isSelected()) && packetInfoAvailable) {
            List<PacketInfo> messages = packetInfoManager.getAllPacketInfoFromHeaderId(
                    (isIncoming ? HMessage.Direction.TOCLIENT : HMessage.Direction.TOSERVER),
                    packet.headerId()
            );
            List<String> names = messages.stream().map(PacketInfo::getName)
                    .filter(Objects::nonNull).distinct().collect(Collectors.toList());
            List<String> hashes = messages.stream().map(PacketInfo::getHash)
                    .filter(Objects::nonNull).distinct().collect(Collectors.toList());

            boolean addedSomething = false;
            if (chkMessageName.isSelected() && names.size() > 0) {
                for (String name : names) {elements.add(new Element("["+name+"]", "messageinfo")); }
                addedSomething = true;
            }
            if (chkMessageHash.isSelected() && hashes.size() > 0) {
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
            if (chkSkipBigPackets.isSelected() && packet.length() > 4000) {
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

            if (chkSkipBigPackets.isSelected() && packet.length() > 4000) {
                elements.add(new Element("<packet skipped>", "skipped"));
            }
            else {
                elements.add(new Element(packet.toString(), "outgoing"));
            }
        }

        if (packet.length() <= 2000) {
            try {
                String expr = packet.toExpression(isIncoming ? HMessage.Direction.TOCLIENT : HMessage.Direction.TOSERVER, packetInfoManager, chkUseNewStructures.isSelected());
                String cleaned = cleanTextContent(expr);
                if (cleaned.equals(expr)) {
                    if (!expr.equals("") && chkDisplayStructure.isSelected())
                        elements.add(new Element("\n" + cleanTextContent(expr), "structure"));
                }
            }
            catch (Exception e) {
                System.out.println(packet.toString());
                System.out.println("if you see this message pls report it");
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
            area.setStyleSpans(oldLen, styleSpansBuilder.create());

            if (chkAutoscroll.isSelected()) {
                area.requestFollowCaret();
            }
        });
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void updateLoggerInfo() {
        Platform.runLater(() -> {
            lblViewIncoming.setText("View Incoming: " + (chkViewIncoming.isSelected() ? "True" : "False"));
            lblViewOutgoing.setText("View Outgoing: " + (chkViewOutgoing.isSelected() ? "True" : "False"));
            lblAutoScrolll.setText("Autoscroll: " + (chkAutoscroll.isSelected() ? "True" : "False"));
            lblFiltered.setText("Filtered: " + filteredAmount);

            boolean packetInfoAvailable = packetInfoManager.getPacketInfoList().size() > 0;
            lblPacketInfo.setText("Packet info: " + (packetInfoAvailable ? "True" : "False"));
        });
    }

    public void setPacketInfoManager(PacketInfoManager packetInfoManager) {
        this.packetInfoManager = packetInfoManager;
        Platform.runLater(this::updateLoggerInfo);
    }

    public void toggleAlwaysOnTop(ActionEvent actionEvent) {
        stage.setAlwaysOnTop(chkAlwaysOnTop.isSelected());
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

    public void exportAll(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();

        //Set extension filter
        FileChooser.ExtensionFilter extFilter =
                new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setTitle("Save Packets");

        //Show save file dialog
        File file = fileChooser.showSaveDialog(stage);

        if(file != null){
            try {
                FileWriter fileWriter = new FileWriter(file);
                BufferedWriter out = new BufferedWriter(fileWriter);

                out.write(area.getText());

                out.flush();
                out.close();
                fileWriter.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }
    }
}
