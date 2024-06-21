package gearth.ui.subforms.injection;

import gearth.misc.Cacher;
import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.connection.HClient;
import gearth.protocol.packethandler.shockwave.packets.ShockPacket;
import gearth.ui.SubForm;
import gearth.ui.translations.LanguageBundle;
import gearth.ui.translations.TranslatableString;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class InjectionController extends SubForm {

    private static final String HISTORY_CACHE_KEY = "INJECTED_HISTORY";
    private static final int historylimit = 69;

    public TextArea inputPacket;
    public Text lbl_corruption;
    public Text lbl_pcktInfo;
    public Button btn_sendToServer;
    public Button btn_sendToClient;
    public ListView<InjectedPackets> history;
    public Label lblHistory;
    public Hyperlink lnk_clearHistory;

    private TranslatableString corruption, pcktInfo;

    protected void onParentSet() {
        getHConnection().onDeveloperModeChange(developMode -> updateUI());
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

        List<Object> rawHistory = Cacher.getList(HISTORY_CACHE_KEY);
        if (rawHistory != null) {
            List<InjectedPackets> history = rawHistory.stream()
                    .map(o -> (String)o).limit(historylimit - 1).map(InjectedPackets::new).collect(Collectors.toList());
            updateHistoryView(history);
        }

        initLanguageBinding();
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

    private static HPacket[] parsePackets(HClient client, String fullText) {
        LinkedList<HPacket> packets = new LinkedList<>();
        String[] lines = fullText.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            while (isPacketIncomplete(line) && i < lines.length - 1)
                line += '\n' + lines[++i];

            packets.add(client == HClient.SHOCKWAVE
                    ? new ShockPacket(line)
                    : new HPacket(line));
        }
        return packets.toArray(new HPacket[0]);
    }

    private void updateUI() {
        boolean dirty = false;

        corruption.setFormat("%s: %s");
        corruption.setKey(1, "tab.injection.corrupted.false");
        lbl_corruption.getStyleClass().clear();
        lbl_corruption.getStyleClass().add("not-corrupted-label");

        HPacket[] packets = parsePackets(getHConnection().getClientType(), inputPacket.getText());

        if (packets.length == 0) {
            dirty = true;
            lbl_corruption.setFill(Paint.valueOf("#ee0404b2"));
            lbl_corruption.getStyleClass().clear();
            lbl_corruption.getStyleClass().add("corrupted-label");
        }

        for (int i = 0; i < packets.length; i++) {
            if (packets[i].isCorrupted()) {
                if (!dirty) {
                    corruption.setFormat("%s: %s -> " + i);
                    corruption.setKey(1, "tab.injection.corrupted.true");
                    lbl_corruption.getStyleClass().clear();
                    lbl_corruption.getStyleClass().add("corrupted-label");
                    dirty = true;
                } else
                    corruption.setFormat(corruption.getFormat() + ", " + i);
            }
        }

        if (dirty && packets.length == 1) {
            corruption.setFormatAndKeys("%s: %s", "tab.injection.corrupted", "tab.injection.corrupted.true");
        }

        if (!dirty) {

            HConnection connection = getHConnection();

            boolean canSendToClient = Arrays.stream(packets).allMatch(packet ->
                    connection.canSendPacket(HMessage.Direction.TOCLIENT, packet));
            boolean canSendToServer = Arrays.stream(packets).allMatch(packet ->
                    connection.canSendPacket(HMessage.Direction.TOSERVER, packet));

            btn_sendToClient.setDisable(!canSendToClient);
            btn_sendToServer.setDisable(!canSendToServer);

            // mark packet sending unsafe if there is any packet that is unsafe for both TOSERVER and TOCLIENT
            boolean isUnsafe = Arrays.stream(packets).anyMatch(packet ->
                    !connection.isPacketSendingSafe(HMessage.Direction.TOCLIENT, packet) &&
                            !connection.isPacketSendingSafe(HMessage.Direction.TOSERVER, packet));

            if (packets.length == 1) {
                HPacket packet = packets[0];
                if (isUnsafe) {
                    pcktInfo.setFormatAndKeys("%s (%s: " + packet.headerId() + ", %s: " + packet.length() + "), %s",
                            "tab.injection.description.header",
                            "tab.injection.description.id",
                            "tab.injection.description.length",
                            "tab.injection.description.unsafe");
                }
                else {
                    pcktInfo.setFormatAndKeys("%s (%s: " + packet.headerId() + ", %s: " + packet.length() + ")",
                            "tab.injection.description.header",
                            "tab.injection.description.id",
                            "tab.injection.description.length");
                }
            }
            else {
                if (isUnsafe) {
                    pcktInfo.setFormatAndKeys("%s", "tab.injection.description.unsafe");
                }
                else {
                    pcktInfo.setFormatAndKeys("");
                }
            }
        } else {
            if (packets.length == 1) {
                pcktInfo.setFormatAndKeys("%s (%s:NULL, %s: " + packets[0].getBytesLength() + ")",
                        "tab.injection.description.header",
                        "tab.injection.description.id",
                        "tab.injection.description.length");
            }
            else {
                pcktInfo.setFormatAndKeys("");
            }

            btn_sendToClient.setDisable(true);
            btn_sendToServer.setDisable(true);
        }

    }

    public void sendToServer_clicked(ActionEvent actionEvent) {
        HPacket[] packets = parsePackets(getHConnection().getClientType(), inputPacket.getText());
        for (HPacket packet : packets) {
            getHConnection().sendToServer(packet);
            writeToLog(Color.BLUE, String.format("SS -> %s: %d", LanguageBundle.get("tab.injection.log.packetwithid"), packet.headerId()));
        }

        addToHistory(packets, inputPacket.getText(), HMessage.Direction.TOSERVER);
    }

    public void sendToClient_clicked(ActionEvent actionEvent) {
        HPacket[] packets = parsePackets(getHConnection().getClientType(), inputPacket.getText());
        for (HPacket packet : packets) {
            getHConnection().sendToClient(packet);
            writeToLog(Color.RED, String.format("CS -> %s: %d", LanguageBundle.get("tab.injection.log.packetwithid"), packet.headerId()));
        }

        addToHistory(packets, inputPacket.getText(), HMessage.Direction.TOCLIENT);
    }

    private void addToHistory(HPacket[] packets, String packetsAsString, HMessage.Direction direction) {
        InjectedPackets injectedPackets = new InjectedPackets(packetsAsString, packets.length, getHConnection().getPacketInfoManager(), direction, getHConnection().getClientType());

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

    public void clearHistoryClick(ActionEvent actionEvent) {
        Cacher.put(HISTORY_CACHE_KEY, new ArrayList<>());
        updateHistoryView(new ArrayList<>());
    }

    private void initLanguageBinding() {
        lblHistory.textProperty().bind(new TranslatableString("%s", "tab.injection.history"));
        lblHistory.setTooltip(new Tooltip());
        lblHistory.getTooltip().textProperty().bind(new TranslatableString("%s", "tab.injection.history.tooltip"));

        corruption = new TranslatableString("%s: %s", "tab.injection.corrupted", "tab.injection.corrupted.true");
        lbl_corruption.textProperty().bind(corruption);

        pcktInfo = new TranslatableString("%s (%s:NULL, %s:0)", "tab.injection.description.header", "tab.injection.description.id", "tab.injection.description.length");
        lbl_pcktInfo.textProperty().bind(pcktInfo);

        btn_sendToServer.textProperty().bind(new TranslatableString("%s", "tab.injection.send.toserver"));
        btn_sendToClient.textProperty().bind(new TranslatableString("%s", "tab.injection.send.toclient"));

        lnk_clearHistory.textProperty().bind(new TranslatableString("%s", "tab.injection.history.clear"));
    }

    public static void main(String[] args) {
        HPacket[] packets = parsePackets(HClient.FLASH, "{l}{h:3}{i:967585}{i:9589}{s:\"furni_inscriptionfuckfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionsssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss\"}{s:\"sirjonasxx-II\"}{s:\"\"}{i:188}{i:0}{i:0}{b:false}");
        System.out.println(new HPacket("{l}{h:2550}{s:\"ClientPerf\"\"ormance\\\"}\"}{s:\"23\"}{s:\"fps\"}{s:\"Avatars: 1, Objects: 0\"}{i:76970180}").toExpression());

        System.out.println("hi");
    }
}
