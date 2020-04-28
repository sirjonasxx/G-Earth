package gearth.ui.scheduler;

import gearth.services.scheduler.ScheduleItem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import gearth.ui.buttons.DeleteButton;
import gearth.ui.buttons.EditButton;
import gearth.ui.buttons.PauseResumeButton;

/**
 * Created by Jonas on 07/04/18.
 */
public class ScheduleItemContainer extends GridPane {

    private static final int[] columnWidths = {10, 39, 16, 18, 15};
    private InteractableScheduleItem item;

    private Label indexLabel;
    private Label packetLabel;
    private Label delayLabel;
    private Label destinationLabel;

    private VBox parent;

    ScheduleItemContainer(InteractableScheduleItem item, VBox parent, ScrollPane scrollPane) {
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

        indexLabel = initNewLabelColumn(item.getIndexProperty().get()+"");
        packetLabel = initNewLabelColumn(item.getPacketAsStringProperty().get());
        delayLabel = initNewLabelColumn(item.getDelayProperty().get()+"");
        destinationLabel = initNewLabelColumn(item.getDestinationProperty().get().name());

        add(indexLabel, 0, 0);
        add(packetLabel, 1, 0);
        add(delayLabel, 2, 0);
        add(destinationLabel, 3, 0);

//        getChildren().addAll(indexLabel, packetLabel, delayLabel, destinationLabel);

        item.getIndexProperty().addListener(observable -> indexLabel.setText(item.getIndexProperty().get()+""));
        item.getPacketAsStringProperty().addListener(observable -> packetLabel.setText(item.getPacketAsStringProperty().get()));
        item.getDelayProperty().addListener(observable -> delayLabel.setText(item.getDelayProperty().get()+""));
        item.getDestinationProperty().addListener(observable -> destinationLabel.setText(item.getDestinationProperty().get().name()));

        EditButton editButton = new EditButton();
        DeleteButton deleteButton = new DeleteButton();
        PauseResumeButton pauseResumeButton = new PauseResumeButton(item.getPausedProperty().get());
        item.getPausedProperty().addListener(observable -> pauseResumeButton.setPaused(item.getPausedProperty().get()));
        editButton.show();
        deleteButton.show();
        editButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> item.edit());
        deleteButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> item.delete());
        pauseResumeButton.onClick(observable -> item.getPausedProperty().set(!pauseResumeButton.isPaused()));
        HBox buttonsBox = new HBox(pauseResumeButton, editButton, deleteButton);
        buttonsBox.setSpacing(10);
        buttonsBox.setAlignment(Pos.CENTER);
        GridPane.setMargin(buttonsBox, new Insets(0, 5, 0, 5));
        add(buttonsBox, 4, 0);

        parent.getChildren().add(this);


        GridPane this2 = this;
        item.onDelete(() -> parent.getChildren().remove(this2));
        item.onIsBeingUpdated(() -> setStyle("-fx-background-color: #faebcc;"));
        item.onIsupdated(() -> setStyle("-fx-background-color: #ffffff;"));
    }

    private Label initNewLabelColumn(String text) {
        Label label = new Label();
//        label.setMaxWidth(Double.MAX_VALUE);
//        label.setMinHeight(Double.MAX_VALUE);
//        label.setAlignment(Pos.CENTER);
        label.setFont(new Font(12));
        GridPane.setMargin(label, new Insets(0, 0, 0, 5));
        label.setText(text);
        return label;
    }

}
