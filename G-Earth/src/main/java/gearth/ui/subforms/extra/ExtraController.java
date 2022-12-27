package gearth.ui.subforms.extra;

import gearth.GEarth;
import gearth.misc.Cacher;
import gearth.protocol.HConnection;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.proxy.ProxyProviderFactory;
import gearth.protocol.connection.proxy.SocksConfiguration;
import gearth.services.always_admin.AdminService;
import gearth.services.g_python.GPythonVersionUtils;
import gearth.ui.SubForm;
import gearth.ui.subforms.info.InfoController;
import gearth.ui.titlebar.TitleBarController;
import gearth.ui.translations.LanguageBundle;
import gearth.ui.translations.TranslatableString;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Optional;

/**
 * Created by Jonas on 06/04/18.
 */
public class ExtraController extends SubForm implements SocksConfiguration {

    public static final String INFO_URL_GPYTHON = "https://github.com/sirjonasxx/G-Earth/wiki/G-Python-qtConsole";

    public static final String NOTEPAD_CACHE_KEY = "notepad_text";
    public static final String DEVELOP_CACHE_KEY = "develop_mode";
    public static final String ALWAYS_ADMIN_KEY = "always_admin";
    public static final String SOCKS_CACHE_KEY = "socks_config";
    public static final String GPYTHON_CACHE_KEY = "use_gpython";

    public static final String SOCKS_IP = "ip";
    public static final String SOCKS_PORT = "port";
//    public static final String IGNORE_ONCE = "ignore_once";


    public TextArea txtarea_notepad;

    public CheckBox cbx_alwaysOnTop;
    public Hyperlink url_troubleshooting;

    //TODO add setup link to g-earth wiki
    public CheckBox cbx_gpython;

    public CheckBox cbx_advanced;
    public GridPane grd_advanced;

    public CheckBox cbx_disableDecryption;
    public CheckBox cbx_debug;

    public CheckBox cbx_useSocks;
    public GridPane grd_socksInfo;
    public TextField txt_socksIp;
    public CheckBox cbx_admin;
    public Label lbl_notepad, lbl_proxyIp;
    public CheckBox cbx_develop;

    private AdminService adminService;

    public void initialize() {
        url_troubleshooting.setTooltip(new Tooltip("https://github.com/sirjonasxx/G-Earth/wiki/Troubleshooting"));
        InfoController.activateHyperlink(url_troubleshooting);

        String notepadInitValue = (String)Cacher.get(NOTEPAD_CACHE_KEY);
        if (notepadInitValue != null) {
            txtarea_notepad.setText(notepadInitValue);
        }

        if (Cacher.getCacheContents().has(SOCKS_CACHE_KEY)) {
            JSONObject socksInitValue = Cacher.getCacheContents().getJSONObject(SOCKS_CACHE_KEY);
            txt_socksIp.setText(socksInitValue.getString(SOCKS_IP) + ":" + socksInitValue.getInt(SOCKS_PORT));
//            cbx_socksUseIfNeeded.setSelected(socksInitValue.getBoolean(IGNORE_ONCE));
        }

        if (Cacher.getCacheContents().has(GPYTHON_CACHE_KEY)) {
            cbx_gpython.setSelected(Cacher.getCacheContents().getBoolean(GPYTHON_CACHE_KEY));
        }

        if (Cacher.getCacheContents().has(ALWAYS_ADMIN_KEY)) {
            cbx_admin.setSelected(Cacher.getCacheContents().getBoolean(ALWAYS_ADMIN_KEY));
        }

        cbx_debug.selectedProperty().addListener(observable -> HConnection.DEBUG = cbx_debug.isSelected());
        cbx_disableDecryption.selectedProperty().addListener(observable -> HConnection.DECRYPTPACKETS = !cbx_disableDecryption.isSelected());

        cbx_useSocks.selectedProperty().addListener(observable -> grd_socksInfo.setDisable(!cbx_useSocks.isSelected()));

        ProxyProviderFactory.setSocksConfig(this);

        initLanguageBinding();
    }

    @Override
    protected void onParentSet() {
        adminService = new AdminService(cbx_admin.isSelected(), getHConnection());
        getHConnection().addTrafficListener(1, message -> adminService.onMessage(message));
        getHConnection().getStateObservable().addListener((oldState, newState) -> {if (newState == HState.CONNECTED) adminService.onConnect();});

        parentController.getStage().setAlwaysOnTop(cbx_alwaysOnTop.isSelected());
        cbx_alwaysOnTop.selectedProperty().addListener(observable -> parentController.getStage().setAlwaysOnTop(cbx_alwaysOnTop.isSelected()));

        cbx_advanced.selectedProperty().addListener(observable -> updateAdvancedUI());
        getHConnection().getStateObservable().addListener((oldState, newState) -> {
            if (oldState == HState.NOT_CONNECTED || newState == HState.NOT_CONNECTED) {
                updateAdvancedUI();
            }
        });

        if (Cacher.getCacheContents().has(DEVELOP_CACHE_KEY)) {
            boolean inDevelopMode = Cacher.getCacheContents().getBoolean(DEVELOP_CACHE_KEY);
            setDevelopMode(inDevelopMode);
        }

        updateAdvancedUI();
    }

    @Override
    protected void onExit() {
        Cacher.put(NOTEPAD_CACHE_KEY, txtarea_notepad.getText());
        Cacher.put(GPYTHON_CACHE_KEY, cbx_gpython.isSelected());
        Cacher.put(ALWAYS_ADMIN_KEY, cbx_admin.isSelected());
        Cacher.put(DEVELOP_CACHE_KEY, cbx_develop.isSelected());
        saveSocksConfig();
    }

    private void saveSocksConfig() {
        if (txt_socksIp.getText().contains(":")) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(SOCKS_IP, getSocksHost());
            jsonObject.put(SOCKS_PORT, getSocksPort());
            Cacher.put(SOCKS_CACHE_KEY, jsonObject);
        }
        else {
            Cacher.remove(SOCKS_CACHE_KEY);
        }
    }

    private void updateAdvancedUI() {
        if (!cbx_advanced.isSelected()) {
            cbx_debug.setSelected(false);
            cbx_useSocks.setSelected(false);
            if (getHConnection().getState() == HState.NOT_CONNECTED) {
                cbx_disableDecryption.setSelected(false);
            }
        }
        grd_advanced.setDisable(!cbx_advanced.isSelected());

        cbx_disableDecryption.setDisable(getHConnection().getState() != HState.NOT_CONNECTED);
    }

    @Override
    public boolean useSocks() {
        saveSocksConfig();
        return cbx_useSocks.isSelected();
    }

    @Override
    public int getSocksPort() {
        String socksString = txt_socksIp.getText();
        if (socksString.contains(":")) {
            return Integer.parseInt(socksString.split(":")[1]);
        }
        return 1337;
    }

    @Override
    public String getSocksHost() {
        return txt_socksIp.getText().split(":")[0];
    }

    @Override
    public boolean onlyUseIfNeeded() {
//        return cbx_socksUseIfNeeded.isSelected();
        return false;
    }

    public boolean useGPython() {
        return cbx_gpython.isSelected();
    }

    public void gpythonCbxClick(ActionEvent actionEvent) {
        if (cbx_gpython.isSelected()) {
            new Thread(() -> {
                Platform.runLater(() -> {
                    cbx_gpython.setSelected(false);
                    cbx_gpython.setDisable(true);
                });
                if (!GPythonVersionUtils.validInstallation()) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR, LanguageBundle.get("tab.extra.options.pythonscripting.alert.title"), ButtonType.OK);
                        alert.setTitle(LanguageBundle.get("tab.extra.options.pythonscripting.alert.title"));

                        FlowPane fp = new FlowPane();
                        Label lbl = new Label(LanguageBundle.get("tab.extra.options.pythonscripting.alert.content") +
                                System.lineSeparator() + System.lineSeparator() +
                                LanguageBundle.get("tab.extra.options.pythonscripting.alert.moreinformation"));
                        Hyperlink link = new Hyperlink(INFO_URL_GPYTHON);
                        fp.getChildren().addAll( lbl, link);
                        link.setOnAction(event -> {
                            GEarth.main.getHostServices().showDocument(link.getText());
                            event.consume();
                        });

                        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                        alert.getDialogPane().setContent(fp);
                        try {
                            TitleBarController.create(alert).showAlert();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        cbx_gpython.setDisable(false);
                    });
                }
                else {
                    Platform.runLater(() -> {
                        cbx_gpython.setSelected(true);
                        cbx_gpython.setDisable(false);
                        parentController.extensionsController.updateGPythonStatus();
                    });
                }
            }).start();


        }

    }

    public void developCbxClick(ActionEvent actionEvent) {
        if (cbx_develop.isSelected()) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING, LanguageBundle.get("tab.extra.options.developmode.alert.title"), ButtonType.NO, ButtonType.YES);
                alert.setTitle(LanguageBundle.get("tab.extra.options.developmode.alert.title"));

                Label lbl = new Label(LanguageBundle.get("tab.extra.options.developmode.alert.content"));

                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                alert.getDialogPane().setContent(lbl);

                try {
                    Optional<ButtonType> result = TitleBarController.create(alert).showAlertAndWait();
                    if (!result.isPresent() || result.get() == ButtonType.NO) {
                        cbx_develop.setSelected(false);
                    }
                    else {
                        setDevelopMode(true);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        else {
            setDevelopMode(false);
        }
    }

    private void setDevelopMode(boolean enabled) {
        cbx_develop.setSelected(enabled);
        getHConnection().setDeveloperMode(enabled);
        parentController.extensionsController.setLocalInstallingEnabled(enabled);
    }

    public void adminCbxClick(ActionEvent actionEvent) {
        adminService.setEnabled(cbx_admin.isSelected());
    }

    private void initLanguageBinding() {
        url_troubleshooting.textProperty().bind(new TranslatableString("%s", "tab.extra.troubleshooting"));

        lbl_notepad.textProperty().bind(new TranslatableString("%s:", "tab.extra.notepad"));
        lbl_proxyIp.textProperty().bind(new TranslatableString("%s:", "tab.extra.options.advanced.proxy.ip"));

        cbx_alwaysOnTop.textProperty().bind(new TranslatableString("%s", "tab.extra.options.alwaysontop"));

        cbx_develop.textProperty().bind(new TranslatableString("%s", "tab.extra.options.developmode"));
        cbx_admin.textProperty().bind(new TranslatableString("%s", "tab.extra.options.staffpermissions"));
        cbx_gpython.textProperty().bind(new TranslatableString("%s", "tab.extra.options.pythonscripting"));
        cbx_advanced.textProperty().bind(new TranslatableString("%s", "tab.extra.options.advanced"));

        cbx_useSocks.textProperty().bind(new TranslatableString("%s", "tab.extra.options.advanced.socks"));
        cbx_disableDecryption.textProperty().bind(new TranslatableString("%s", "tab.extra.options.advanced.disabledecryption"));
        cbx_debug.textProperty().bind(new TranslatableString("%s", "tab.extra.options.advanced.debugstdout"));
    }
}
