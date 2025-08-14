package gearth.app.ui;

import gearth.app.protocol.connection.proxy.ProxyProviderFactory;
import gearth.app.protocol.connection.proxy.SocksConfiguration;
import gearth.app.ui.subforms.logger.loggerdisplays.PacketLoggerFactory;
import gearth.app.ui.translations.TranslatableString;
import javafx.scene.control.*;
import javafx.stage.Stage;
import gearth.app.protocol.HConnection;
import gearth.app.ui.subforms.connection.ConnectionController;
import gearth.app.ui.subforms.extensions.ExtensionsController;
import gearth.app.ui.subforms.info.InfoController;
import gearth.app.ui.subforms.injection.InjectionController;
import gearth.app.ui.subforms.logger.LoggerController;
import gearth.app.ui.subforms.scheduler.SchedulerController;
import gearth.app.ui.subforms.extra.ExtraController;
import gearth.app.ui.subforms.tools.ToolsController;

import java.util.ArrayList;
import java.util.List;

public class GEarthController {

    public Tab tab_Connection, tab_Logger, tab_Injection, tab_Tools, tab_Scheduler, tab_Extensions, tab_Extra, tab_Info;
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

        if (PacketLoggerFactory.usesUIlogger()) {
            tabBar.getTabs().remove(tab_Logger);
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

        initLanguageBinding();
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

    private void initLanguageBinding() {
        tab_Connection.textProperty().bind(new TranslatableString("%s", "tab.connection"));
        tab_Injection.textProperty().bind(new TranslatableString("%s", "tab.injection"));
        tab_Tools.textProperty().bind(new TranslatableString("%s", "tab.tools"));
        tab_Scheduler.textProperty().bind(new TranslatableString("%s", "tab.scheduler"));
        tab_Extensions.textProperty().bind(new TranslatableString("%s", "tab.extensions"));
        tab_Extra.textProperty().bind(new TranslatableString("%s", "tab.extra"));
        tab_Info.textProperty().bind(new TranslatableString("%s", "tab.info"));
    }
}
