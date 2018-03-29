package main.ui;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import main.protocol.HConnection;
import main.ui.connection.ConnectionForm;
import main.ui.injection.InjectionForm;
import main.ui.logger.LoggerForm;
import main.ui.tools.ToolsForm;

import java.awt.*;

public class GEarthController {

    private Stage stage = null;
    private volatile HConnection hConnection;

    public ConnectionForm connectionController;
    public InjectionForm injectionController;
    public LoggerForm loggerController;
    public ToolsForm toolsController;
    public Pane mover;

    public GEarthController() {
        hConnection = new HConnection();
    }

    public void initialize() {
        connectionController.setParentController(this);
        injectionController.setParentController(this);
        loggerController.setParentController(this);
        toolsController.setParentController(this);
        //custom header bar
//        final Point[] startpos = {null};
//        final Double[] xx = {0.0};
//        final Double[] yy = {0.0};
//        final Boolean[] isMoving = {false};
//
//        mover.addEventHandler(MouseEvent.MOUSE_PRESSED,
//                event -> {
//                    startpos[0] = MouseInfo.getPointerInfo().getLocation();
//                    xx[0] = stage.getX();
//                    yy[0] = stage.getY();
//                    isMoving[0] = true;
//                });
//
//        mover.addEventHandler(MouseEvent.MOUSE_RELEASED,
//                event -> {
//                    isMoving[0] = false;
//                });
//
//
//        mover.setOnMouseDragged(event -> {
//            if (isMoving[0]) {
//                Point now = MouseInfo.getPointerInfo().getLocation();
//                double diffX = now.getX() - startpos[0].getX();
//                double diffY = now.getY() - startpos[0].getY();
//                stage.setX(xx[0] + diffX);
//                stage.setY(yy[0] + diffY);
//            }
//        });
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }
    public Stage getStage() {
        return stage;
    }

    HConnection getHConnection() {
        return hConnection;
    }
    void writeToLog(javafx.scene.paint.Color color, String text) {
        loggerController.miniLogText(color, text);
    }


    public void abort() {
        hConnection.abort();
    }

}
