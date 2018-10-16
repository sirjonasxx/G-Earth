package gearth.misc;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Jonas on 27/09/18.
 */
public class ConfirmationDialog {

    private static Set<String> ignoreDialogs = new HashSet<>();

    public static Alert createAlertWithOptOut(Alert.AlertType type, String dialogKey, String title, String headerText,
                                              String message, String optOutMessage, /*Callback<Boolean, Void> optOutAction,*/
                                              ButtonType... buttonTypes) {
        Alert alert = new Alert(type);
        // Need to force the alert to layout in order to grab the graphic,
        // as we are replacing the dialog pane with a custom pane
        alert.getDialogPane().applyCss();
        Node graphic = alert.getDialogPane().getGraphic();
        // Create a new dialog pane that has a checkbox instead of the hide/show details button
        // Use the supplied callback for the action of the checkbox
        alert.setDialogPane(new DialogPane() {
            @Override
            protected Node createDetailsButton() {
                CheckBox optOut = new CheckBox();
                optOut.setText(optOutMessage);
                optOut.setOnAction(event -> {
                    if (optOut.isSelected()) {
                        ignoreDialogs.add(dialogKey);
                    }
                });
                return optOut;
            }
        });
        alert.getDialogPane().getButtonTypes().addAll(buttonTypes);
        alert.getDialogPane().setContentText(message);
        // Fool the dialog into thinking there is some expandable content
        // a Group won't take up any space if it has no children
        alert.getDialogPane().setExpandableContent(new Group());
        alert.getDialogPane().setExpanded(true);
        // Reset the dialog graphic using the default style
        alert.getDialogPane().setGraphic(graphic);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        return alert;
    }

    public static boolean showDialog(String dialogKey) {
        return !ignoreDialogs.contains(dialogKey);
    }

}
