package gearth.ui.subforms.injection;

import gearth.misc.Cacher;
import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.ui.GEarthProperties;
import gearth.ui.SubForm;
import gearth.ui.translations.LanguageBundle;
import gearth.ui.translations.TranslatableString;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InjectionController extends SubForm implements Initializable {

    private static final String HISTORY_CACHE_KEY = "INJECTED_HISTORY";
    private static final int MAX_HISTORY_ENTRIES = 69;

    public TextArea packetTextArea;
    public Text packetCorruptedText;
    public Text packetInfoText;
    public Button sendToServerButton;
    public Button sendToClientButton;
    public ListView<InjectedPackets> historyView;
    public Label historyLabel;
    public Hyperlink clearHistoryLink;

    private TranslatableString packetCorruptedString;
    private TranslatableString packetInfoString;

    @Override
    protected void onParentSet() {
        final InvalidationListener updateUI = observable -> Platform.runLater(this::updateUI);
        GEarthProperties.enableDeveloperModeProperty.addListener(updateUI);
        getHConnection().stateProperty().addListener(updateUI);
        packetTextArea.textProperty().addListener(updateUI);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        historyView.setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
                final InjectedPackets injectedPackets = historyView.getSelectionModel().getSelectedItem();
                if (injectedPackets != null) {
                    Platform.runLater(() -> {
                        packetTextArea.setText(injectedPackets.getPacketsAsString());
                        updateUI();
                    });
                }
            }
        });
        findInjectedPackets().ifPresent(this::updateHistoryView);
        initLanguageBinding();
    }

    @FXML
    public void onClickSendToServer() {
        final HPacket[] packets = parsePackets(packetTextArea.getText());
        for (HPacket packet : packets) {
            getHConnection().sendToServer(packet);
            writeToLog(Color.BLUE, String.format("SS -> %s: %d", LanguageBundle.get("tab.injection.log.packetwithid"), packet.headerId()));
        }
        addToHistory(packets, packetTextArea.getText(), HMessage.Direction.TOSERVER);
    }

    @FXML
    public void onClickSendToClient() {
        final HPacket[] packets = parsePackets(packetTextArea.getText());
        for (HPacket packet : packets) {
            getHConnection().sendToClient(packet);
            writeToLog(Color.RED, String.format("CS -> %s: %d", LanguageBundle.get("tab.injection.log.packetwithid"), packet.headerId()));
        }
        addToHistory(packets, packetTextArea.getText(), HMessage.Direction.TOCLIENT);
    }

    @FXML
    public void onClickClearHistory() {
        Cacher.put(HISTORY_CACHE_KEY, Collections.emptyList());
        updateHistoryView(Collections.emptyList());
    }

    private void addToHistory(HPacket[] packets, String packetsAsString, HMessage.Direction direction) {

        final InjectedPackets injectedPackets = new InjectedPackets(packetsAsString, packets.length, getHConnection().getPacketInfoManager(), direction);

        final List<InjectedPackets> history = Stream
                .concat(
                        Stream.of(injectedPackets),
                        findInjectedPackets()
                                .filter(old -> !old.isEmpty() && !old.get(0).getPacketsAsString().equals(injectedPackets.getPacketsAsString()))
                                .orElseGet(Collections::emptyList)
                                .stream())
                .collect(Collectors.toList());
        updateHistoryView(history);

        final List<String> historyAsString = history.stream().map(InjectedPackets::stringify).collect(Collectors.toList());
        Cacher.put(HISTORY_CACHE_KEY, historyAsString);
    }

    private void updateHistoryView(List<InjectedPackets> allHistoryItems) {
        Platform.runLater(() -> historyView.getItems().setAll(allHistoryItems));
    }

    private void updateUI() {
        boolean dirty = false;

        packetCorruptedString.setFormat("%s: %s");
        packetCorruptedString.setKey(1, "tab.injection.corrupted.false");
        packetCorruptedText.getStyleClass().setAll("not-corrupted-label");

        final HPacket[] packets = parsePackets(packetTextArea.getText());

        if (packets.length == 0) {
            dirty = true;
            packetCorruptedText.setFill(Paint.valueOf("#ee0404b2"));
            packetCorruptedText.getStyleClass().setAll("corrupted-label");
        }

        for (int i = 0; i < packets.length; i++) {
            if (packets[i].isCorrupted()) {
                if (!dirty) {
                    packetCorruptedString.setFormat("%s: %s -> " + i);
                    packetCorruptedString.setKey(1, "tab.injection.corrupted.true");
                    packetCorruptedText.getStyleClass().setAll("corrupted-label");
                    dirty = true;
                } else
                    packetCorruptedString.setFormat(packetCorruptedString.getFormat() + ", " + i);
            }
        }

        if (dirty && packets.length == 1) {
            packetCorruptedString.setFormatAndKeys("%s: %s", "tab.injection.corrupted", "tab.injection.corrupted.true");
        }

        if (!dirty) {

            final HConnection connection = getHConnection();

            final boolean canSendToClient = Arrays.stream(packets).allMatch(packet ->
                    connection.canSendPacket(HMessage.Direction.TOCLIENT, packet));
            final boolean canSendToServer = Arrays.stream(packets).allMatch(packet ->
                    connection.canSendPacket(HMessage.Direction.TOSERVER, packet));

            sendToClientButton.setDisable(!canSendToClient);
            sendToServerButton.setDisable(!canSendToServer);

            // mark packet sending unsafe if there is any packet that is unsafe for both TOSERVER and TOCLIENT
            boolean isUnsafe = Arrays.stream(packets).anyMatch(packet ->
                    !connection.isPacketSendingSafe(HMessage.Direction.TOCLIENT, packet) &&
                            !connection.isPacketSendingSafe(HMessage.Direction.TOSERVER, packet));

            if (packets.length == 1) {
                HPacket packet = packets[0];
                if (isUnsafe) {
                    packetInfoString.setFormatAndKeys("%s (%s: " + packet.headerId() + ", %s: " + packet.length() + "), %s",
                            "tab.injection.description.header",
                            "tab.injection.description.id",
                            "tab.injection.description.length",
                            "tab.injection.description.unsafe");
                } else {
                    packetInfoString.setFormatAndKeys("%s (%s: " + packet.headerId() + ", %s: " + packet.length() + ")",
                            "tab.injection.description.header",
                            "tab.injection.description.id",
                            "tab.injection.description.length");
                }
            } else {
                if (isUnsafe) {
                    packetInfoString.setFormatAndKeys("%s", "tab.injection.description.unsafe");
                } else {
                    packetInfoString.setFormatAndKeys("");
                }
            }
        } else {
            if (packets.length == 1) {
                packetInfoString.setFormatAndKeys("%s (%s:NULL, %s: " + packets[0].getBytesLength() + ")",
                        "tab.injection.description.header",
                        "tab.injection.description.id",
                        "tab.injection.description.length");
            } else {
                packetInfoString.setFormatAndKeys("");
            }
            sendToClientButton.setDisable(true);
            sendToServerButton.setDisable(true);
        }
    }

    private void initLanguageBinding() {
        historyLabel.textProperty().bind(new TranslatableString("%s", "tab.injection.history"));
        historyLabel.setTooltip(new Tooltip());
        historyLabel.getTooltip().textProperty().bind(new TranslatableString("%s", "tab.injection.history.tooltip"));

        packetCorruptedString = new TranslatableString("%s: %s", "tab.injection.corrupted", "tab.injection.corrupted.true");
        packetCorruptedText.textProperty().bind(packetCorruptedString);

        packetInfoString = new TranslatableString("%s (%s:NULL, %s:0)", "tab.injection.description.header", "tab.injection.description.id", "tab.injection.description.length");
        packetInfoText.textProperty().bind(packetInfoString);

        sendToServerButton.textProperty().bind(new TranslatableString("%s", "tab.injection.send.toserver"));
        sendToClientButton.textProperty().bind(new TranslatableString("%s", "tab.injection.send.toclient"));

        clearHistoryLink.textProperty().bind(new TranslatableString("%s", "tab.injection.history.clear"));
    }

    private static Optional<List<InjectedPackets>> findInjectedPackets() {
        return Optional.ofNullable(Cacher.getList(HISTORY_CACHE_KEY))
                .map(rawList -> rawList.stream()
                        .limit(MAX_HISTORY_ENTRIES - 1)
                        .map(o -> (String) o)
                        .map(InjectedPackets::new)
                        .collect(Collectors.toList()));
    }

    private static HPacket[] parsePackets(String fullText) {
        final LinkedList<HPacket> packets = new LinkedList<>();
        final String[] lines = fullText.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            while (isPacketIncomplete(line) && i < lines.length - 1)
                line += '\n' + lines[++i];
            packets.add(new HPacket(line));
        }
        return packets.toArray(new HPacket[0]);
    }

    private static boolean isPacketIncomplete(String line) {
        boolean unmatchedBrace = false;
        boolean ignoreBrace = false;
        for (int i = 0; i < line.length(); i++) {
            final char c = line.charAt(i);
            if (unmatchedBrace && c == '"' && line.charAt(i - 1) != '\\')
                ignoreBrace = !ignoreBrace;
            if (!ignoreBrace)
                unmatchedBrace = c == '{' || (c != '}' && unmatchedBrace);
        }
        return unmatchedBrace;
    }
}
