package gearth.ui.injection;

import gearth.protocol.connection.HState;
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

import java.util.LinkedList;

public class InjectionController extends SubForm {
    public TextArea inputPacket;
    public Text lbl_corrruption;
    public Text lbl_pcktInfo;
    public Button btn_sendToServer;
    public Button btn_sendToClient;

    protected void onParentSet() {
        getHConnection().getStateObservable().addListener((oldState, newState) -> Platform.runLater(this::updateUI));

        inputPacket.textProperty().addListener(event -> Platform.runLater(this::updateUI));
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

    private static HPacket[] parsePackets(String fullText) {
        LinkedList<HPacket> packets = new LinkedList<>();
        String[] lines = fullText.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            while (isPacketIncomplete(line) && i < lines.length - 1)
                line += '\n' + lines[++i];

            packets.add(new HPacket(line));
        }
        return packets.toArray(new HPacket[0]);
    }

    private void updateUI() {
        boolean dirty = false;

        lbl_corrruption.setText("isCorrupted: False");
        lbl_corrruption.setFill(Paint.valueOf("Green"));

        HPacket[] packets = parsePackets(inputPacket.getText());

        if (packets.length == 0) {
            dirty = true;
            lbl_corrruption.setFill(Paint.valueOf("#ee0404b2"));
            lbl_corrruption.setText("isCorrupted: True");
        }

        for (int i = 0; i < packets.length; i++) {
            if (packets[i].isCorrupted()) {
                if (!dirty) {
                    lbl_corrruption.setText("isCorrupted: True -> " + i);
                    lbl_corrruption.setFill(Paint.valueOf("#ee0404b2"));
                    dirty = true;
                } else
                    lbl_corrruption.setText(lbl_corrruption.getText() + ", " + i);
            }
        }

        if (dirty && packets.length == 1) {
            lbl_corrruption.setText("isCorrupted: True"); // no index needed
        }

        if (!dirty) {
            btn_sendToClient.setDisable(getHConnection().getState() != HState.CONNECTED);
            btn_sendToServer.setDisable(getHConnection().getState() != HState.CONNECTED);
            if (packets.length == 1) {
                lbl_pcktInfo.setText("header (id:" + packets[0].headerId() + ", length:" +
                        packets[0].length() + ")");
            }
            else {
                lbl_pcktInfo.setText("");
            }
        } else {
            if (packets.length == 1) {
                lbl_pcktInfo.setText("header (id:NULL, length:" + packets[0].getBytesLength()+")");
            }
            else {
                lbl_pcktInfo.setText("");
            }

            btn_sendToClient.setDisable(true);
            btn_sendToServer.setDisable(true);
        }

    }

    public void sendToServer_clicked(ActionEvent actionEvent) {
        HPacket[] packets = parsePackets(inputPacket.getText());
        for (HPacket packet : packets) {
            getHConnection().sendToServerAsync(packet);
            writeToLog(Color.BLUE, "SS -> packet with id: " + packet.headerId());
        }
    }

    public void sendToClient_clicked(ActionEvent actionEvent) {
        HPacket[] packets = parsePackets(inputPacket.getText());
        for (HPacket packet : packets) {
            getHConnection().sendToClientAsync(packet);
            writeToLog(Color.RED, "CS -> packet with id: " + packet.headerId());
        }
    }


    public static void main(String[] args) {
        HPacket[] packets = parsePackets("{l}{h:3}{i:967585}{i:9589}{s:\"furni_inscriptionfuckfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionfurni_inscriptionsssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss\"}{s:\"sirjonasxx-II\"}{s:\"\"}{i:188}{i:0}{i:0}{b:false}");
        System.out.println(new HPacket("{l}{h:2550}{s:\"ClientPerf\"\"ormance\\\"}\"}{s:\"23\"}{s:\"fps\"}{s:\"Avatars: 1, Objects: 0\"}{i:76970180}").toExpression());

        System.out.println("hi");
    }
}
