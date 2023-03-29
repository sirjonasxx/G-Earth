package gearth.ui.alert;

import gearth.ui.titlebar.TitleBarController;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

public class AlertUtil {

    private final static Logger LOGGER = LoggerFactory.getLogger(AlertUtil.class);
    public static void showAlertAndWait(String contentText) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, contentText, ButtonType.OK);
            try {
                TitleBarController.create(alert).showAlertAndWait();
            } catch (IOException ex) {
                LOGGER.error("Failed to create alert with content \"{}\"", contentText, ex);
            }
        });
    }

    public static void showAlert(Consumer<Alert> consumer) {
        Platform.runLater(() -> {
            final Alert alert = new Alert(Alert.AlertType.ERROR, "", ButtonType.OK);
            alert.setResizable(false);
            consumer.accept(alert);

            try {
                TitleBarController.create(alert).showAlert();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
