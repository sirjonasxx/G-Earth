package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import main.ui.GEarthController;

import java.util.Arrays;

// run as root issue Invalid MIT-MAGIC-COOKIE-1 key fix: https://stackoverflow.com/questions/48139447/invalid-mit-magic-cookie-1-key

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(GEarthController.class.getResource("G-Earth.fxml"));
        Parent root = loader.load();

        GEarthController companion = loader.getController();
        companion.setStage(primaryStage);

        primaryStage.setResizable(false);
        //primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle("G-Earth");
        primaryStage.setScene(new Scene(root, 620, 295));
        primaryStage.show();
        primaryStage.getScene().getStylesheets().add(GEarthController.class.getResource("bootstrap3.css").toExternalForm());

        primaryStage.setOnCloseRequest( event -> {
            companion.abort();
        });

    }

    public static String[] args;

    public static void main(String[] args) {
        Main.args = args;
        launch(args);
    }
}
