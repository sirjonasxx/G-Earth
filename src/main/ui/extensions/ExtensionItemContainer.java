package main.ui.extensions;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import main.ui.buttons.SimpleClickButton;
import main.ui.scheduler.ScheduleItem;
import main.ui.buttons.DeleteButton;
import main.ui.buttons.EditButton;
import main.ui.buttons.PauseResumeButton;

/**
 * Created by Jonas on 19/07/18.
 */
public class ExtensionItemContainer extends GridPane {

    public static final int[] columnWidths = {22, 34, 18, 13, 11};
    GEarthExtension item;

    Label titleLabel;
    Label descriptionLabel;
    Label authorLabel;
    Label versionLabel;

    VBox parent;

    ExtensionItemContainer(GEarthExtension item, VBox parent, ScrollPane scrollPane) {
        super();
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

//        getChildren().addAll(indexLabel, packetLabel, delayLabel, destinationLabel);



        DeleteButton deleteButton = new DeleteButton();
        deleteButton.show();
        deleteButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> item.isRemoveClickTrigger());
        SimpleClickButton clickButton = new SimpleClickButton();
        clickButton.show();
        clickButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> item.isClickTrigger());

        HBox buttonsBox = new HBox(clickButton, deleteButton);
        buttonsBox.setSpacing(10);
        buttonsBox.setAlignment(Pos.CENTER);
        GridPane.setMargin(buttonsBox, new Insets(0, 5, 0, 5));
        add(buttonsBox, 4, 0);

        parent.getChildren().add(this);


        GridPane this2 = this;
        item.onDelete(observable -> parent.getChildren().remove(this2));
    }

    private Label initNewLabelColumn(String text) {
        Label label = new Label();
        label.setFont(new Font(12));
        GridPane.setMargin(label, new Insets(0, 0, 0, 5));
        label.setText(text);
        return label;
    }
}
