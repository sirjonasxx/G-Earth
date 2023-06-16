package gearth.ui.buttons;

import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.util.Objects;

public class BoxButton extends StackPane {

    private final ImageView imageView;
    private final Image image;
    private final Image imageOnHover;
    private boolean isVisible;

    /**
     * Creates a new box button.
     *
     * @param imageName        name of the image file in src/main/java/gearth/ui/buttons/files/
     * @param imageOnHoverName name of the image file in src/main/java/gearth/ui/buttons/files/
     */
    public BoxButton(String imageName, String imageOnHoverName) {
        this.image = getImageResource(imageName);
        this.imageOnHover = getImageResource(imageOnHoverName);
        this.imageView = new ImageView();
        setCursor(Cursor.DEFAULT);
        getChildren().add(imageView);
        setOnMouseEntered(t -> {
            if (isVisible) imageView.setImage(imageOnHover);
        });
        setOnMouseExited(t -> {
            if (isVisible) imageView.setImage(image);
        });
    }

    private Image getImageResource(String imageName) {
        return new Image(Objects.requireNonNull(getClass().getResourceAsStream("files/" + imageName)));
    }

    public void show() {
        imageView.setImage(image);
        isVisible = true;
    }

    public void dispose() {
        imageView.setImage(null);
        isVisible = false;
    }
}
