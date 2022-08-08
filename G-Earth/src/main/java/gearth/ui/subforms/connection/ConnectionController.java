package gearth.ui.subforms.connection;

import gearth.GEarth;
import gearth.misc.Cacher;
import gearth.protocol.connection.HClient;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.proxy.ProxyProviderFactory;
import gearth.services.Constants;
import gearth.ui.translations.TranslatableString;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import gearth.protocol.HConnection;
import gearth.ui.SubForm;
import javafx.scene.layout.GridPane;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConnectionController extends SubForm {

    private final String CONNECTION_INFO_CACHE_KEY = "last_connection_settings";
    private final String AUTODETECT_CACHE = "auto_detect";
    private final String HOST_CACHE = "host";
    private final String PORT_CACHE = "port";

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


    public static final String CLIENT_CACHE_KEY = "last_client_mode";
    public ToggleGroup tgl_clientMode;
    public RadioButton rd_unity;
    public RadioButton rd_flash;
    public RadioButton rd_nitro;
    public GridPane grd_clientSelection;

    private volatile int initcount = 0;

    public void initialize() {

        Constants.UNITY_PACKETS = rd_unity.isSelected();
        tgl_clientMode.selectedToggleProperty().addListener(observable -> {
            changeClientMode();
            Constants.UNITY_PACKETS = rd_unity.isSelected();
        });

        if (Cacher.getCacheContents().has(CLIENT_CACHE_KEY)) {
            switch (Cacher.getCacheContents().getEnum(HClient.class, CLIENT_CACHE_KEY)) {
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


        Object object;
        String hostRemember = null;
        String portRemember = null;
        if ((object = Cacher.get(CONNECTION_INFO_CACHE_KEY)) != null) {
            JSONObject connectionSettings = (JSONObject) object;
            boolean autoDetect = connectionSettings.getBoolean(AUTODETECT_CACHE);
            hostRemember = connectionSettings.getString(HOST_CACHE);
            portRemember = connectionSettings.getInt(PORT_CACHE) + "";
            cbx_autodetect.setSelected(autoDetect);
        }

        inpPort.getEditor().textProperty().addListener(observable -> {
            updateInputUI();
        });
        cbx_autodetect.selectedProperty().addListener(observable -> {
            updateInputUI();
        });

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



    private void updateInputUI() {
        if (parentController == null) return;

        grd_clientSelection.setDisable(getHConnection().getState() != HState.NOT_CONNECTED);
        txtfield_hotelversion.setText(getHConnection().getHotelVersion());

        btnConnect.setDisable(getHConnection().getState() == HState.PREPARING || getHConnection().getState() == HState.ABORTING);


        if (!cbx_autodetect.isSelected() && !btnConnect.isDisable() && useFlash()) {
            try {
                int i = Integer.parseInt(inpPort.getEditor().getText());
                btnConnect.setDisable(i < 0 || i >= 256 * 256);
            }
            catch (Exception e) {
                btnConnect.setDisable(true);
            }
        }

        inpHost.setDisable(getHConnection().getState() != HState.NOT_CONNECTED || cbx_autodetect.isSelected());
        inpPort.setDisable(getHConnection().getState() != HState.NOT_CONNECTED || cbx_autodetect.isSelected());

        cbx_autodetect.setDisable(!useFlash());
        outHost.setDisable(!useFlash());
        outPort.setDisable(!useFlash());

        inpHost.setDisable(!useFlash() || getHConnection().getState() != HState.NOT_CONNECTED || cbx_autodetect.isSelected());
        inpPort.setDisable(!useFlash() || getHConnection().getState() != HState.NOT_CONNECTED || cbx_autodetect.isSelected());
    }

    public void onParentSet(){
        synchronized (lock) {
            fullyInitialized++;
            if (fullyInitialized == 2) {
                Platform.runLater(this::updateInputUI);
            }
        }

        getHConnection().getStateObservable().addListener((oldState, newState) -> Platform.runLater(() -> {
            updateInputUI();
            if (newState == HState.NOT_CONNECTED) {
                lblState.textProperty().bind(ConnectionState.NOTCONNECTED.value);
                btnConnect.textProperty().bind(ConnectButton.CONNECT.value);
                outHost.setText("");
                outPort.setText("");
            }
            else if (oldState == HState.NOT_CONNECTED) {
                btnConnect.textProperty().bind(ConnectButton.ABORT.value);
            }

            if (newState == HState.CONNECTED) {
                lblState.textProperty().bind(ConnectionState.CONNECTED.value);
            }
            if (newState == HState.WAITING_FOR_CLIENT) {
                lblState.textProperty().bind(ConnectionState.WAITING.value);
            }

            if (newState == HState.CONNECTED && useFlash()) {
                outHost.setText(getHConnection().getDomain());
                outPort.setText(getHConnection().getServerPort()+"");

                JSONObject connectionSettings = new JSONObject();
                connectionSettings.put(AUTODETECT_CACHE, cbx_autodetect.isSelected());
                connectionSettings.put(HOST_CACHE, inpHost.getEditor().getText());
                connectionSettings.put(PORT_CACHE, Integer.parseInt(inpPort.getEditor().getText()));

                Cacher.put(CONNECTION_INFO_CACHE_KEY, connectionSettings);
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
                        if (!inpHost.getItems().contains(host)) inpHost.getItems().add(host);
                        inpHost.getSelectionModel().select(host);
                        if (!inpPort.getItems().contains(port)) inpPort.getItems().add(port);
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


                if (HConnection.DEBUG) System.out.println("connecting");
            }).start();


        }
        else {
            getHConnection().abort();
        }
    }

    @Override
    protected void onExit() {
        if (rd_flash.isSelected()) {
            Cacher.put(CLIENT_CACHE_KEY, HClient.FLASH);
        } else if (rd_unity.isSelected()) {
            Cacher.put(CLIENT_CACHE_KEY, HClient.UNITY);
        } else if (rd_nitro.isSelected()) {
            Cacher.put(CLIENT_CACHE_KEY, HClient.NITRO);
        }
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

    private enum ConnectButton {
        CONNECT ("tab.connection.button.connect"),
        ABORT ("tab.connection.button.abort");

        public final TranslatableString value;

        ConnectButton(String key) {
            this.value = new TranslatableString(key);
        }
    }

    private enum ConnectionState {
        CONNECTED ("tab.connection.state.connected"),
        NOTCONNECTED ("tab.connection.state.notconnected"),
        WAITING ("tab.connection.state.waiting");

        public final TranslatableString value;

        ConnectionState(String key) {
            this.value = new TranslatableString(key);
        }
    }

    private void initLanguageBinding() {
        TranslatableString port = new TranslatableString("tab.connection.port");
        TranslatableString host = new TranslatableString("tab.connection.host");
        lblInpPort.textProperty().bind(port);
        lblInpHost.textProperty().bind(host);
        lblPort.textProperty().bind(port);
        lblHost.textProperty().bind(host);
        cbx_autodetect.textProperty().bind(new TranslatableString("tab.connection.autodetect"));
        btnConnect.textProperty().bind(ConnectButton.CONNECT.value);
        lblHotelVersion.textProperty().bind(new TranslatableString("tab.connection.version"));
        lblClient.textProperty().bind(new TranslatableString("tab.connection.client"));
        rd_unity.textProperty().bind(new TranslatableString("tab.connection.client.unity"));
        rd_flash.textProperty().bind(new TranslatableString("tab.connection.client.flash"));
        rd_nitro.textProperty().bind(new TranslatableString("tab.connection.client.nitro"));
        lblStateHead.textProperty().bind(new TranslatableString("tab.connection.state"));
        lblState.textProperty().bind(ConnectionState.NOTCONNECTED.value);
    }
}
