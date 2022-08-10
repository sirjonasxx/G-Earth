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

/**
 * Created by Jonas on 11/04/18.
 */
public class PauseResumeButton extends StackPane{

    private boolean isPaused[] = {false};


    private ImageView imageView;
    private Image imagePause;
    private Image imagePauseOnHover;

    private Image imageResume;
    private Image imageResumeOnHover;

    private volatile boolean isHovering = false;

    private List<InvalidationListener> clickListeners = new ArrayList<>();

    public PauseResumeButton(boolean isPaused) {
        this.isPaused[0] = isPaused;

        this.imagePause = new Image(getClass().getResourceAsStream("files/ButtonPause.png"));
        this.imagePauseOnHover = new Image(getClass().getResourceAsStream("files/ButtonPauseHover.png"));
        this.imageResume = new Image(getClass().getResourceAsStream("files/ButtonResume.png"));
        this.imageResumeOnHover = new Image(getClass().getResourceAsStream("files/ButtonResumeHover.png"));
        this.imageView = new ImageView();

        setCursor(Cursor.DEFAULT);
        getChildren().add(imageView);
        setOnMouseEntered(onMouseHover);
        setOnMouseExited(onMouseHoverDone);

        imageView.setImage(isPaused() ? imageResume : imagePause);

        setEventHandler(MouseEvent.MOUSE_CLICKED, event -> click());
    }

    public boolean isPaused() {
        return isPaused[0];
    }

    public void onClick(InvalidationListener listener) {
        clickListeners.add(listener);
    }


    private EventHandler<MouseEvent> onMouseHover =
            t -> {
        imageView.setImage(isPaused() ? imageResumeOnHover : imagePauseOnHover);
        isHovering = true;
            };

    private EventHandler<MouseEvent> onMouseHoverDone =
            t -> {
        imageView.setImage(isPaused() ? imageResume : imagePause);
        isHovering = false;
            };


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

//    private ImageView imageView;
//    private Image image;
//    private Image imageOnHover;
//    private boolean isVisible;
//
//    //paths zijn relatief aan deze classpath
//    public BoxButton(String imagePath, String imageOnHoverPath) {
//        this.image = new Image(getClass().getResourceAsStream(imagePath));
//        this.imageOnHover = new Image(getClass().getResourceAsStream(imageOnHoverPath));
//        this.imageView = new ImageView();
//
//        setCursor(Cursor.DEFAULT);
//        getChildren().add(imageView);
//        setOnMouseEntered(onMouseHover);
//        setOnMouseExited(onMouseHoverDone);
//    }

}
