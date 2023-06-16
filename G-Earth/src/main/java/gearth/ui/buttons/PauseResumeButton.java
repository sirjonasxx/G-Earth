package gearth.ui.buttons;

import javafx.beans.InvalidationListener;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by Jonas on 11/04/18.
 */
public class PauseResumeButton extends StackPane{

    private final boolean[] isPaused = {false};

    private final ImageView imageView;
    private final Image imagePause;
    private final Image imagePauseOnHover;

    private final Image imageResume;
    private final Image imageResumeOnHover;

    private volatile boolean isHovering = false;

    private final List<InvalidationListener> clickListeners = new ArrayList<>();

    public PauseResumeButton(boolean isPaused) {
        this.isPaused[0] = isPaused;

        this.imagePause = getImageResource("files/ButtonPause.png");
        this.imagePauseOnHover = getImageResource("files/ButtonPauseHover.png");
        this.imageResume = getImageResource("files/ButtonResume.png");
        this.imageResumeOnHover = getImageResource("files/ButtonResumeHover.png");
        this.imageView = new ImageView();

        setCursor(Cursor.DEFAULT);
        getChildren().add(imageView);
        setOnMouseEntered( t -> {
            imageView.setImage(isPaused() ? imageResumeOnHover : imagePauseOnHover);
            isHovering = true;
        });
        setOnMouseExited(t -> {
            imageView.setImage(isPaused() ? imageResume : imagePause);
            isHovering = false;
        });

        imageView.setImage(isPaused() ? imageResume : imagePause);

        setEventHandler(MouseEvent.MOUSE_CLICKED, event -> click());
    }

    private Image getImageResource(String name) {
        return new Image(Objects.requireNonNull(getClass().getResourceAsStream(name)));
    }

    public boolean isPaused() {
        return isPaused[0];
    }

    public void onClick(InvalidationListener listener) {
        clickListeners.add(listener);
    }

    public void setPaused(boolean paused) {
        isPaused[0] = paused;
        imageView.setImage(isPaused() ?
                (isHovering ? imageResumeOnHover : imageResume) :
                (isHovering ? imagePauseOnHover : imagePause)
        );
    }

    private void click() {
        for (int i = clickListeners.size() - 1; i >= 0; i--) {
            clickListeners.get(i).invalidated(null);
        }
    }
}
