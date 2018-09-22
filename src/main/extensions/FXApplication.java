package main.extensions;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Created by Jonas on 22/09/18.
 */
public class FXApplication extends Application {

    public interface InitStage {
        void callback(Stage primaryStage, Parent root);
    }
    private InitStage initStage;
    private URL layoutLocation;
    private boolean[] isOpen = {false};

    public FXApplication(URL layoutLocation, InitStage initStage) {
        super();

        this.layoutLocation = layoutLocation;
        this.initStage = initStage;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(layoutLocation);
        Parent root = loader.load();

        initStage.callback(primaryStage, root);
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> isOpen[0] = false);
        isOpen[0] = true;
    }

    @Override
    public void stop() throws Exception {
        super.stop();

        isOpen[0] = false;
    }

    public boolean isOpen() {
        return isOpen[0];
    }

    public void open() {
        if (!isOpen()) {
            launch();
        }
    }

}
