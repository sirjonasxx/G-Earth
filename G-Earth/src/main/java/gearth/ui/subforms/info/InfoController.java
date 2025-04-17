package gearth.ui.subforms.info;

import gearth.GEarth;
import gearth.ui.titlebar.TitleBarController;
import gearth.ui.translations.LanguageBundle;
import gearth.ui.translations.TranslatableString;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import gearth.ui.SubForm;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * Created by Jonas on 06/04/18.
 */
public class InfoController extends SubForm {
    public ImageView img_logo;
    public Hyperlink link_darkbox;
    public Hyperlink link_g_gearth;
    public Hyperlink link_g_tanji;
    public Hyperlink link_d_gearth;
    public Hyperlink link_g_store;
    public Hyperlink link_t_gearth;

    public Label version, lbl_description, lbl_createdBy, lbl_contrib, lbl_links;
    public Button btn_donate;

    public static void activateHyperlink(Hyperlink link) {
        link.setOnAction((ActionEvent event) -> {
            Hyperlink h = (Hyperlink) event.getTarget();
            String s = h.getTooltip().getText();
            GEarth.main.getHostServices().showDocument(s);
            event.consume();
        });
    }

    public void initialize() {
        link_darkbox.setTooltip(new Tooltip("https://darkbox.nl"));
        link_d_gearth.setTooltip(new Tooltip("https://discord.gg/AVkcF8y"));
        link_g_gearth.setTooltip(new Tooltip("https://github.com/sirjonasxx/G-Earth"));
        link_g_tanji.setTooltip(new Tooltip("https://github.com/ArachisH/Tanji"));
        link_g_store.setTooltip(new Tooltip("https://github.com/sirjonasxx/G-ExtensionStore"));
        link_t_gearth.setTooltip(new Tooltip("https://twitter.com/Scripting_Habbo"));

        activateHyperlink(link_darkbox);
        activateHyperlink(link_d_gearth);
        activateHyperlink(link_g_gearth);
        activateHyperlink(link_g_tanji);
        activateHyperlink(link_g_store);
        activateHyperlink(link_t_gearth);

        initLanguageBinding();
    }

    public void donate(ActionEvent actionEvent) {
        String pubkey = "1GEarthEV9Ua3RcixsKTcuc1PPZd9hqri3";

        Alert alert = new Alert(Alert.AlertType.INFORMATION, LanguageBundle.get("tab.info.donate.alert.title"), ButtonType.OK);
        alert.setHeaderText(LanguageBundle.get("tab.info.donate.alert.title"));

        VBox test = new VBox();
        test.getChildren().add(new Label(LanguageBundle.get("tab.info.donate.alert.content")));
        TextArea pubText = new TextArea(pubkey);
        pubText.setPrefHeight(28);
        pubText.setMaxWidth(250);
        test.getChildren().add(pubText);

        alert.setResizable(false);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.getDialogPane().setContent(test);
        try {
            TitleBarController.create(alert).showAlert();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initLanguageBinding() {
        lbl_description.textProperty().bind(new TranslatableString("%s", "tab.info.description"));
        lbl_createdBy.textProperty().bind(new TranslatableString("%s:", "tab.info.createdby"));
        lbl_contrib.textProperty().bind(new TranslatableString("%s:", "tab.info.contributors"));
        lbl_links.textProperty().bind(new TranslatableString("%s:", "tab.info.links"));

        btn_donate.textProperty().bind(new TranslatableString("%s", "tab.info.donate"));
    }
}
