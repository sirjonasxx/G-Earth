package gearth.ui.logger.loggerdisplays.uilogger;

import gearth.protocol.HPacket;
import gearth.ui.logger.loggerdisplays.PacketLogger;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UiLogger implements PacketLogger {
    private Stage stage;
    private UiLoggerController controller = null;

    @Override
    public void start() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gearth/ui/logger/uilogger/UiLogger.fxml"));

        try {
            Parent root = loader.load();
            synchronized (appendLater) {
                controller = loader.getController();
                for (Elem elem : appendLater) {
                    controller.appendMessage(elem.packet, elem.types);
                }
                appendLater.clear();
            }

            stage = new Stage();
            stage.setTitle("G-Earth | Packet Logger");
            stage.initModality(Modality.NONE);

            Scene scene = new Scene(root);
            scene.getStylesheets().add("/gearth/ui/bootstrap3.css");
            scene.getStylesheets().add("/gearth/ui/logger/uilogger/logger.css");
            UiLoggerController controller = (UiLoggerController) loader.getController();
            controller.setStage(stage);

//            scene.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
//                final KeyCombination keyCombIncoming = new KeyCodeCombination(KeyCode.I,
//                        KeyCombination.CONTROL_DOWN);
//                final KeyCombination keyCombOutgoing = new KeyCodeCombination(KeyCode.O,
//                        KeyCombination.CONTROL_DOWN);
//
//                public void handle(KeyEvent ke) {
//                    if (keyCombIncoming.match(ke)) {
//                        controller.toggleViewIncoming();
//                        ke.consume();
//                    } else if (keyCombOutgoing.match(ke)) {
//                        controller.toggleViewOutgoing();
//                        ke.consume();
//                    }
//                }
//            });

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

    private class Elem {
        HPacket packet;
        int types;
        Elem(HPacket packet, int types) {
            this.packet = packet;
            this.types = types;
        }
    }

    private final List<Elem> appendLater = new ArrayList<>();

    @Override
    public void appendMessage(HPacket packet, int types) {
        synchronized (appendLater) {
            if (controller == null) {
                appendLater.add(new Elem(packet, types));
            }
            else  {
                controller.appendMessage(packet, types);
            }
        }
    }

    @Override
    public void appendStructure(HPacket packet) {

    }
}
