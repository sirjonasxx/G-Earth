package gearth.ui.subforms.connection;

import gearth.GEarth;
import gearth.misc.BindingsUtil;
import gearth.protocol.HConnection;
import gearth.protocol.connection.HClient;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.proxy.ProxyProviderFactory;
import gearth.services.Constants;
import gearth.ui.GEarthProperties;
import gearth.ui.translations.TranslatableString;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import gearth.ui.SubForm;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConnectionController extends SubForm {

    public ComboBox<String> inpPort;
    public ComboBox<String> inpHost;
    public Button btnConnect;
    public Label lblInpPort, lblInpHost, lblPort, lblHost, lblHotelVersion, lblClient, lblStateHead, lblState;
    public TextField outHost;
    public TextField outPort;
    public CheckBox cbx_autodetect;
    public TextField txtfield_hotelversion;

    private final Object lock = new Object();
    private volatile int fullyInitialized = 0;


    public ToggleGroup tgl_clientMode;
    public RadioButton rd_unity;
    public RadioButton rd_flash;
    public RadioButton rd_nitro;
    public GridPane grd_clientSelection;

    private volatile int initcount = 0;

    private TranslatableString connect, state;

    public void initialize() {

        Constants.UNITY_PACKETS = rd_unity.isSelected();
        tgl_clientMode.selectedToggleProperty().addListener(observable -> {
            changeClientMode();
            Constants.UNITY_PACKETS = rd_unity.isSelected();
        });

        GEarthProperties.clientTypeProperty
                .addListener((observable, oldValue, newValue) -> selectClientType(newValue));
        selectClientType(GEarthProperties.clientTypeProperty.getValue());

        cbx_autodetect.selectedProperty().addListener(observable -> updateInputUI());
        inpPort.getEditor().textProperty().addListener(observable -> updateInputUI());

        BindingsUtil.setAndBindBiDirectional(cbx_autodetect.selectedProperty(), GEarthProperties.autoDetectProperty);
        BindingsUtil.setAndBindBiDirectional(outHost.textProperty(), GEarthProperties.hostProperty);
        BindingsUtil.setAndBindBiDirectional(outPort.textProperty(), GEarthProperties.portProperty);

        List<String> knownHosts = ProxyProviderFactory.autoDetectHosts;
        Set<String> hosts = new HashSet<>();
        Set<String> ports = new HashSet<>();

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


        inpPort.getItems().addAll(portsSorted);
        inpHost.getItems().addAll(hostsSorted);

        inpPort.getSelectionModel().select(portSelectIndex);
        inpHost.getSelectionModel().select(hostSelectIndex);

        synchronized (lock) {
            fullyInitialized++;
            if (fullyInitialized == 2) {
                Platform.runLater(this::updateInputUI);
            }
        }

        synchronized (this) {
            tryMaybeConnectOnInit();
        }

        initLanguageBinding();
    }

    private void selectClientType(HClient newValue) {
        switch (newValue) {
            case FLASH:
                rd_flash.setSelected(true);
                break;
            case UNITY:
                rd_unity.setSelected(true);
                break;
            case NITRO:
                rd_nitro.setSelected(true);
                break;
        }
    }


    private void updateInputUI() {
        if (parentController == null) return;

        final HConnection hConnection = getHConnection();
        final HState hConnectionState = hConnection.getState();

        grd_clientSelection.setDisable(hConnectionState != HState.NOT_CONNECTED);
        txtfield_hotelversion.setText(hConnection.getHotelVersion());

        btnConnect.setDisable(hConnectionState == HState.PREPARING || hConnectionState == HState.ABORTING);


        if (!cbx_autodetect.isSelected() && !btnConnect.isDisable() && useFlash()) {
            try {
                int i = Integer.parseInt(inpPort.getEditor().getText());
                btnConnect.setDisable(i < 0 || i >= 256 * 256);
            }
            catch (Exception e) {
                btnConnect.setDisable(true);
            }
        }

        inpHost.setDisable(hConnectionState != HState.NOT_CONNECTED || cbx_autodetect.isSelected());
        inpPort.setDisable(hConnectionState != HState.NOT_CONNECTED || cbx_autodetect.isSelected());

        cbx_autodetect.setDisable(!useFlash());
        outHost.setDisable(!useFlash());
        outPort.setDisable(!useFlash());

        inpHost.setDisable(!useFlash() || hConnectionState != HState.NOT_CONNECTED || cbx_autodetect.isSelected());
        inpPort.setDisable(!useFlash() || hConnectionState != HState.NOT_CONNECTED || cbx_autodetect.isSelected());
    }

    public void onParentSet(){
        synchronized (lock) {
            fullyInitialized++;
            if (fullyInitialized == 2) {
                Platform.runLater(this::updateInputUI);
            }
        }

        getHConnection().stateProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(() -> {
            updateInputUI();
            if (newValue == HState.NOT_CONNECTED) {
                state.setKey(0, "tab.connection.state.notconnected");
                connect.setKey(0, "tab.connection.button.connect");
                outHost.setText("");
                outPort.setText("");
            }
            else if (oldValue == HState.NOT_CONNECTED)
                connect.setKey(0, "tab.connection.button.abort");
            if (newValue == HState.CONNECTED)
                state.setKey(0, "tab.connection.state.connected");
            if (newValue == HState.WAITING_FOR_CLIENT)
                state.setKey(0, "tab.connection.state.waiting");
            if (newValue == HState.CONNECTED && useFlash()) {
                final String host = getHConnection().getDomain();
                final int port = getHConnection().getServerPort();
                outHost.setText(host);
                outPort.setText(Integer.toString(port));
                GEarthProperties.hostProperty.set(host);
                GEarthProperties.portProperty.set(port);
            }
        }));

        Platform.runLater(this::updateInputUI);

        synchronized (this) {
            tryMaybeConnectOnInit();
        }
    }


    private void tryMaybeConnectOnInit() {
        if (++initcount == 2) {
            maybeConnectOnInit();
        }
    }

    private void maybeConnectOnInit() {
        String connectMode = GEarth.getArgument("--connect", "-c");
        if (connectMode != null) {
            if (connectMode.equals("flash")) {
                Platform.runLater(() -> rd_flash.setSelected(true));
                String host = GEarth.getArgument("--host");
                String port = GEarth.getArgument("--port");
                if (host != null && port != null) {
                    Platform.runLater(() -> {
                        if (!inpHost.getItems().contains(host))
                            inpHost.getItems().add(host);
                        inpHost.getSelectionModel().select(host);
                        if (!inpPort.getItems().contains(port))
                            inpPort.getItems().add(port);
                        inpPort.getSelectionModel().select(port);
                        cbx_autodetect.setSelected(false);
                    });
                    getHConnection().start(host, Integer.parseInt(port));
                }
                else {
                    Platform.runLater(() -> cbx_autodetect.setSelected(true));
                    getHConnection().start();
                }
            }
            else if (connectMode.equals("unity")) {
                Platform.runLater(() -> rd_unity.setSelected(true));
                getHConnection().startUnity();
            }
            else if (connectMode.equals("nitro")) {
                Platform.runLater(() -> rd_nitro.setSelected(true));
                getHConnection().startNitro();
            }
            Platform.runLater(this::updateInputUI);
        }
    }

    public void btnConnect_clicked(ActionEvent actionEvent) {
        if (getHConnection().getState() == HState.NOT_CONNECTED) {

            btnConnect.setDisable(true);
            new Thread(() -> {
                if (isClientMode(HClient.FLASH)) {
                    if (cbx_autodetect.isSelected()) {
                        getHConnection().start();
                    } else {
                        getHConnection().start(inpHost.getEditor().getText(), Integer.parseInt(inpPort.getEditor().getText()));
                    }
                } else if (isClientMode(HClient.UNITY)) {
                    getHConnection().startUnity();
                } else if (isClientMode(HClient.NITRO)) {
                    getHConnection().startNitro();
                }
                if (GEarthProperties.isDebugModeEnabled())
                    System.out.println("connecting");
            }).start();
        }
        else {
            getHConnection().abort();
        }
    }

    @Override
    protected void onExit() {
        GEarthProperties.clientTypeProperty.set(
                rd_flash.isSelected() ? HClient.FLASH
                        : rd_unity.isSelected() ? HClient.UNITY
                        : HClient.NITRO);
        getHConnection().abort();
    }

    public void changeClientMode() {
        updateInputUI();
    }

    private boolean useFlash() {
        return rd_flash.isSelected();
    }

    private boolean isClientMode(HClient client) {
        switch (client) {
            case FLASH:
                return rd_flash.isSelected();
            case UNITY:
                return rd_unity.isSelected();
            case NITRO:
                return rd_nitro.isSelected();
        }

        return false;
    }

    private void initLanguageBinding() {
        TranslatableString port = new TranslatableString("%s", "tab.connection.port");
        TranslatableString host = new TranslatableString("%s", "tab.connection.host");
        lblInpPort.textProperty().bind(port);
        lblInpHost.textProperty().bind(host);
        lblPort.textProperty().bind(port);
        lblHost.textProperty().bind(host);
        cbx_autodetect.textProperty().bind(new TranslatableString("%s", "tab.connection.autodetect"));
        connect = new TranslatableString("%s", "tab.connection.button.connect");
        btnConnect.textProperty().bind(connect);
        lblHotelVersion.textProperty().bind(new TranslatableString("%s", "tab.connection.version"));
        lblClient.textProperty().bind(new TranslatableString("%s", "tab.connection.client"));
        rd_unity.textProperty().bind(new TranslatableString("%s", "tab.connection.client.unity"));
        rd_flash.textProperty().bind(new TranslatableString("%s", "tab.connection.client.flash"));
        rd_nitro.textProperty().bind(new TranslatableString("%s", "tab.connection.client.nitro"));
        lblStateHead.textProperty().bind(new TranslatableString("%s", "tab.connection.state"));
        state = new TranslatableString("%s", "tab.connection.state.notconnected");
        lblState.textProperty().bind(state);
    }
}
