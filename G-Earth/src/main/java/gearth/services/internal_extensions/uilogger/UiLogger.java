package gearth.services.internal_extensions.uilogger;

import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.services.packet_info.PacketInfoManager;
import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.ui.logger.loggerdisplays.PacketLogger;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
    protected void initExtension() {
        onConnect((host, port, hotelversion, clientIdentifier, clientType, packetInfoManager) -> {
            controller.setPacketInfoManager(packetInfoManager);
            controller.onConnect();
        });
    }

    @Override
    protected void onEndConnection() {
        controller.onDisconnect();
        controller.setPacketInfoManager(PacketInfoManager.EMPTY);
    }

    @Override
    public ExtensionForm launchForm(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(UiLogger.class.getResource("UiLogger.fxml"));

        Parent root = loader.load();
        stage.setTitle("G-Earth | Packet Logger");
        stage.initModality(Modality.NONE);
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/gearth/G-EarthLogoSmaller.png")));

        Scene scene = new Scene(root);
        scene.getStylesheets().add("/gearth/ui/bootstrap3.css");
        scene.getStylesheets().add("/gearth/services/internal_extensions/uilogger/logger.css");
        controller = loader.getController();
        controller.setStage(stage);

        stage.setScene(scene);
        return this;
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
    protected boolean canLeave() {
        return false;
    }

    @Override
    protected boolean canDelete() {
        return false;
    }
}
