package gearth.ui.extra;

import gearth.Main;
import gearth.ui.SubForm;
import gearth.ui.info.Info;
import javafx.event.ActionEvent;
import javafx.scene.control.*;

/**
 * Created by Jonas on 06/04/18.
 */
public class Extra extends SubForm {

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
    }

}
