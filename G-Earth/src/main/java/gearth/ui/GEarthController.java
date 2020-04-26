package gearth.ui;

import gearth.ui.logger.loggerdisplays.PacketLoggerFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import gearth.protocol.HConnection;
import gearth.ui.connection.ConnectionController;
import gearth.ui.extensions.ExtensionsController;
import gearth.ui.info.InfoController;
import gearth.ui.injection.InjectionController;
import gearth.ui.logger.LoggerController;
import gearth.ui.scheduler.SchedulerController;
import gearth.ui.extra.ExtraController;
import gearth.ui.tools.ToolsController;

import java.util.ArrayList;
import java.util.List;

public class GEarthController {

    public Tab tab_Logger;
    public TabPane tabBar;
    private Stage stage = null;
    private volatile HConnection hConnection;
    private volatile int initcount = 0;

    public ConnectionController connectionController;
    public InjectionController injectionController;
    public LoggerController loggerController;
    public ToolsController toolsController;
    public SchedulerController schedulerController;
    public ExtraController extraController;
    public InfoController infoController;
    public ExtensionsController extensionsController;

    private List<SubForm> tabs = null;

    public Pane mover;
    public GEarthController() {
        hConnection = new HConnection();
    }

    public void initialize() {
        tabs = new ArrayList<>();
        tabs.add(connectionController);
        tabs.add(injectionController);
        tabs.add(loggerController);
        tabs.add(toolsController);
        tabs.add(schedulerController);
        tabs.add(extraController);
        tabs.add(infoController);
        tabs.add(extensionsController);

        synchronized (this) {
            trySetController();
        }

        if (PacketLoggerFactory.usesUIlogger()) {
            tabBar.getTabs().remove(tab_Logger);
        }

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
        synchronized (this) {
            trySetController();
        }
    }

    private void trySetController() {
        if (++initcount == 2) {
            GEarthController self = this;
            tabs.forEach(subForm -> subForm.setParentController(self));
        }
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


    public void exit() {
        tabs.forEach(SubForm::exit);
        hConnection.abort();
    }

}
