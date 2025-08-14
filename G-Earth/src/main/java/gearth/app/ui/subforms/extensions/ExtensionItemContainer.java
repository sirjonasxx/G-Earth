package gearth.app.ui.subforms.extensions;

import gearth.app.misc.ConfirmationDialog;
import gearth.services.extension_handler.extensions.ExtensionType;
import gearth.services.extension_handler.extensions.GEarthExtension;
import gearth.app.services.extension_handler.extensions.implementations.network.executer.ExecutionInfo;
import gearth.app.services.extension_handler.extensions.implementations.network.executer.ExtensionRunner;
import gearth.app.services.extension_handler.extensions.implementations.network.executer.ExtensionRunnerFactory;
import gearth.app.ui.buttons.DeleteButton;
import gearth.app.ui.buttons.ExitButton;
import gearth.app.ui.buttons.ReloadButton;
import gearth.app.ui.buttons.SimpleClickButton;
import gearth.app.ui.titlebar.TitleBarAlert;
import gearth.app.ui.translations.LanguageBundle;
import gearth.app.ui.translations.TranslatableString;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.io.File;
import java.io.IOException;

/**
 * Created by Jonas on 19/07/18.
 */
public class ExtensionItemContainer extends GridPane {

    public static final int[] columnWidths = {22, 34, 18, 13, 11};
    private GEarthExtension item;

    private Label titleLabel;
    private Label descriptionLabel;
    private Label authorLabel;
    private Label versionLabel;

    private VBox parent;

    private volatile int port;

    private HBox buttonsBox = null;
    private HBox additionalButtonBox = null;

    private ExitButton exitButton;
    private SimpleClickButton clickButton;
    private ReloadButton reloadButton;

    ExtensionItemContainer(GEarthExtension item, VBox parent, ScrollPane scrollPane, int port) {
        super();
        this.port = port;
        setGridLinesVisible(true);
        VBox.setMargin(this, new Insets(2, -2, -2, -2));

        setPrefWidth(scrollPane.getWidth());
        setPrefHeight(23);
        scrollPane.widthProperty().addListener(observable -> setPrefWidth(scrollPane.getWidth()));
        this.parent = parent;
        this.item = item;
        initialize();
    }

    private void initialize() {
        RowConstraints rowConstraints = new RowConstraints(23);
        getRowConstraints().addAll(rowConstraints);

        for (int i = 0; i < columnWidths.length; i++) {
            ColumnConstraints columnConstraints = new ColumnConstraints(20);
            columnConstraints.setPercentWidth(columnWidths[i]);
            getColumnConstraints().add(columnConstraints);
        }

        titleLabel = initNewLabelColumn(item.getTitle());
        descriptionLabel = initNewLabelColumn(item.getDescription());
        authorLabel = initNewLabelColumn(item.getAuthor());
        versionLabel = initNewLabelColumn(item.getVersion());

        add(titleLabel, 0, 0);
        add(descriptionLabel, 1, 0);
        add(authorLabel, 2, 0);
        add(versionLabel, 3, 0);

        exitButton = new ExitButton();
        Tooltip delete = new Tooltip();
        delete.textProperty().bind(new TranslatableString("%s", "tab.extensions.table.edit.delete.tooltip"));
        Tooltip.install(exitButton,delete);
        exitButton.show();
        clickButton = new SimpleClickButton();
        clickButton.show();

        buttonsBox = new HBox(clickButton, exitButton);

        reloadButton = new ReloadButton();
        Tooltip reload = new Tooltip();
        reload.textProperty().bind(new TranslatableString("%s", "tab.extensions.table.edit.restart.tooltip"));
        Tooltip.install(reloadButton, reload);
        reloadButton.show();
        reloadButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            reloadButton.setVisible(false);
            ExtensionRunner runner = ExtensionRunnerFactory.get();
            runner.tryRunExtension(new File(ExecutionInfo.EXTENSIONS_DIRECTORY, item.getFileName()), port);
        });

        DeleteButton deleteButton = new DeleteButton();
        deleteButton.show();
        GridPane this2 = this;

        final String uninstallKey = "uninstallExtension";
        deleteButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            boolean delet_dis = true;

            if (ConfirmationDialog.showDialog(uninstallKey)) {
                Alert alert = ConfirmationDialog.createAlertWithOptOut(Alert.AlertType.CONFIRMATION, uninstallKey
                        , LanguageBundle.get("alert.confirmation.windowtitle"), null,
                        LanguageBundle.get("tab.extensions.table.edit.uninstall.confirmation"), LanguageBundle.get("alert.confirmation.button.donotaskagain"),
                        ButtonType.YES, ButtonType.NO
                );

                try {
                    if (!(TitleBarAlert.create(alert).showAlertAndWait()
                            .filter(t -> t == ButtonType.YES).isPresent())) {
                        delet_dis = false;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (delet_dis) {
                ExtensionRunner runner = ExtensionRunnerFactory.get();
                runner.uninstallExtension(item.getFileName());
                parent.getChildren().remove(this2);
            }
        });

        additionalButtonBox = new HBox(reloadButton, deleteButton);

        clickButton.setVisible(item.isFireButtonUsed());
        exitButton.setVisible(item.isLeaveButtonVisible());
        deleteButton.setVisible(item.isDeleteButtonVisible());

        buttonsBox.setSpacing(8);
        buttonsBox.setAlignment(Pos.CENTER);
        additionalButtonBox.setSpacing(8);
        additionalButtonBox.setAlignment(Pos.CENTER);

        GridPane.setMargin(buttonsBox, new Insets(0, 5, 0, 5));
        GridPane.setMargin(additionalButtonBox, new Insets(0, 5, 0, 5));
        add(buttonsBox, 4, 0);

        parent.getChildren().add(this);

        getStyleClass().clear();
        getStyleClass().add("tableRowActive");

        if (item.extensionType() == ExtensionType.INTERNAL) {
            getStyleClass().clear();
            getStyleClass().add("tableRowBlue");
        }


        initExtension();
    }

    private Label initNewLabelColumn(String text) {
        Label label = new Label();
        label.setFont(new Font(12));
        GridPane.setMargin(label, new Insets(0, 0, 0, 5));
        label.setText(text);
        return label;
    }

    private EventHandler<MouseEvent> onExit = null;
    private EventHandler<MouseEvent> onClick = null;

    void initExtension(){
        if (onExit != null) {
            exitButton.removeEventHandler(MouseEvent.MOUSE_CLICKED, onExit);
            clickButton.removeEventHandler(MouseEvent.MOUSE_CLICKED, onClick);
        }
        onExit = event -> item.getRemoveClickObservable().fireEvent();
        onClick = event -> item.getClickedObservable().fireEvent();

        exitButton.addEventHandler(MouseEvent.MOUSE_CLICKED, onExit);
        clickButton.addEventHandler(MouseEvent.MOUSE_CLICKED, onClick);

        ExtensionItemContainer this2 = this;
        item.getDeletedObservable().addListener(() -> Platform.runLater(() -> {
            if (item.isInstalledExtension()) {
                getStyleClass().clear();
                getStyleClass().add("tableRowInactive");
                getChildren().remove(buttonsBox);
                add(additionalButtonBox, 4, 0);
                reloadButton.setVisible(true);
            }
            else {
                parent.getChildren().remove(this2);
            }
        }));
    }

    void hasReconnected(GEarthExtension extension) {
        item = extension;
        initExtension();

        getStyleClass().clear();
        getStyleClass().add("tableRowActive");
        getChildren().remove(additionalButtonBox);
        if (buttonsBox != null) {
            add(buttonsBox, 4, 0);
        }
    }

    //returns null if there is none
    String getExtensionFileName() {
        if (item.isInstalledExtension()) {
            return item.getFileName();
        }
        return null;
    }

    private void initLanguageBinding() {

    }
}
