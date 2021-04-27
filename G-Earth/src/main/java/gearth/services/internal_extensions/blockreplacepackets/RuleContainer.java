package gearth.services.internal_extensions.blockreplacepackets;

import gearth.services.internal_extensions.blockreplacepackets.rules.BlockReplaceRule;
import gearth.ui.buttons.DeleteButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

/**
 * Created by Jonas on 6/11/2018.
 */
public class RuleContainer extends GridPane {

    public static final int[] columnWidths = {12, 14, 18, 33, 15, 6};

    VBox parent;
    BlockReplaceRule item;

    RuleContainer(BlockReplaceRule item, VBox parent) {
        super();

        this.parent = parent;
        this.item = item;

        setGridLinesVisible(true);
        VBox.setMargin(this, new Insets(2, -2, -2, -2));

        setPrefWidth(parent.getWidth());
        setPrefHeight(23);
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

        Label optionLabel = initNewLabelColumn(item.option().name());
        Label typeLabel = initNewLabelColumn(item.type().name());
        Label valueLabel = initNewLabelColumn(item.value());
        Label replacementLabel = initNewLabelColumn(item.replacement());
        Label destinationLabel = initNewLabelColumn(item.side().name());

        add(optionLabel, 0, 0);
        add(typeLabel, 1, 0);
        add(valueLabel, 2, 0);
        add(replacementLabel, 3,  0);
        add(destinationLabel, 4, 0);

        DeleteButton deleteButton = new DeleteButton();
        deleteButton.setAlignment(Pos.CENTER);
        deleteButton.show();

        RuleContainer thiss = this;
        item.onDelete(observable -> parent.getChildren().remove(thiss));
        deleteButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> item.delete());

        add(deleteButton, 5, 0);


        parent.getChildren().add(this);
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
