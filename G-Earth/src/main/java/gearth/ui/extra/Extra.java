package gearth.ui.extra;

import gearth.Main;
import gearth.misc.Cacher;
import gearth.ui.SubForm;
import gearth.ui.info.Info;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.scene.control.*;

/**
 * Created by Jonas on 06/04/18.
 */
public class Extra extends SubForm {

    public static final String NOTEPAD_CACHE_KEY = "notepad_text";

    public TextArea txtarea_notepad;

    public CheckBox cbx_alwaysOnTop;
    public Hyperlink url_troubleshooting;

    public CheckBox cbx_advanced;

    public CheckBox cbx_ovcinfo;
    public TextField txt_realPort;
    public TextField txt_mitmIP;
    public TextField txt_realIp;
    public TextField txt_mitmPort;

    public CheckBox cbx_disableDecryption;
    public CheckBox txt_debug;

    public void initialize() {
        url_troubleshooting.setTooltip(new Tooltip("https://github.com/sirjonasxx/G-Earth/wiki/Troubleshooting"));
        Info.activateHyperlink(url_troubleshooting);

        String notepadInitValue = (String)Cacher.get(NOTEPAD_CACHE_KEY);
        if (notepadInitValue != null) {
            txtarea_notepad.setText(notepadInitValue);
        }
    }

    @Override
    protected void onParentSet() {
        parentController.getStage().setAlwaysOnTop(cbx_alwaysOnTop.isSelected());
        cbx_alwaysOnTop.selectedProperty().addListener(observable -> parentController.getStage().setAlwaysOnTop(cbx_alwaysOnTop.isSelected()));
    }

    @Override
    protected void onExit() {
        Cacher.put(NOTEPAD_CACHE_KEY, txtarea_notepad.getText());
    }
}
