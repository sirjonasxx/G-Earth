package gearth.ui.logger;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.ui.SubForm;
import gearth.ui.logger.loggerdisplays.PacketLogger;
import gearth.ui.logger.loggerdisplays.PacketLoggerFactory;

import java.util.Calendar;

public class Logger extends SubForm {


    public TextField txtPacketLimit;
    public CheckBox cbx_blockIn;
    public CheckBox cbx_blockOut;
    public CheckBox cbx_showAdditional;
    public CheckBox cbx_splitPackets;
    public CheckBox cbx_useLog;
    public TextFlow txt_logField;
    public Button btnUpdate;
    public CheckBox cbx_showstruct;

    private int packetLimit = 8000;

    private PacketLogger packetLogger = PacketLoggerFactory.get();

    public void onParentSet(){
        getHConnection().addStateChangeListener((oldState, newState) -> Platform.runLater(() -> {
            if (newState == HConnection.State.PREPARING) {
                miniLogText(Color.ORANGE, "Connecting to "+getHConnection().getDomain() + ":" + getHConnection().getPort());
            }
            if (newState == HConnection.State.CONNECTED) {
                miniLogText(Color.GREEN, "Connected to "+getHConnection().getDomain() + ":" + getHConnection().getPort());
                packetLogger.start();
            }
            if (newState == HConnection.State.NOT_CONNECTED) {
                miniLogText(Color.RED, "End of connection");
                packetLogger.stop();
            }
        }));

        getHConnection().addTrafficListener(2, message -> { Platform.runLater(() -> {
            if (message.getDestination() == HMessage.Side.TOCLIENT && cbx_blockIn.isSelected() ||
                    message.getDestination() == HMessage.Side.TOSERVER && cbx_blockOut.isSelected()) return;

            if (cbx_splitPackets.isSelected()) {
                packetLogger.appendSplitLine();
            }

            int types = 0;
            if (message.getDestination() == HMessage.Side.TOCLIENT) types |= PacketLogger.MESSAGE_TYPE.INCOMING.getValue();
            else if (message.getDestination() == HMessage.Side.TOSERVER) types |= PacketLogger.MESSAGE_TYPE.OUTGOING.getValue();
            if (message.getPacket().length() >= packetLimit) types |= PacketLogger.MESSAGE_TYPE.SKIPPED.getValue();
            if (message.isBlocked()) types |= PacketLogger.MESSAGE_TYPE.BLOCKED.getValue();
            if (message.getPacket().isReplaced()) types |= PacketLogger.MESSAGE_TYPE.REPLACED.getValue();
            if (cbx_showAdditional.isSelected()) types |= PacketLogger.MESSAGE_TYPE.SHOW_ADDITIONAL_DATA.getValue();

            packetLogger.appendMessage(message.getPacket(), types);

            if (cbx_showstruct.isSelected() && message.getPacket().length() < packetLimit) {
                packetLogger.appendStructure(message.getPacket());
            }
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
