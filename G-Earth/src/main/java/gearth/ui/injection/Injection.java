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
        boolean dirty = false;
        String[] rawPackets = inputPacket.getText().split("\n");
        HPacket[] packets = new HPacket[rawPackets.length];

        lbl_corrruption.setText("isCorrupted: False");
        lbl_corrruption.setFill(Paint.valueOf("Green"));

        for (int i = 0; i < packets.length; i++) {
            packets[i] = new HPacket(rawPackets[i]);
            if (packets[i].isCorrupted()) {
                if (!dirty) {
                    lbl_corrruption.setText("isCorrupted: True -> " + i);
                    lbl_corrruption.setFill(Paint.valueOf("#ee0404b2"));
                    dirty = true;
                } else
                    lbl_corrruption.setText(lbl_corrruption.getText() + ", " + i);
            }
        }

        if (!dirty) {
            btn_sendToClient.setDisable(getHConnection().getState() != HConnection.State.CONNECTED);
            btn_sendToServer.setDisable(getHConnection().getState() != HConnection.State.CONNECTED);
            lbl_pcktInfo.setText("header (id:" + packets[packets.length - 1].headerId() + ", length:" +
                    packets[packets.length - 1].length() + ")");
        } else {
           lbl_pcktInfo.setText("header (id:NULL, length:" + packets[packets.length - 1].getBytesLength()+")");

            btn_sendToClient.setDisable(true);
            btn_sendToServer.setDisable(true);
        }

    }

    public void sendToServer_clicked(ActionEvent actionEvent) {
        String[] rawPackets = inputPacket.getText().split("\n");
        for (String rawPacket : rawPackets) {
            HPacket packet = new HPacket(rawPacket);
            getHConnection().sendToServerAsync(packet);
            writeToLog(Color.BLUE, "SS -> packet with id: " + packet.headerId());
        }
    }

    public void sendToClient_clicked(ActionEvent actionEvent) {
        String[] rawPackets = inputPacket.getText().split("\n");
        for (String rawPacket : rawPackets) {
            HPacket packet = new HPacket(rawPacket);
            getHConnection().sendToClientAsync(packet);
            writeToLog(Color.RED, "CS -> packet with id: " + packet.headerId());
        }
    }
}
