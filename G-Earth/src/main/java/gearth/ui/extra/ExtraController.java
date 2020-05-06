package gearth.ui.extra;

import gearth.misc.Cacher;
import gearth.protocol.HConnection;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.proxy.SocksConfiguration;
import gearth.protocol.connection.proxy.SocksProxyProvider;
import gearth.ui.SubForm;
import gearth.ui.info.InfoController;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.json.JSONObject;

/**
 * Created by Jonas on 06/04/18.
 */
public class ExtraController extends SubForm implements SocksConfiguration {

    public static final String NOTEPAD_CACHE_KEY = "notepad_text";
    public static final String SOCKS_CACHE_KEY = "socks_config";

    public static final String SOCKS_IP = "ip";
    public static final String SOCKS_PORT = "port";
    public static final String IGNORE_ONCE = "ignore_once";


    public TextArea txtarea_notepad;

    public CheckBox cbx_alwaysOnTop;
    public Hyperlink url_troubleshooting;

    public CheckBox cbx_advanced;
    public GridPane grd_advanced;

    public CheckBox cbx_disableDecryption;
    public CheckBox cbx_debug;


    public CheckBox cbx_useSocks;
    public GridPane grd_socksInfo;
    public TextField txt_socksPort;
    public TextField txt_socksIp;
    public CheckBox cbx_ignoreSocksOnce;

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
            cbx_ignoreSocksOnce.setSelected(socksInitValue.getBoolean(IGNORE_ONCE));
        }

        cbx_debug.selectedProperty().addListener(observable -> HConnection.DEBUG = cbx_debug.isSelected());
        cbx_disableDecryption.selectedProperty().addListener(observable -> HConnection.DECRYPTPACKETS = !cbx_disableDecryption.isSelected());

        cbx_useSocks.selectedProperty().addListener(observable -> grd_socksInfo.setDisable(!cbx_useSocks.isSelected()));

        SocksProxyProvider.setSocksConfig(this);
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
        saveSocksConfig();
    }

    private void saveSocksConfig() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(SOCKS_IP, txt_socksIp.getText());
        jsonObject.put(SOCKS_PORT, txt_socksPort.getText());
        jsonObject.put(IGNORE_ONCE, cbx_ignoreSocksOnce.isSelected());
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
    public boolean dontUseFirstTime() {
        return cbx_ignoreSocksOnce.isSelected();
    }
}
