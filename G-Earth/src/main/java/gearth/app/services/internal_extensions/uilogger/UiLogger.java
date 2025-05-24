package gearth.app.services.internal_extensions.uilogger;

import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.app.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.app.ui.subforms.logger.loggerdisplays.PacketLogger;

@ExtensionInfo(
        Title = "Packet Logger",
        Description = "",
        Version = "1.0",
        Author = "sirjonasxx & Scott"
)
public class UiLogger extends ExtensionForm implements PacketLogger {
    private UiLoggerController controller = null;

    @Override
    public void start(HConnection hConnection) {
//            // don't let the user close this window on their own
//            stage.setOnCloseRequest(Event::consume);

//        primaryStage.show();
    }

    @Override
    public void stop() {
//        primaryStage.hide();
//        if (stage != null)
//            stage.close();
    }

    @Override
    public void appendSplitLine() {
        // don't use this, we can't discern incoming/outgoing
        //Platform.runLater(() -> controller.appendSplitLine());
    }

    @Override
    public void initExtension() {
        controller.init(this);
        onConnect((host, port, hotelversion, clientIdentifier, clientType) -> {
            controller.onConnect();
        });
    }

    @Override
    public void onEndConnection() {
        controller.onDisconnect();
    }

    private class Elem {
        HPacket packet;
        int types;
        Elem(HPacket packet, int types) {
            this.packet = packet;
            this.types = types;
        }
    }

    @Override
    public void appendMessage(HPacket packet, int types) {
        controller.appendMessage(packet, types);
    }

    @Override
    public void appendStructure(HPacket packet, HMessage.Direction direction) {

    }

    @Override
    public boolean canLeave() {
        return false;
    }

    @Override
    public boolean canDelete() {
        return false;
    }

    public void setController(UiLoggerController controller) {
        this.controller = controller;
    }
}
