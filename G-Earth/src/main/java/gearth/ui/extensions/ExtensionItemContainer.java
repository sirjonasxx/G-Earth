package gearth.ui.extensions;

import gearth.services.extensionhandler.extensions.ExtensionType;
import gearth.services.extensionhandler.extensions.GEarthExtension;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import gearth.misc.ConfirmationDialog;
import gearth.ui.buttons.*;
import gearth.services.extensionhandler.extensions.implementations.network.executer.ExecutionInfo;
import gearth.services.extensionhandler.extensions.implementations.network.executer.ExtensionRunner;
import gearth.services.extensionhandler.extensions.implementations.network.executer.ExtensionRunnerFactory;
import gearth.services.extensionhandler.extensions.implementations.network.executer.NormalExtensionRunner;

import java.nio.file.Paths;

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
        Tooltip delete = new Tooltip("Close connection with this extension");
        Tooltip.install(exitButton,delete);
        exitButton.show();
        clickButton = new SimpleClickButton();
        clickButton.show();

        buttonsBox = new HBox(clickButton, exitButton);

        reloadButton = new ReloadButton();
        Tooltip reload = new Tooltip("Restart this extension");
        Tooltip.install(reloadButton, reload);
        reloadButton.show();
        reloadButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            reloadButton.setVisible(false);
            ExtensionRunner runner = ExtensionRunnerFactory.get();
            runner.tryRunExtension(Paths.get(NormalExtensionRunner.JARPATH, ExecutionInfo.EXTENSIONSDIRECTORY, item.getFileName()).toString(), port);
        });

        DeleteButton deleteButton = new DeleteButton();
        Tooltip uninstall = new Tooltip("Uninstall this extension");
        Tooltip.install(deleteButton, uninstall);
        deleteButton.show();
        GridPane this2 = this;

        final String uninstallKey = "uninstallExtension";
        deleteButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            boolean delet_dis = true;

            if (ConfirmationDialog.showDialog(uninstallKey)) {
                Alert alert = ConfirmationDialog.createAlertWithOptOut(Alert.AlertType.CONFIRMATION, uninstallKey
                        ,"Confirmation Dialog", null,
                        "Are you sure want to uninstall this extension?", "Do not ask again",
                        ButtonType.YES, ButtonType.NO
                );

                if (!(alert.showAndWait().filter(t -> t == ButtonType.YES).isPresent())) {
                    delet_dis = false;
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

        if (item.extensionType() == ExtensionType.INTERNAL) {
            setBackground(new Background(new BackgroundFill(Paint.valueOf("F0FFFF"), CornerRadii.EMPTY, Insets.EMPTY)));
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
                setBackground(new Background(new BackgroundFill(Paint.valueOf("#cccccc"),null, null)));
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

        setBackground(new Background(new BackgroundFill(Paint.valueOf("#ffffff"),null, null)));
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
}
