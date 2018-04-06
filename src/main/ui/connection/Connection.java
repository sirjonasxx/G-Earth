package main.ui.connection;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.*;
import main.protocol.HConnection;
import main.protocol.HMessage;
import main.protocol.TrafficListener;
import main.ui.SubForm;

import java.io.IOException;

public class Connection extends SubForm {

    public ComboBox<String> inpPort;
    public ComboBox<String> inpHost;
    public Button btnConnect;
    public Label lblState;
    public TextField outHost;
    public TextField outPort;
    public CheckBox cbx_autodetect;

    private boolean isBusy = false;

    public void initialize() {
        inpPort.getEditor().textProperty().addListener(observable -> {
            updateInputUI();
        });
        cbx_autodetect.selectedProperty().addListener(observable -> {
            updateInputUI();
        });

        inpPort.getItems().addAll("30000", "38101");
        inpHost.getItems().addAll("game-nl.habbo.com", "game-us.habbo.com");

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

        }));
    }

    public void btnConnect_clicked(ActionEvent actionEvent) {
        if (!isBusy) {
            isBusy = true;
            if (cbx_autodetect.isSelected()) {
                getHConnection().prepare();
            }
            else {
                getHConnection().prepare(inpHost.getEditor().getText(), Integer.parseInt(inpPort.getEditor().getText()));
            }

            if (HConnection.DEBUG) System.out.println("connecting");

            new Thread(() -> {
                try {
                    getHConnection().start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        else {
            getHConnection().abort();
            isBusy = false;
        }
    }
    
}
