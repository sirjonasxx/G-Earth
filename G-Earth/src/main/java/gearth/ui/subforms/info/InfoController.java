package gearth.ui.subforms.info;

import gearth.misc.BindingsUtil;
import gearth.misc.HyperLinkUtil;
import gearth.ui.GEarthProperties;
import gearth.ui.titlebar.TitleBarController;
import gearth.ui.translations.LanguageBundle;
import gearth.ui.translations.TranslatableString;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import gearth.ui.SubForm;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by Jonas on 06/04/18.
 */
public class InfoController extends SubForm implements Initializable {

    private static final String PUBKEY = "1GEarthEV9Ua3RcixsKTcuc1PPZd9hqri3";

    public ImageView logoImageView;
    public Hyperlink darkBoxLink;
    public Hyperlink githubGEarthLink;
    public Hyperlink githubTanjiLink;
    public Hyperlink discordGEarthLink;
    public Hyperlink githubExtensionStoreLink;
    public Hyperlink twitterGEarthLink;

    public Label
            themeTitleLabel,
            descriptionLabel,
            createdByLabel,
            contributorsLabel,
            linksLabel;

    public Button donateButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        BindingsUtil.setAndBind(logoImageView.imageProperty(), GEarthProperties.logoImageBinding);
        BindingsUtil.setAndBind(themeTitleLabel.textProperty(), GEarthProperties.themeTitleBinding);

        darkBoxLink.setTooltip(new Tooltip("https://darkbox.nl"));
        discordGEarthLink.setTooltip(new Tooltip("https://discord.gg/AVkcF8y"));
        githubGEarthLink.setTooltip(new Tooltip("https://github.com/sirjonasxx/G-Earth"));
        githubTanjiLink.setTooltip(new Tooltip("https://github.com/ArachisH/Tanji"));
        githubExtensionStoreLink.setTooltip(new Tooltip("https://github.com/sirjonasxx/G-ExtensionStore"));
        twitterGEarthLink.setTooltip(new Tooltip("https://twitter.com/Scripting_Habbo"));

        HyperLinkUtil.showDocumentOnClick(darkBoxLink);
        HyperLinkUtil.showDocumentOnClick(discordGEarthLink);
        HyperLinkUtil.showDocumentOnClick(githubGEarthLink);
        HyperLinkUtil.showDocumentOnClick(githubTanjiLink);
        HyperLinkUtil.showDocumentOnClick(githubExtensionStoreLink);
        HyperLinkUtil.showDocumentOnClick(twitterGEarthLink);

        initLanguageBinding();
    }


    public void donate(ActionEvent actionEvent) {

        final Alert alert = new Alert(Alert.AlertType.INFORMATION, LanguageBundle.get("tab.info.donate.alert.title"), ButtonType.OK);
        alert.setHeaderText(LanguageBundle.get("tab.info.donate.alert.title"));

        final VBox test = new VBox();
        test.getChildren().add(new Label(LanguageBundle.get("tab.info.donate.alert.content")));
        final TextArea pubText = new TextArea(PUBKEY);
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
        descriptionLabel.textProperty().bind(new TranslatableString("%s", "tab.info.description"));
        createdByLabel.textProperty().bind(new TranslatableString("%s:", "tab.info.createdby"));
        contributorsLabel.textProperty().bind(new TranslatableString("%s:", "tab.info.contributors"));
        linksLabel.textProperty().bind(new TranslatableString("%s:", "tab.info.links"));

        donateButton.textProperty().bind(new TranslatableString("%s", "tab.info.donate"));
    }
}
