package gearth.ui.logger.loggerdisplays;

import gearth.protocol.HPacket;
import gearth.ui.UiLoggerController;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class UiLogger implements PacketLogger {
    private Stage stage;
    private UiLoggerController controller;

    @Override
    public void start() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gearth/ui/UiLogger.fxml"));

        try {
            Parent root = loader.load();
            controller = loader.getController();
            stage = new Stage();
            stage.setTitle("G-Earth | Packet Logger");
            stage.initModality(Modality.NONE);

            Scene scene = new Scene(root);
            scene.getStylesheets().add("/gearth/ui/bootstrap3.css");
            scene.getStylesheets().add("/gearth/ui/logger.css");

            scene.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
                final KeyCombination keyCombIncoming = new KeyCodeCombination(KeyCode.I,
                        KeyCombination.CONTROL_DOWN);
                final KeyCombination keyCombOutgoing = new KeyCodeCombination(KeyCode.O,
                        KeyCombination.CONTROL_DOWN);

                public void handle(KeyEvent ke) {
                    if (keyCombIncoming.match(ke)) {
                        controller.toggleViewIncoming();
                        ke.consume();
                    } else if (keyCombOutgoing.match(ke)) {
                        controller.toggleViewOutgoing();
                        ke.consume();
                    }
                }
            });

            stage.setScene(scene);

//            ScenicView.show(scene);

            // don't let the user close this window on their own
            stage.setOnCloseRequest(Event::consume);

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        if (stage != null)
            stage.close();
    }

    @Override
    public void appendSplitLine() {
        // don't use this, we can't discern incoming/outgoing
        //Platform.runLater(() -> controller.appendSplitLine());
    }

    @Override
    public void appendMessage(HPacket packet, int types) {
        Platform.runLater(() -> controller.appendMessage(packet, types));
    }

    @Override
    public void appendStructure(HPacket packet) {

    }
}
