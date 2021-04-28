package gearth.ui;

import gearth.protocol.connection.proxy.ProxyProviderFactory;
import gearth.protocol.connection.proxy.SocksConfiguration;
import gearth.ui.logger.loggerdisplays.PacketLoggerFactory;
import javafx.event.Event;
import javafx.event.EventHandler;
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
        SocksConfiguration temporary_socks = new SocksConfiguration() {
            public boolean useSocks() { return false; }
            public int getSocksPort() { return 0; }
            public String getSocksHost() { return null; }
            public boolean onlyUseIfNeeded() { return true; }
        };

        ProxyProviderFactory.setSocksConfig(temporary_socks);

        hConnection = new HConnection();
    }

    public void initialize() {
        tabs = new ArrayList<>();
        // must be ordered correctly
        tabs.add(connectionController);
        tabs.add(injectionController);
        tabs.add(loggerController);
        tabs.add(toolsController);
        tabs.add(schedulerController);
        tabs.add(extensionsController);
        tabs.add(extraController);
        tabs.add(infoController);

        synchronized (this) {
            trySetController();
        }

        List<Tab> uiTabs = tabBar.getTabs();
        for (int i = 0; i < uiTabs.size(); i++) {
            Tab tab = uiTabs.get(i);
            int[] ii = {i};

            tab.setOnSelectionChanged(event -> {
                if (tab.isSelected()) {
                    tabs.get(ii[0]).onTabOpened();
                }
            });
        }

        if (PacketLoggerFactory.usesUIlogger()) {
            tabBar.getTabs().remove(tab_Logger);
        }
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

            extensionsController.setParentController(self);
            tabs.forEach(subForm -> {
                if (subForm != extensionsController) subForm.setParentController(self);
            });
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
