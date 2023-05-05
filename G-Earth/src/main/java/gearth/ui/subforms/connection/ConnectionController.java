package gearth.ui.subforms.connection;

import gearth.GEarth;
import gearth.misc.BindingsUtil;
import gearth.protocol.HConnection;
import gearth.protocol.connection.HClient;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.proxy.ProxyProviderFactory;
import gearth.services.Constants;
import gearth.ui.GEarthProperties;
import gearth.ui.SubForm;
import gearth.ui.translations.TranslatableString;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionController extends SubForm {

    public ComboBox<String> portOptionsBox;
    public ComboBox<String> hostOptionsBox;

    public Button connectButton;
    public Label
            portOptionsLabel,
            hostOptionsLabel,
            connectedPortLabel,
            connectedHostLabel,
            connectedHotelVersionLabel,
            selectedClientLabel,
            connectionStateHeaderLabel,
            notConnectedStateLabel;

    public TextField connectedHostField;
    public TextField connectedPortField;
    public CheckBox autoDetectBox;
    public TextField connectedHotelVersionField;

    public GridPane clientTypeSelectionGrid;
    public ToggleGroup clientTypeOptions;
    public RadioButton unityOption;
    public RadioButton flashOption;
    public RadioButton nitroOption;

    private final AtomicInteger initState = new AtomicInteger(0);
    private final AtomicInteger initCount = new AtomicInteger(0);

    private TranslatableString connect, state;

    public void initialize() {

        Constants.UNITY_PACKETS = unityOption.isSelected();
        clientTypeOptions.selectedToggleProperty().addListener(observable -> {
            changeClientMode();
            Constants.UNITY_PACKETS = unityOption.isSelected();
        });

        GEarthProperties.clientTypeProperty
                .addListener((observable, oldValue, newValue) -> selectClientType(newValue));
        selectClientType(GEarthProperties.clientTypeProperty.getValue());

        autoDetectBox.selectedProperty().addListener(observable -> updateInputUI());
        portOptionsBox.getEditor().textProperty().addListener(observable -> updateInputUI());

        BindingsUtil.setAndBindBiDirectional(autoDetectBox.selectedProperty(), GEarthProperties.autoDetectProperty);
        BindingsUtil.setAndBindBiDirectional(connectedHostField.textProperty(), GEarthProperties.hostProperty);
        BindingsUtil.setAndBindBiDirectional(connectedPortField.textProperty(), GEarthProperties.portProperty);

        initHostSelection();

        if (initState.incrementAndGet() == 2)
            Platform.runLater(this::updateInputUI);

        tryMaybeConnectOnInit();
        initLanguageBinding();
    }


    @Override
    public void onParentSet() {

        if (initState.incrementAndGet() == 2)
            Platform.runLater(this::updateInputUI);

        getHConnection().stateProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(() -> {
            updateInputUI();
            if (newValue == HState.NOT_CONNECTED) {
                state.setKey(0, "tab.connection.state.notconnected");
                connect.setKey(0, "tab.connection.button.connect");
                connectedHostField.setText("");
                connectedPortField.setText("");
            } else if (oldValue == HState.NOT_CONNECTED)
                connect.setKey(0, "tab.connection.button.abort");
            if (newValue == HState.CONNECTED)
                state.setKey(0, "tab.connection.state.connected");
            if (newValue == HState.WAITING_FOR_CLIENT)
                state.setKey(0, "tab.connection.state.waiting");
            if (newValue == HState.CONNECTED && useFlash()) {
                final String host = getHConnection().getDomain();
                final int port = getHConnection().getServerPort();
                connectedHostField.setText(host);
                connectedPortField.setText(Integer.toString(port));
                GEarthProperties.hostProperty.set(host);
                GEarthProperties.portProperty.set(port);
            }
        }));

        Platform.runLater(this::updateInputUI);

        tryMaybeConnectOnInit();
    }

    @Override
    protected void onExit() {
        GEarthProperties.clientTypeProperty.set(
                flashOption.isSelected() ? HClient.FLASH
                        : unityOption.isSelected() ? HClient.UNITY
                        : HClient.NITRO);
        getHConnection().abort();
    }

    @FXML
    public void onClickConnectButton() {
        if (getHConnection().getState() == HState.NOT_CONNECTED) {

            connectButton.setDisable(true);
            new Thread(() -> {
                if (isClientMode(HClient.FLASH)) {
                    if (autoDetectBox.isSelected()) {
                        getHConnection().start();
                    } else {
                        getHConnection().start(hostOptionsBox.getEditor().getText(), Integer.parseInt(portOptionsBox.getEditor().getText()));
                    }
                } else if (isClientMode(HClient.UNITY)) {
                    getHConnection().startUnity();
                } else if (isClientMode(HClient.NITRO)) {
                    getHConnection().startNitro();
                }
                if (GEarthProperties.isDebugModeEnabled())
                    System.out.println("connecting");
            }).start();
        } else {
            getHConnection().abort();
        }
    }

    private void selectClientType(HClient newValue) {
        switch (newValue) {
            case FLASH:
                flashOption.setSelected(true);
                break;
            case UNITY:
                unityOption.setSelected(true);
                break;
            case NITRO:
                nitroOption.setSelected(true);
                break;
        }
    }

    private void updateInputUI() {
        if (parentController == null) return;

        final HConnection hConnection = getHConnection();
        final HState hConnectionState = hConnection.getState();

        clientTypeSelectionGrid.setDisable(hConnectionState != HState.NOT_CONNECTED);
        connectedHotelVersionField.setText(hConnection.getHotelVersion());

        connectButton.setDisable(hConnectionState == HState.PREPARING || hConnectionState == HState.ABORTING);

        if (!autoDetectBox.isSelected() && !connectButton.isDisable() && useFlash()) {
            try {
                int i = Integer.parseInt(portOptionsBox.getEditor().getText());
                connectButton.setDisable(i < 0 || i >= 256 * 256);
            } catch (Exception e) {
                connectButton.setDisable(true);
            }
        }

        hostOptionsBox.setDisable(hConnectionState != HState.NOT_CONNECTED || autoDetectBox.isSelected());
        portOptionsBox.setDisable(hConnectionState != HState.NOT_CONNECTED || autoDetectBox.isSelected());

        autoDetectBox.setDisable(!useFlash());
        connectedHostField.setDisable(!useFlash());
        connectedPortField.setDisable(!useFlash());

        hostOptionsBox.setDisable(!useFlash() || hConnectionState != HState.NOT_CONNECTED || autoDetectBox.isSelected());
        portOptionsBox.setDisable(!useFlash() || hConnectionState != HState.NOT_CONNECTED || autoDetectBox.isSelected());
    }

    private void tryMaybeConnectOnInit() {
        if (initCount.incrementAndGet() == 2)
            maybeConnectOnInit();
    }

    private void maybeConnectOnInit() {
        String connectMode = GEarth.getArgument("--connect", "-c");
        if (connectMode != null) {
            switch (connectMode) {
                case "flash":
                    Platform.runLater(() -> flashOption.setSelected(true));
                    String host = GEarth.getArgument("--host");
                    String port = GEarth.getArgument("--port");
                    if (host != null && port != null) {
                        Platform.runLater(() -> {
                            if (!hostOptionsBox.getItems().contains(host))
                                hostOptionsBox.getItems().add(host);
                            hostOptionsBox.getSelectionModel().select(host);
                            if (!portOptionsBox.getItems().contains(port))
                                portOptionsBox.getItems().add(port);
                            portOptionsBox.getSelectionModel().select(port);
                            autoDetectBox.setSelected(false);
                        });
                        getHConnection().start(host, Integer.parseInt(port));
                    } else {
                        Platform.runLater(() -> autoDetectBox.setSelected(true));
                        getHConnection().start();
                    }
                    break;
                case "unity":
                    Platform.runLater(() -> unityOption.setSelected(true));
                    getHConnection().startUnity();
                    break;
                case "nitro":
                    Platform.runLater(() -> nitroOption.setSelected(true));
                    getHConnection().startNitro();
                    break;
            }
            Platform.runLater(this::updateInputUI);
        }
    }

    public void changeClientMode() {
        updateInputUI();
    }

    private boolean useFlash() {
        return flashOption.isSelected();
    }

    private boolean isClientMode(HClient client) {
        switch (client) {
            case FLASH:
                return flashOption.isSelected();
            case UNITY:
                return unityOption.isSelected();
            case NITRO:
                return nitroOption.isSelected();
        }

        return false;
    }

    private void initHostSelection() {
        final List<String> knownHosts = ProxyProviderFactory.autoDetectHosts;
        final Set<String> hosts = new HashSet<>();
        final Set<String> ports = new HashSet<>();

        for (String h : knownHosts) {
            String[] split = h.split(":");
            hosts.add(split[0]);
            ports.add(split[1]);
        }

        List<String> hostsSorted = new ArrayList<>(hosts);
        hostsSorted.sort(String::compareTo);

        List<String> portsSorted = new ArrayList<>(ports);
        portsSorted.sort(String::compareTo);

        int hostSelectIndex = 0;
        int portSelectIndex = 0;
        final String hostRemember = GEarthProperties.hostProperty.get();
        final String portRemember = Integer.toString(GEarthProperties.portProperty.get());
        if (hostRemember != null) {
            hostSelectIndex = hostsSorted.indexOf(hostRemember);
            portSelectIndex = portsSorted.indexOf(portRemember);
            hostSelectIndex = Math.max(hostSelectIndex, 0);
            portSelectIndex = Math.max(portSelectIndex, 0);
        }


        portOptionsBox.getItems().addAll(portsSorted);
        hostOptionsBox.getItems().addAll(hostsSorted);

        portOptionsBox.getSelectionModel().select(portSelectIndex);
        hostOptionsBox.getSelectionModel().select(hostSelectIndex);
    }

    private void initLanguageBinding() {
        TranslatableString port = new TranslatableString("%s", "tab.connection.port");
        TranslatableString host = new TranslatableString("%s", "tab.connection.host");
        portOptionsLabel.textProperty().bind(port);
        hostOptionsLabel.textProperty().bind(host);
        connectedPortLabel.textProperty().bind(port);
        connectedHostLabel.textProperty().bind(host);
        autoDetectBox.textProperty().bind(new TranslatableString("%s", "tab.connection.autodetect"));
        connect = new TranslatableString("%s", "tab.connection.button.connect");
        connectButton.textProperty().bind(connect);
        connectedHotelVersionLabel.textProperty().bind(new TranslatableString("%s", "tab.connection.version"));
        selectedClientLabel.textProperty().bind(new TranslatableString("%s", "tab.connection.client"));
        unityOption.textProperty().bind(new TranslatableString("%s", "tab.connection.client.unity"));
        flashOption.textProperty().bind(new TranslatableString("%s", "tab.connection.client.flash"));
        nitroOption.textProperty().bind(new TranslatableString("%s", "tab.connection.client.nitro"));
        connectionStateHeaderLabel.textProperty().bind(new TranslatableString("%s", "tab.connection.state"));
        state = new TranslatableString("%s", "tab.connection.state.notconnected");
        notConnectedStateLabel.textProperty().bind(state);
    }
}
