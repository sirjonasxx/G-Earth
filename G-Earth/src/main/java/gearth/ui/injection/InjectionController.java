package gearth.ui.injection;

import gearth.misc.Cacher;
import gearth.services.packet_info.PacketInfoManager;
import gearth.protocol.HMessage;
import gearth.protocol.connection.HState;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import gearth.protocol.HPacket;
import gearth.ui.SubForm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class InjectionController extends SubForm {

    private static final String HISTORY_CACHE_KEY = "INJECTED_HISTORY";
    private static final int historylimit = 69;

    public TextArea inputPacket;
    public Text lbl_corrruption;
    public Text lbl_pcktInfo;
    public Button btn_sendToServer;
    public Button btn_sendToClient;
    public ListView<InjectedPackets> history;
    public Label lblHistory;

    protected void onParentSet() {
        getHConnection().getStateObservable().addListener((oldState, newState) -> Platform.runLater(this::updateUI));

        inputPacket.textProperty().addListener(event -> Platform.runLater(this::updateUI));
    }

    public void initialize() {
        history.setOnMouseClicked(event -> {
            if(event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
                InjectedPackets injectedPackets = history.getSelectionModel().getSelectedItem();
                if (injectedPackets != null) {
                    Platform.runLater(() -> {
                        inputPacket.setText(injectedPackets.getPacketsAsString());
                        updateUI();
                    });
                }
            }
        });

        lblHistory.setTooltip(new Tooltip("Double click a packet to restore it"));

        List<Object> rawHistory = Cacher.getList(HISTORY_CACHE_KEY);
        if (rawHistory != null) {
            List<InjectedPackets> history = rawHistory.stream()
                    .map(o -> (String)o).limit(historylimit - 1).map(InjectedPackets::new).collect(Collectors.toList());
            updateHistoryView(history);
        }
    }

    private static boolean isPacketIncomplete(String line) {
        boolean unmatchedBrace = false;

        boolean ignoreBrace = false;

        for (int i = 0; i < line.length(); i++) {
            if (unmatchedBrace && line.charAt(i) == '"' && line.charAt(i - 1) != '\\') {
                ignoreBrace = !ignoreBrace;
            }

            if (!ignoreBrace) {
                if (line.charAt(i) == '{'){

                    unmatchedBrace = true;
                }
                else if (line.charAt(i) == '}') {
                    unmatchedBrace = false;
                }
            }
        }

        return unmatchedBrace;
    }

    private static HPacket[] parsePackets(String fullText) {
        LinkedList<HPacket> packets = new LinkedList<>();
        String[] lines = fullText.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            while (isPacketIncomplete(line) && i < lines.length - 1)
                line += '\n' + lines[++i];

            packets.add(new HPacket(line));
        }
        return packets.toArray(new HPacket[0]);
    }

    private void updateUI() {
        boolean dirty = false;

        lbl_corrruption.setText("isCorrupted: False");
        lbl_corrruption.setFill(Paint.valueOf("Green"));

        HPacket[] packets = parsePackets(inputPacket.getText());

        if (packets.length == 0) {
            dirty = true;
            lbl_corrruption.setFill(Paint.valueOf("#ee0404b2"));
            lbl_corrruption.setText("isCorrupted: True");
        }

        for (int i = 0; i < packets.length; i++) {
            if (packets[i].isCorrupted()) {
                if (!dirty) {
                    lbl_corrruption.setText("isCorrupted: True -> " + i);
                    lbl_corrruption.setFill(Paint.valueOf("#ee0404b2"));
                    dirty = true;
                } else
                    lbl_corrruption.setText(lbl_corrruption.getText() + ", " + i);
            }
        }

        if (dirty && packets.length == 1) {
            lbl_corrruption.setText("isCorrupted: True"); // no index needed
        }

        if (!dirty) {
            PacketInfoManager packetInfoManager = getHConnection().getPacketInfoManager();

            for (HPacket packet : packets) {
                packet.completePacket(packetInfoManager);
            }

            boolean canSendToClient = Arrays.stream(packets).allMatch(packet ->
                    packet.isPacketComplete() && packet.canSendToClient());
            boolean canSendToServer = Arrays.stream(packets).allMatch(packet ->
                    packet.isPacketComplete() && packet.canSendToServer());

            btn_sendToClient.setDisable(!canSendToClient || getHConnection().getState() != HState.CONNECTED);
            btn_sendToServer.setDisable(!canSendToServer || getHConnection().getState() != HState.CONNECTED);
            if (packets.length == 1) {
                lbl_pcktInfo.setText("header (id:" + packets[0].headerId() + ", length:" +
                        packets[0].length() + ")");
            }
            else {
                lbl_pcktInfo.setText("");
            }
        } else {
            if (packets.length == 1) {
                lbl_pcktInfo.setText("header (id:NULL, length:" + packets[0].getBytesLength()+")");
            }
            else {
                lbl_pcktInfo.setText("");
            }

            btn_sendToClient.setDisable(true);
            btn_sendToServer.setDisable(true);
        }

    }

    public void sendToServer_clicked(ActionEvent actionEvent) {
        HPacket[] packets = parsePackets(inputPacket.getText());
        for (HPacket packet : packets) {
            getHConnection().sendToServer(packet);
            writeToLog(Color.BLUE, "SS -> packet with id: " + packet.headerId());
        }

        addToHistory(packets, inputPacket.getText(), HMessage.Direction.TOSERVER);
    }

    public void sendToClient_clicked(ActionEvent actionEvent) {
        HPacket[] packets = parsePackets(inputPacket.getText());
        for (HPacket packet : packets) {
            getHConnection().sendToClient(packet);
            writeToLog(Color.RED, "CS -> packet with id: " + packet.headerId());
        }

        addToHistory(packets, inputPacket.getText(), HMessage.Direction.TOCLIENT);
    }

    private void addToHistory(HPacket[] packets, String packetsAsString, HMessage.Direction direction) {
        InjectedPackets injectedPackets = new InjectedPackets(packetsAsString, packets.length, getHConnection().getPacketInfoManager(), direction);

        List<InjectedPackets> newHistory = new ArrayList<>();
        newHistory.add(injectedPackets);

        List<Object> rawOldHistory = Cacher.getList(HISTORY_CACHE_KEY);
        if (rawOldHistory != null) {
            List<InjectedPackets> history = rawOldHistory.stream()
                    .map(o -> (String)o).limit(historylimit - 1).map(InjectedPackets::new).collect(Collectors.toList());

            // dont add to history if its equal to the latest added packet
            if (history.size() != 0 && history.get(0).getPacketsAsString().equals(injectedPackets.getPacketsAsString())) {
                return;
            }

            newHistory.addAll(history);
        }

        List<String> historyAsStrings = newHistory.stream().map(InjectedPackets::stringify).collect(Collectors.toList());
        Cacher.put(HISTORY_CACHE_KEY, historyAsStrings);

        updateHistoryView(newHistory);
    }

    private void updateHistoryView(List<InjectedPackets> allHistoryItems) {
        Platform.runLater(() -> {
            history.getItems().clear();
            history.getItems().addAll(allHistoryItems);
        });
    }


    public static void main(String[] args) {
        HPacket[] packets = parsePackets("{l}{h:3}{i:967585}{i:9589}{s:\"furni_inscriptionfuckfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionsssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss\"}{s:\"sirjonasxx-II\"}{s:\"\"}{i:188}{i:0}{i:0}{b:false}");
        System.out.println(new HPacket("{l}{h:2550}{s:\"ClientPerf\"\"ormance\\\"}\"}{s:\"23\"}{s:\"fps\"}{s:\"Avatars: 1, Objects: 0\"}{i:76970180}").toExpression());

        System.out.println("hi");
    }
}
