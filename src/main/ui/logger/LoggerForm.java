package main.ui.logger;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import main.protocol.HConnection;
import main.protocol.HMessage;
import main.ui.SubForm;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class LoggerForm extends SubForm {


    public TextField txtPacketLimit;
    public CheckBox cbx_blockIn;
    public CheckBox cbx_blockOut;
    public CheckBox cbx_showAdditional;
    public CheckBox cbx_splitPackets;
    public CheckBox cbx_useLog;
    public TextFlow txt_logField;
    public Button btnUpdate;

    private int packetLimit = 8000;

    public final static Map<String, String> colorizePackets;

    static {
        //FOR GNOME ONLY, shows up colorized packets
        colorizePackets = new HashMap<>();
        colorizePackets.put("BLOCKED", (char)27 + "[35m");     // some kind of grey
        colorizePackets.put("INCOMING", (char)27 + "[31m");    // red
        colorizePackets.put("OUTGOING", (char)27 + "[34m");    // blue
        colorizePackets.put("REPLACED", (char)27 + "[33m");    // yellow

        // others:
        colorizePackets.put("INJECTED", "");
        colorizePackets.put("SKIPPED", (char)27 + "[36m");
        colorizePackets.put("DEFAULT", (char)27 + "[0m");

        if (System.getenv("XDG_CURRENT_DESKTOP") == null || !System.getenv("XDG_CURRENT_DESKTOP").toLowerCase().contains("gnome")) {
            for (String key : colorizePackets.keySet()) {
                colorizePackets.put(key, "");
            }
        }
    }


    public void onParentSet(){

        getHConnection().addStateChangeListener((oldState, newState) -> Platform.runLater(() -> {
            if (newState == HConnection.State.PREPARING) {
                miniLogText(Color.ORANGE, "Connecting to "+getHConnection().getDomain() + ":" + getHConnection().getPort());
            }
            if (newState == HConnection.State.CONNECTED) {
                miniLogText(Color.GREEN, "Connecting to "+getHConnection().getDomain() + ":" + getHConnection().getPort());
            }
            if (newState == HConnection.State.NOT_CONNECTED) {
                miniLogText(Color.RED, "End of connection");
            }
        }));

        getHConnection().addTrafficListener(2, message -> { Platform.runLater(() -> {
            if (message.getDestination() == HMessage.Side.TOCLIENT && cbx_blockIn.isSelected() ||
                    message.getDestination() == HMessage.Side.TOSERVER && cbx_blockOut.isSelected()) return;

            String splitter = cbx_splitPackets.isSelected() ? "-----------------------------------\n" : "";

            String type = message.getDestination() == HMessage.Side.TOCLIENT ?"" +
                            colorizePackets.get("INCOMING") + "INCOMING" :
                            colorizePackets.get("OUTGOING") + "OUTGOING";

            String additionalData = " ";
            if (!message.isCorrupted() && cbx_showAdditional.isSelected()) {
                additionalData = " (h:"+ message.getPacket().headerId() +", l:"+message.getPacket().length()+") ";
            }

            String arrow = "--> ";

            String packet = message.isCorrupted() ?
                    message.getPacket().getBytesLength() + " (encrypted)": // message.getPacket().toString() : // TEMP CODE TO VIEW ENCRYPTED BODY
                    message.getPacket().length() < packetLimit ?
                            message.getPacket().toString() :
                            colorizePackets.get("SKIPPED") + "<packet skipped (length >= " + (packetLimit) + ")>";


            String skipStyle = colorizePackets.get("DEFAULT");

            String isBlocked = message.isBlocked() ? colorizePackets.get("BLOCKED") + "[BLOCKED] " : "";
            String isEdited = !message.getPacket().isReplaced() || message.isBlocked() ? "" : colorizePackets.get("REPLACED") + "[REPLACED] ";
            System.out.println(splitter + isBlocked + isEdited + type + additionalData + arrow + packet + skipStyle);

        });
        });
    }

    public void updatePacketLimit(ActionEvent actionEvent) {
        packetLimit = Integer.parseInt(txtPacketLimit.getText());
    }

    @SuppressWarnings("Duplicates")
    public void initialize() {
        txtPacketLimit.textProperty().addListener(observable -> {
            boolean isInt = true;

            try {
                Integer.parseInt(txtPacketLimit.getText());
            } catch (NumberFormatException e) {
                isInt = false;
            }

            btnUpdate.setDisable(!isInt);
        });

        txtPacketLimit.setOnKeyPressed(event -> {
            if(event.getCode().equals(KeyCode.ENTER) && !btnUpdate.isDisable()) {
                updatePacketLimit(null);
            }
        });
    }

    public void miniLogText(Color color, String text) {
        if (cbx_useLog.isSelected()) {
            String color2 = "#" + color.toString().substring(2, 8);

            Calendar rightNow = Calendar.getInstance();
            String hour = addToNumber(""+rightNow.get(Calendar.HOUR_OF_DAY));
            String minutes = addToNumber(""+rightNow.get(Calendar.MINUTE));
            String seconds = addToNumber(""+rightNow.get(Calendar.SECOND));
            String timestamp = "["+hour+":"+minutes+":"+seconds+"] ";

            timestamp = timestamp.replace(" ", "\u00A0"); // disable automatic linebreaks
            Text time = new Text(timestamp);
            time.setStyle("-fx-opacity: "+0.5+";");

            text = text.replace(" ", "\u00A0");
            Text otherText = new Text(text + "\n");
            otherText.setStyle("-fx-fill: "+color2+";");

            txt_logField.getChildren().addAll(time, otherText);
        }
    }

    private String addToNumber(String text)	{
        if (text.length() == 1) text = "0" + text;
        return text;
    }

}
