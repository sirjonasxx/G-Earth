package gearth.ui.connection;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import gearth.protocol.HConnection;
import gearth.ui.SubForm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Connection extends SubForm {

    public ComboBox<String> inpPort;
    public ComboBox<String> inpHost;
    public Button btnConnect;
    public Label lblState;
    public TextField outHost;
    public TextField outPort;
    public CheckBox cbx_autodetect;
    public TextField txtfield_hotelversion;

    public void initialize() {
        inpPort.getEditor().textProperty().addListener(observable -> {
            updateInputUI();
        });
        cbx_autodetect.selectedProperty().addListener(observable -> {
            updateInputUI();
        });

        List<String> knownHosts = HConnection.autoDetectHosts;
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

        inpPort.getItems().addAll(portsSorted);
        inpHost.getItems().addAll(hostsSorted);

        inpPort.getSelectionModel().selectFirst();
        inpHost.getSelectionModel().selectFirst();
    }

    private void updateInputUI() {
        if (cbx_autodetect.isSelected()) {
            btnConnect.setDisable(false);
        }
        else {
            try {
                int i = Integer.parseInt(inpPort.getEditor().getText());
                btnConnect.setDisable(i < 0 || i >= 256 * 256);
            }
            catch (Exception e) {
                btnConnect.setDisable(true);
            }
        }

        inpHost.setDisable(getHConnection().getState() != HConnection.State.NOT_CONNECTED || cbx_autodetect.isSelected());
        inpPort.setDisable(getHConnection().getState() != HConnection.State.NOT_CONNECTED || cbx_autodetect.isSelected());
    }

    public void onParentSet(){
        getHConnection().addStateChangeListener((oldState, newState) -> Platform.runLater(() -> {
            txtfield_hotelversion.setText(getHConnection().getHotelVersion());
            Platform.runLater(() -> {
                if (newState == HConnection.State.NOT_CONNECTED) {
                    updateInputUI();
                    lblState.setText("Not connected");
                    btnConnect.setText("Connect");
                    outHost.setText("");
                    outPort.setText("");
                }
                else if (oldState == HConnection.State.NOT_CONNECTED) {
                    updateInputUI();
                    btnConnect.setText("Abort");
                }

                if (newState == HConnection.State.CONNECTED) {
                    lblState.setText("Connected");
                    outHost.setText(getHConnection().getDomain());
                    outPort.setText(getHConnection().getPort()+"");
                }
                if (newState == HConnection.State.WAITING_FOR_CLIENT) {
                    lblState.setText("Waiting for connection");
                }
            });


        }));
    }

    public void btnConnect_clicked(ActionEvent actionEvent) {
        if (getHConnection().getState() == HConnection.State.NOT_CONNECTED) {

            btnConnect.setDisable(true);
            new Thread(() -> {
                if (cbx_autodetect.isSelected()) {
                    getHConnection().prepare();
                }
                else {
                    getHConnection().prepare(inpHost.getEditor().getText(), Integer.parseInt(inpPort.getEditor().getText()));
                }
                Platform.runLater(() -> btnConnect.setDisable(false));

                if (HConnection.DEBUG) System.out.println("connecting");

                try {
                    getHConnection().start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        }
        else {
            getHConnection().abort();
        }
    }
    
}
