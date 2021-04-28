package gearth.ui.tools;

import gearth.services.packet_info.PacketInfoManager;
import gearth.protocol.HMessage;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import gearth.protocol.HPacket;
import gearth.ui.SubForm;

import java.nio.ByteBuffer;


public class ToolsController extends SubForm {
    public TextField txt_intDecoded;
    public TextField txt_intEncoded;
    public TextField txt_ushortDecoded;
    public TextField txt_ushortEncoded;
    public Button btnEncodeInt;
    public Button btnDecodeInt;
    public Button btnEncodeUShort;
    public Button btnDecodeUshort;

    public Button btn_toExpr;
    public TextArea txt_packetArea;
    public Button btn_toPacket;
    public TextArea txt_exprArea;

    //TODO: toExpression() without bytelength limit for this use only

    public void initialize() {
        txt_packetArea.textProperty().addListener(observable -> {
            btn_toExpr.setDisable(new HPacket(txt_packetArea.getText()).isCorrupted());
        });

        txt_packetArea.setOnKeyPressed(event -> {
            if(event.getCode().equals(KeyCode.ENTER) && !btn_toExpr.isDisable()) {
                btn_toExpr_clicked(null);
            }
        });

        txt_exprArea.textProperty().addListener(observable -> {
            btn_toPacket.setDisable(new HPacket(txt_exprArea.getText()).isCorrupted());
        });

        txt_exprArea.setOnKeyPressed(event -> {
            if(event.getCode().equals(KeyCode.ENTER) && !btn_toPacket.isDisable()) {
                btn_toPacket_clicked(null);
            }
        });


        txt_intDecoded.textProperty().addListener(observable -> {
            boolean isInt = true;

            try {
                Integer.parseInt(txt_intDecoded.getText());
            } catch (NumberFormatException e) {
                isInt = false;
            }

            btnEncodeInt.setDisable(!isInt);
        });

        txt_intDecoded.setOnKeyPressed(event -> {
            if(event.getCode().equals(KeyCode.ENTER) && !btnEncodeInt.isDisable()) {
                btnEncodeInt_clicked(null);
            }
        });

        //---------------

        txt_ushortDecoded.textProperty().addListener(observable -> {
            boolean isDouble = true;

            try {
                int res = Integer.parseInt(txt_ushortDecoded.getText());
                if (res < 0 || res >= (256*256)) isDouble = false;
            } catch (NumberFormatException e) {
                isDouble = false;
            }
            btnEncodeUShort.setDisable(!isDouble);
        });

        txt_ushortDecoded.setOnKeyPressed(event -> {
            if(event.getCode().equals(KeyCode.ENTER) && !btnEncodeUShort.isDisable()) {
                btnEncodeUShort_clicked(null);
            }
        });

        //----------------

        txt_intEncoded.textProperty().addListener(observable -> {
            HPacket packet = new HPacket(txt_intEncoded.getText());
            btnDecodeInt.setDisable(packet.getBytesLength() != 4);
        });

        txt_intEncoded.setOnKeyPressed(event -> {
            if(event.getCode().equals(KeyCode.ENTER) && !btnDecodeInt.isDisable()) {
                btnDecodeInt_clicked(null);
            }
        });

        //----------------

        txt_ushortEncoded.textProperty().addListener(observable -> {
            HPacket packet = new HPacket(txt_ushortEncoded.getText());
            btnDecodeUshort.setDisable(packet.getBytesLength() != 2);
        });

        txt_ushortEncoded.setOnKeyPressed(event -> {
            if(event.getCode().equals(KeyCode.ENTER) && !btnDecodeUshort.isDisable()) {
                btnDecodeUshort_clicked(null);
            }
        });

    }

    public void btnEncodeInt_clicked(ActionEvent actionEvent) {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(Integer.parseInt(txt_intDecoded.getText()));

        HPacket packet = new HPacket(b.array());
        txt_intEncoded.setText(packet.toString());
    }
    public void btnDecodeInt_clicked(ActionEvent actionEvent) {
        txt_intDecoded.setText(new HPacket(txt_intEncoded.getText()).readInteger(0) + "");
    }
    public void btnEncodeUShort_clicked(ActionEvent actionEvent) {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(Integer.parseInt(txt_ushortDecoded.getText()));

        HPacket packet = new HPacket(new byte[]{b.array()[2], b.array()[3]});
        txt_ushortEncoded.setText(packet.toString());
    }
    public void btnDecodeUshort_clicked(ActionEvent actionEvent) {
        txt_ushortDecoded.setText(new HPacket(txt_ushortEncoded.getText()).readUshort(0) + "");
    }


    private HPacket parseToPacket(String p) {
        PacketInfoManager packetInfoManager = getHConnection().getPacketInfoManager();
        HPacket packet = new HPacket(p);
        if (!packet.isPacketComplete() && packetInfoManager != null) {
            packet.completePacket(packetInfoManager);
        }

        return packet;
    }

    public void btn_toExpr_clicked(ActionEvent actionEvent) {
        txt_exprArea.setText(parseToPacket(txt_packetArea.getText()).toExpression(getHConnection().getPacketInfoManager(), true));
    }

    public void btn_toPacket_clicked(ActionEvent actionEvent) {
        txt_packetArea.setText(parseToPacket(txt_exprArea.getText()).toString());
    }
}
