package gearth.ui.logger;

import gearth.extensions.parsers.HDirection;
import gearth.protocol.connection.HState;
import gearth.services.extensionhandler.extensions.extensionproducers.ExtensionProducer;
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
import java.util.function.Predicate;

public class LoggerController extends SubForm {


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

    private PacketLoggerFactory packetLoggerFactory;
    private PacketLogger packetLogger;

    public void onParentSet(){
        packetLoggerFactory = new PacketLoggerFactory(parentController.extensionsController.getExtensionHandler());
        packetLogger = packetLoggerFactory.get();

        getHConnection().getStateObservable().addListener((oldState, newState) -> Platform.runLater(() -> {
            if (newState == HState.PREPARING) {
                miniLogText(Color.ORANGE, "Connecting to "+getHConnection().getDomain() + ":" + getHConnection().getServerPort());
            }
            if (newState == HState.CONNECTED) {
                miniLogText(Color.GREEN, "Connected to "+getHConnection().getDomain() + ":" + getHConnection().getServerPort());
                packetLogger.start(getHConnection());
            }
            if (newState == HState.NOT_CONNECTED) {
                miniLogText(Color.RED, "End of connection");
                packetLogger.stop();
            }
        }));

        getHConnection().addTrafficListener(2, message -> { Platform.runLater(() -> {
            if (message.getDestination() == HMessage.Direction.TOCLIENT && cbx_blockIn.isSelected() ||
                    message.getDestination() == HMessage.Direction.TOSERVER && cbx_blockOut.isSelected()) return;

            if (cbx_splitPackets.isSelected()) {
                packetLogger.appendSplitLine();
            }

            int types = 0;
            if (message.getDestination() == HMessage.Direction.TOCLIENT) types |= PacketLogger.MESSAGE_TYPE.INCOMING.getValue();
            else if (message.getDestination() == HMessage.Direction.TOSERVER) types |= PacketLogger.MESSAGE_TYPE.OUTGOING.getValue();
            if (message.getPacket().length() >= packetLimit) types |= PacketLogger.MESSAGE_TYPE.SKIPPED.getValue();
            if (message.isBlocked()) types |= PacketLogger.MESSAGE_TYPE.BLOCKED.getValue();
            if (message.getPacket().isReplaced()) types |= PacketLogger.MESSAGE_TYPE.REPLACED.getValue();
            if (cbx_showAdditional.isSelected()) types |= PacketLogger.MESSAGE_TYPE.SHOW_ADDITIONAL_DATA.getValue();

            packetLogger.appendMessage(message.getPacket(), types);

            if (cbx_showstruct.isSelected() && message.getPacket().length() < packetLimit) {
                packetLogger.appendStructure(message.getPacket(), message.getDestination());
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
