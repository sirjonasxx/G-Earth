package gearth.ui.subforms.tools;

import gearth.misc.BindingsUtil;
import gearth.protocol.HPacket;
import gearth.services.packet_info.PacketInfoManager;
import gearth.ui.SubForm;
import gearth.ui.translations.TranslatableString;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;

import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ResourceBundle;

public class ToolsController extends SubForm implements Initializable {

    public TextField decodedIntegerField;
    public TextField encodedIntegerField;
    public TextField decodedUShortField;
    public TextField encodedUShortField;
    public Button encodeIntegerButton;
    public Button decodeIntegerButton;
    public Button encodeUShortButton;
    public Button decodeUShortButton;

    public TextArea packetArea;
    public TextArea expressionArea;
    public Button convertPacketToExpressionButton;
    public Button convertExpressionToPacketButton;

    public Label
            integerLabel,
            uShortLabel,
            encodingOrDecodingLabel,
            packetToExpressionLabel;

    //TODO: toExpression() without bytelength limit for this use only

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        convertPacketToExpressionButton.disableProperty().bind(packetIsCorruptBinding(packetArea));
        convertExpressionToPacketButton.disableProperty().bind(packetIsCorruptBinding(expressionArea));

        encodeIntegerButton.disableProperty().bind(BindingsUtil.isInteger(decodedIntegerField.textProperty()).not());
        decodeIntegerButton.disableProperty().bind(packetLengthEqualsBinding(encodedIntegerField, Integer.BYTES).not());

        encodeUShortButton.disableProperty().bind(BindingsUtil.isUShort(decodedUShortField.textProperty()).not());
        decodeUShortButton.disableProperty().bind(packetLengthEqualsBinding(encodedUShortField, Short.BYTES).not());

        fireButtonOnPressEnter(packetArea, convertPacketToExpressionButton);
        fireButtonOnPressEnter(expressionArea, convertExpressionToPacketButton);
        fireButtonOnPressEnter(decodedIntegerField, encodeIntegerButton);
        fireButtonOnPressEnter(decodedUShortField, encodeUShortButton);
        fireButtonOnPressEnter(encodedIntegerField, decodeIntegerButton);
        fireButtonOnPressEnter(encodedUShortField, decodeUShortButton);

        initLanguageBinding();
    }

    private BooleanBinding packetIsCorruptBinding(TextInputControl input) {
        return Bindings.createBooleanBinding(() -> new HPacket(input.getText()).isCorrupted(), input.textProperty());
    }

    private BooleanBinding packetLengthEqualsBinding(TextInputControl input, int length) {
        return Bindings.createBooleanBinding(() -> new HPacket(input.getText()).getBytesLength() == length, input.textProperty());
    }

    private void fireButtonOnPressEnter(Node node, Button button) {
        node.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER) && !button.isDisable())
                button.fire();
        });
    }

    @FXML
    public void onClickEncodeIntegerButton() {
        final HPacket packet = new HPacket(ByteBuffer
                .allocate(4)
                .putInt(Integer.parseInt(decodedIntegerField.getText()))
                .array());
        encodedIntegerField.setText(packet.toString());
    }

    @FXML
    public void onClickDecodeIntegerButton() {
        final HPacket hPacket = new HPacket(encodedIntegerField.getText());
        decodedIntegerField.setText(Integer.toString(hPacket.readInteger(0)));
    }

    @FXML
    public void onClickEncodeUShortButton() {
        final byte[] dst = new byte[2];
        ByteBuffer
                .allocate(4)
                .putInt(Integer.parseInt(decodedUShortField.getText()))
                .get(dst, 2, 2);
        final HPacket packet = new HPacket(dst);
        encodedUShortField.setText(packet.toString());
    }

    @FXML
    public void onClickDecodeUShortButton() {
        final HPacket packet = new HPacket(encodedUShortField.getText());
        decodedUShortField.setText(Integer.toString(packet.readUshort(0)));
    }

    @FXML
    public void onClickPacketToExpressionButton() {
        final HPacket packet = parseToPacket(packetArea.getText());
        expressionArea.setText(packet.toExpression(getHConnection().getPacketInfoManager(), true));
    }

    @FXML
    public void onClickExpressionToPacketButton() {
        final HPacket packet = parseToPacket(expressionArea.getText());
        packetArea.setText(packet.toString());
    }

    private HPacket parseToPacket(String p) {
        final PacketInfoManager packetInfoManager = getHConnection().getPacketInfoManager();
        final HPacket packet = new HPacket(p);
        if (!packet.isPacketComplete() && packetInfoManager != null)
            packet.completePacket(packetInfoManager);
        return packet;
    }

    private void initLanguageBinding() {
        integerLabel.textProperty().bind(new TranslatableString("%s:", "tab.tools.type.integer"));
        uShortLabel.textProperty().bind(new TranslatableString("%s:", "tab.tools.type.ushort"));

        final TranslatableString encode = new TranslatableString("%s", "tab.tools.button.encode");
        final TranslatableString decode = new TranslatableString("%s", "tab.tools.button.decode");
        encodeIntegerButton.textProperty().bind(encode);
        encodeUShortButton.textProperty().bind(encode);
        decodeIntegerButton.textProperty().bind(decode);
        decodeUShortButton.textProperty().bind(decode);

        encodingOrDecodingLabel.textProperty().bind(new TranslatableString("%s/%s", "tab.tools.encoding", "tab.tools.decoding"));
        packetToExpressionLabel.textProperty().bind(new TranslatableString("%s <-> %s", "tab.tools.packet", "tab.tools.expression"));
    }
}
