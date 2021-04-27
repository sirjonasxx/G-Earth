package gearth.ui.extra;

import gearth.Main;
import gearth.misc.Cacher;
import gearth.protocol.HConnection;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.proxy.ProxyProviderFactory;
import gearth.protocol.connection.proxy.SocksConfiguration;
import gearth.services.gpython.GPythonVersionUtils;
import gearth.ui.SubForm;
import gearth.ui.info.InfoController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import org.json.JSONObject;

/**
 * Created by Jonas on 06/04/18.
 */
public class ExtraController extends SubForm implements SocksConfiguration {

    public static final String INFO_URL_GPYTHON = "https://github.com/sirjonasxx/G-Earth/wiki/G-Python-qtConsole";

    public static final String NOTEPAD_CACHE_KEY = "notepad_text";
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
    public TextField txt_socksPort;
    public TextField txt_socksIp;

    public void initialize() {
        url_troubleshooting.setTooltip(new Tooltip("https://github.com/sirjonasxx/G-Earth/wiki/Troubleshooting"));
        InfoController.activateHyperlink(url_troubleshooting);

        String notepadInitValue = (String)Cacher.get(NOTEPAD_CACHE_KEY);
        if (notepadInitValue != null) {
            txtarea_notepad.setText(notepadInitValue);
        }

        if (Cacher.getCacheContents().has(SOCKS_CACHE_KEY)) {
            JSONObject socksInitValue = Cacher.getCacheContents().getJSONObject(SOCKS_CACHE_KEY);
            txt_socksIp.setText(socksInitValue.getString(SOCKS_IP));
            txt_socksPort.setText(socksInitValue.getString(SOCKS_PORT));
//            cbx_socksUseIfNeeded.setSelected(socksInitValue.getBoolean(IGNORE_ONCE));
        }

        if (Cacher.getCacheContents().has(GPYTHON_CACHE_KEY)) {
            cbx_gpython.setSelected(Cacher.getCacheContents().getBoolean(GPYTHON_CACHE_KEY));
        }

        cbx_debug.selectedProperty().addListener(observable -> HConnection.DEBUG = cbx_debug.isSelected());
        cbx_disableDecryption.selectedProperty().addListener(observable -> HConnection.DECRYPTPACKETS = !cbx_disableDecryption.isSelected());

        cbx_useSocks.selectedProperty().addListener(observable -> grd_socksInfo.setDisable(!cbx_useSocks.isSelected()));

        ProxyProviderFactory.setSocksConfig(this);
    }

    @Override
    protected void onParentSet() {
        parentController.getStage().setAlwaysOnTop(cbx_alwaysOnTop.isSelected());
        cbx_alwaysOnTop.selectedProperty().addListener(observable -> parentController.getStage().setAlwaysOnTop(cbx_alwaysOnTop.isSelected()));

        cbx_advanced.selectedProperty().addListener(observable -> updateAdvancedUI());
        getHConnection().getStateObservable().addListener((oldState, newState) -> {
            if (oldState == HState.NOT_CONNECTED || newState == HState.NOT_CONNECTED) {
                updateAdvancedUI();
            }
        });

        updateAdvancedUI();
    }

    @Override
    protected void onExit() {
        Cacher.put(NOTEPAD_CACHE_KEY, txtarea_notepad.getText());
        Cacher.put(GPYTHON_CACHE_KEY, cbx_gpython.isSelected());
        saveSocksConfig();
    }

    private void saveSocksConfig() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(SOCKS_IP, txt_socksIp.getText());
        jsonObject.put(SOCKS_PORT, txt_socksPort.getText());
//        jsonObject.put(IGNORE_ONCE, cbx_socksUseIfNeeded.isSelected());
        Cacher.put(SOCKS_CACHE_KEY, jsonObject);
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
        return Integer.parseInt(txt_socksPort.getText());
    }

    @Override
    public String getSocksHost() {
        return txt_socksIp.getText();
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
                        Alert alert = new Alert(Alert.AlertType.ERROR, "G-Python installation", ButtonType.OK);
                        alert.setTitle("G-Python installation");

                        FlowPane fp = new FlowPane();
                        Label lbl = new Label("Before using G-Python, install the right packages using pip!" +
                                System.lineSeparator() + System.lineSeparator() + "More information here:");
                        Hyperlink link = new Hyperlink(INFO_URL_GPYTHON);
                        fp.getChildren().addAll( lbl, link);
                        link.setOnAction(event -> {
                            Main.main.getHostServices().showDocument(link.getText());
                            event.consume();
                        });

                        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                        alert.getDialogPane().setContent(fp);
                        alert.show();

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
}
