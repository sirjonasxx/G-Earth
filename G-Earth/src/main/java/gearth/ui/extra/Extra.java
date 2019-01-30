package gearth.ui.extra;

import gearth.misc.Cacher;
import gearth.protocol.HConnection;
import gearth.protocol.misc.ConnectionInfoOverrider;
import gearth.ui.SubForm;
import gearth.ui.info.Info;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

/**
 * Created by Jonas on 06/04/18.
 */
public class Extra extends SubForm implements ConnectionInfoOverrider {

    public static final String NOTEPAD_CACHE_KEY = "notepad_text";

    public TextArea txtarea_notepad;

    public CheckBox cbx_alwaysOnTop;
    public Hyperlink url_troubleshooting;

    public CheckBox cbx_advanced;
    public GridPane grd_advanced;

    public CheckBox cbx_ovcinfo;
    public GridPane grd_ovcinfo;

    public TextField txt_realPort;
    public TextField txt_mitmIP;
    public TextField txt_realIp;
    public TextField txt_mitmPort;

    public CheckBox cbx_disableDecryption;
    public CheckBox cbx_debug;

    public void initialize() {
        HConnection.setConnectionInfoOverrider(this);

        url_troubleshooting.setTooltip(new Tooltip("https://github.com/sirjonasxx/G-Earth/wiki/Troubleshooting"));
        Info.activateHyperlink(url_troubleshooting);

        String notepadInitValue = (String)Cacher.get(NOTEPAD_CACHE_KEY);
        if (notepadInitValue != null) {
            txtarea_notepad.setText(notepadInitValue);
        }

        cbx_debug.selectedProperty().addListener(observable -> HConnection.DEBUG = cbx_debug.isSelected());
        cbx_disableDecryption.selectedProperty().addListener(observable -> HConnection.DECRYPTPACKETS = !cbx_disableDecryption.isSelected());

        cbx_ovcinfo.selectedProperty().addListener(observable -> grd_ovcinfo.setDisable(!cbx_ovcinfo.isSelected()));
    }

    @Override
    protected void onParentSet() {
        parentController.getStage().setAlwaysOnTop(cbx_alwaysOnTop.isSelected());
        cbx_alwaysOnTop.selectedProperty().addListener(observable -> parentController.getStage().setAlwaysOnTop(cbx_alwaysOnTop.isSelected()));

        cbx_advanced.selectedProperty().addListener(observable -> updateAdvancedUI());
        getHConnection().addStateChangeListener((oldState, newState) -> {
            if (oldState == HConnection.State.NOT_CONNECTED || newState == HConnection.State.NOT_CONNECTED) {
                updateAdvancedUI();
            }
        });

        updateAdvancedUI();
    }

    @Override
    protected void onExit() {
        Cacher.put(NOTEPAD_CACHE_KEY, txtarea_notepad.getText());
    }

    private void updateAdvancedUI() {
        if (!cbx_advanced.isSelected()) {
            cbx_debug.setSelected(false);
            cbx_ovcinfo.setSelected(false);
            if (getHConnection().getState() == HConnection.State.NOT_CONNECTED) {
                cbx_disableDecryption.setSelected(false);
            }
        }
        grd_advanced.setDisable(!cbx_advanced.isSelected());

        cbx_disableDecryption.setDisable(getHConnection().getState() != HConnection.State.NOT_CONNECTED);
    }

    @Override
    public boolean mustOverrideConnection() {
        return cbx_ovcinfo.isSelected();
    }

    @Override
    public HConnection.Proxy getOverrideProxy() {
        return new HConnection.Proxy(
                txt_realIp.getText(),
                txt_realIp.getText(),
                Integer.parseInt(txt_realPort.getText()),
                Integer.parseInt(txt_mitmPort.getText()),
                txt_mitmIP.getText()
        );
    }
}
