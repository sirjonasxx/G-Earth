package gearth.ui.subforms.logger;

import gearth.protocol.connection.HState;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import gearth.protocol.HMessage;
import gearth.ui.SubForm;
import gearth.ui.subforms.logger.loggerdisplays.PacketLogger;
import gearth.ui.subforms.logger.loggerdisplays.PacketLoggerFactory;

import java.util.Calendar;

public class LoggerController extends SubForm {


    public TextField packetLimitField;
    public CheckBox hideIncomingPacketsBox;
    public CheckBox hideOutgoingPacketsBox;
    public CheckBox showPacketStructureBox;
    public CheckBox showAdditionalPacketInfoBox;
    public CheckBox splitPacketsBox;
    public CheckBox enableLoggingBox;
    public TextFlow loggingField;
    public Button updateButton;

    private int packetLimit = 8000;

    private PacketLogger packetLogger;

    public void onParentSet() {
        packetLogger = new PacketLoggerFactory(parentController.extensionsController.getExtensionHandler()).get();

        getHConnection().getStateObservable().addListener((oldState, newState) -> Platform.runLater(() -> {
            if (newState == HState.PREPARING) {
                miniLogText(Color.ORANGE, "Connecting to " + getHConnection().getDomain() + ":" + getHConnection().getServerPort());
            }
            if (newState == HState.CONNECTED) {
                miniLogText(Color.GREEN, "Connected to " + getHConnection().getDomain() + ":" + getHConnection().getServerPort());
                packetLogger.start(getHConnection());
            }
            if (newState == HState.NOT_CONNECTED) {
                miniLogText(Color.RED, "End of connection");
                packetLogger.stop();
            }
        }));

        getHConnection().addTrafficListener(2, message -> {
            Platform.runLater(() -> {
                if (message.getDestination() == HMessage.Direction.TOCLIENT && hideIncomingPacketsBox.isSelected() ||
                        message.getDestination() == HMessage.Direction.TOSERVER && hideOutgoingPacketsBox.isSelected())
                    return;

                if (splitPacketsBox.isSelected()) {
                    packetLogger.appendSplitLine();
                }

                int types = 0;
                if (message.getDestination() == HMessage.Direction.TOCLIENT)
                    types |= PacketLogger.MESSAGE_TYPE.INCOMING.getValue();
                else if (message.getDestination() == HMessage.Direction.TOSERVER)
                    types |= PacketLogger.MESSAGE_TYPE.OUTGOING.getValue();
                if (message.getPacket().length() >= packetLimit) types |= PacketLogger.MESSAGE_TYPE.SKIPPED.getValue();
                if (message.isBlocked()) types |= PacketLogger.MESSAGE_TYPE.BLOCKED.getValue();
                if (message.getPacket().isReplaced()) types |= PacketLogger.MESSAGE_TYPE.REPLACED.getValue();
                if (showAdditionalPacketInfoBox.isSelected())
                    types |= PacketLogger.MESSAGE_TYPE.SHOW_ADDITIONAL_DATA.getValue();

                packetLogger.appendMessage(message.getPacket(), types);

                if (showPacketStructureBox.isSelected() && message.getPacket().length() < packetLimit) {
                    packetLogger.appendStructure(message.getPacket(), message.getDestination());
                }
            });
        });
    }

    public void updatePacketLimit(ActionEvent actionEvent) {
        packetLimit = Integer.parseInt(packetLimitField.getText());
    }

    @SuppressWarnings("Duplicates")
    public void initialize() {
        packetLimitField.textProperty().addListener(observable -> {
            boolean isInt = true;

            try {
                Integer.parseInt(packetLimitField.getText());
            } catch (NumberFormatException e) {
                isInt = false;
            }

            updateButton.setDisable(!isInt);
        });

        packetLimitField.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER) && !updateButton.isDisable()) {
                updatePacketLimit(null);
            }
        });
    }

    public void miniLogText(Color color, String text) {
        if (enableLoggingBox.isSelected()) {
            final String color2 = "#" + color.toString().substring(2, 8);

            final Calendar rightNow = Calendar.getInstance();
            final String hour = addToNumber("" + rightNow.get(Calendar.HOUR_OF_DAY));
            final String minutes = addToNumber("" + rightNow.get(Calendar.MINUTE));
            final String seconds = addToNumber("" + rightNow.get(Calendar.SECOND));
            String timestamp = "[" + hour + ":" + minutes + ":" + seconds + "] ";

            timestamp = timestamp.replace(" ", "\u00A0"); // disable automatic linebreaks
            final Text time = new Text(timestamp);
            time.setStyle("-fx-opacity: " + 0.5 + ";");

            text = text.replace(" ", "\u00A0");
            final Text otherText = new Text(text + "\n");
            otherText.setStyle("-fx-fill: " + color2 + ";");

            loggingField.getChildren().addAll(time, otherText);
        }
    }

    private String addToNumber(String text) {
        if (text.length() == 1) text = "0" + text;
        return text;
    }
}
