package gearth.ui.injection;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import gearth.protocol.HConnection;
import gearth.protocol.HPacket;
import gearth.ui.SubForm;

public class Injection extends SubForm {
    public TextArea inputPacket;
    public Text lbl_corrruption;
    public Text lbl_pcktInfo;
    public Button btn_sendToServer;
    public Button btn_sendToClient;

    protected void onParentSet() {
        getHConnection().addStateChangeListener((oldState, newState) -> Platform.runLater(this::updateUI));

        inputPacket.textProperty().addListener(event -> Platform.runLater(this::updateUI));
    }

    private void updateUI() {
        HPacket packet = new HPacket(inputPacket.getText());
        if (!packet.isCorrupted()) {
            lbl_corrruption.setText("isCorrupted: False");
            lbl_corrruption.setFill(Paint.valueOf("Green"));
            lbl_pcktInfo.setText("header (id:"+packet.headerId()+", length:"+packet.length()+")");

            btn_sendToClient.setDisable(getHConnection().getState() != HConnection.State.CONNECTED);
            btn_sendToServer.setDisable(getHConnection().getState() != HConnection.State.CONNECTED);
        }
        else {
            lbl_corrruption.setText("isCorrupted: True");
            lbl_corrruption.setFill(Paint.valueOf("#ee0404b2"));
            lbl_pcktInfo.setText("header (id:NULL, length:"+packet.getBytesLength()+")");

            btn_sendToClient.setDisable(true);
            btn_sendToServer.setDisable(true);
        }

    }

    public void sendToServer_clicked(ActionEvent actionEvent) {
        HPacket packet = new HPacket(inputPacket.getText());
        getHConnection().sendToServerAsync(packet);
        writeToLog(Color.BLUE, "SS -> packet with id: " + packet.headerId());
    }

    public void sendToClient_clicked(ActionEvent actionEvent) {
        HPacket packet = new HPacket(inputPacket.getText());
        getHConnection().sendToClientAsync(packet);
        writeToLog(Color.RED, "CS -> packet with id: " + packet.headerId());
    }
}
