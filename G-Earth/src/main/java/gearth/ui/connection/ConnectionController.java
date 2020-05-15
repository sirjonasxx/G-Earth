package gearth.ui.connection;

import gearth.misc.Cacher;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.proxy.ProxyProviderFactory;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import gearth.protocol.HConnection;
import gearth.ui.SubForm;
import org.json.JSONObject;

import java.io.IOException;
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
    public Label lblState;
    public TextField outHost;
    public TextField outPort;
    public CheckBox cbx_autodetect;
    public TextField txtfield_hotelversion;

    private final Object lock = new Object();
    private volatile int fullyInitialized = 0;

    public void initialize() {
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
    }

    private void updateInputUI() {
        txtfield_hotelversion.setText(getHConnection().getHotelVersion());

        btnConnect.setDisable(getHConnection().getState() == HState.PREPARING || getHConnection().getState() == HState.ABORTING);
        if (!cbx_autodetect.isSelected() && !btnConnect.isDisable()) {
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
                lblState.setText("Not connected");
                btnConnect.setText("Connect");
                outHost.setText("");
                outPort.setText("");
            }
            else if (oldState == HState.NOT_CONNECTED) {
                btnConnect.setText("Abort");
            }

            if (newState == HState.CONNECTED) {
                lblState.setText("Connected");
                outHost.setText(getHConnection().getDomain());
                outPort.setText(getHConnection().getServerPort()+"");
            }
            if (newState == HState.WAITING_FOR_CLIENT) {
                lblState.setText("Waiting for connection");
            }

            if (newState == HState.CONNECTED) {
                JSONObject connectionSettings = new JSONObject();
                connectionSettings.put(AUTODETECT_CACHE, cbx_autodetect.isSelected());
                connectionSettings.put(HOST_CACHE, inpHost.getEditor().getText());
                connectionSettings.put(PORT_CACHE, Integer.parseInt(inpPort.getEditor().getText()));

                Cacher.put(CONNECTION_INFO_CACHE_KEY, connectionSettings);
            }

        }));
    }

    public void btnConnect_clicked(ActionEvent actionEvent) {
        if (getHConnection().getState() == HState.NOT_CONNECTED) {

            btnConnect.setDisable(true);
            new Thread(() -> {
                if (cbx_autodetect.isSelected()) {
                    getHConnection().start();
                }
                else {
                    getHConnection().start(inpHost.getEditor().getText(), Integer.parseInt(inpPort.getEditor().getText()));
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
        getHConnection().abort();
    }
    
}
