package gearth.ui.info;

import gearth.Main;
import javafx.event.ActionEvent;
import javafx.scene.control.Hyperlink;
import gearth.ui.SubForm;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Created by Jonas on 06/04/18.
 */
public class Info extends SubForm {
    public ImageView img_logo;
    public Hyperlink link_sng;
    public Hyperlink link_darkbox;
    public Hyperlink link_d_harble;
    public Hyperlink link_g_gearth;
    public Hyperlink link_g_tanji;
    public Hyperlink link_d_bonnie;
    public Label version;

    // this is a TEMPORARY info tab

    private void activateHyperlink(Hyperlink link) {
        link.setOnAction((ActionEvent event) -> {
            Hyperlink h = (Hyperlink) event.getTarget();
            String s = h.getTooltip().getText();
            Main.main.getHostServices().showDocument(s);
            event.consume();
        });
    }

    public void initialize() {
        version.setText(version.getText().replace("$version", Main.version));

        img_logo.setImage(new Image("/gearth/G-EarthLogo.png"));

        link_sng.setTooltip(new Tooltip("https://www.sngforum.info"));
        link_darkbox.setTooltip(new Tooltip("https://darkbox.nl"));
        link_g_gearth.setTooltip(new Tooltip("https://github.com/sirjonasxx/G-Earth"));
        link_g_tanji.setTooltip(new Tooltip("https://github.com/ArachisH/Tanji"));
        link_d_harble.setTooltip(new Tooltip("https://discord.gg/Vyc2gFC"));

//        activateHyperlink(link_d_harble);
        activateHyperlink(link_g_gearth);
        activateHyperlink(link_g_tanji);
        activateHyperlink(link_sng);
        activateHyperlink(link_darkbox);
    }
}
