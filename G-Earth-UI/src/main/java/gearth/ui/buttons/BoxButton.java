package gearth.ui.buttons;

import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

import java.io.File;

public class BoxButton extends StackPane {

    private ImageView imageView;
    private Image image;
    private Image imageOnHover;
    private boolean isVisible;

    //paths zijn relatief aan deze classpath
    public BoxButton(String imageName, String imageOnHoverName) {
        this.image = new Image(getClass().getResourceAsStream("files/" + imageName));
        this.imageOnHover = new Image(getClass().getResourceAsStream("files/" + imageOnHoverName));
        this.imageView = new ImageView();

        setCursor(Cursor.DEFAULT);
        getChildren().add(imageView);
        setOnMouseEntered(onMouseHover);
        setOnMouseExited(onMouseHoverDone);
    }

    public void show()  {
        imageView.setImage(image);
        isVisible = true;
    }

    public void dispose()  {
        imageView.setImage(null);
        isVisible = false;
    }

    private EventHandler<MouseEvent> onMouseHover =
            t -> {
                if (isVisible) {
                    imageView.setImage(imageOnHover);
                }
            };

    private EventHandler<MouseEvent> onMouseHoverDone =
            t -> {
                if (isVisible) {
                    imageView.setImage(image);
                }
            };

}
