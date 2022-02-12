package gearth.ui;

import gearth.Main;
import gearth.protocol.connection.proxy.ProxyProviderFactory;
import gearth.protocol.connection.proxy.SocksConfiguration;
import gearth.ui.logger.loggerdisplays.PacketLoggerFactory;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;
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
import java.util.Arrays;
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

    public Pane titleBar;
    public Label titleLabel;

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

    public void setTheme(String theme) {
        Main.theme = theme;

        getStage().getScene().getStylesheets().clear();
        getStage().getScene().getStylesheets().add(Main.class.getResource(String.format("/gearth/themes/%s/styling.css", theme)).toExternalForm());

        getStage().getIcons().clear();
        getStage().getIcons().add(new Image(Main.class.getResourceAsStream(String.format("/gearth/themes/%s/logoSmall.png", theme))));

        getStage().setTitle(theme.split("_")[0] + " " + Main.version);
        titleLabel.setText(getStage().getTitle());

        infoController.img_logo.setImage(new Image(Main.class.getResourceAsStream(String.format("/gearth/themes/%s/logo.png", theme))));
        infoController.version.setText(getStage().getTitle());
    }


    public void exit() {
        tabs.forEach(SubForm::exit);
        hConnection.abort();
    }

    public void handleCloseAction(MouseEvent event) {
        this.exit();
        Platform.exit();

        // Platform.exit doesn't seem to be enough on Windows?
        System.exit(0);
    }

    public void handleMinimizeAction(MouseEvent event) {
        getStage().setIconified(true);
    }

    private double xOffset, yOffset;

    public void handleClickAction(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    public void handleMovementAction(MouseEvent event) {
        getStage().setX(event.getScreenX() - xOffset);
        getStage().setY(event.getScreenY() - yOffset);
    }

    public void toggleTheme(MouseEvent event) {
        int themeIndex = Arrays.asList(Main.themes).indexOf(Main.theme);
        setTheme(Main.themes[(themeIndex + 1) % Main.themes.length]);
    }
}
