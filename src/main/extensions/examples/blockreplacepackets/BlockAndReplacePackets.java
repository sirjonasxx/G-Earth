package main.extensions.examples.blockreplacepackets;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import main.extensions.ExtensionForm;
import main.ui.GEarthController;

import java.net.URL;

/**
 * Created by Jonas on 22/09/18.
 */
public class BlockAndReplacePackets extends ExtensionForm {

    public static void main(String[] args) {
        ExtensionForm.args = args;
        launch(args);
    }

    @Override
    protected void initExtension() {

    }

    @Override
    public void setStageData(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(BlockAndReplacePackets.class.getResource("blockreplace.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Packet blocker and replacer");
        primaryStage.setScene(new Scene(root, 565, 262));
    }

    protected String getTitle() {
        return "iManipulate";
    }
    protected String getDescription() {
        return "Block &/ replace packets";
    }
    protected String getVersion() {
        return "0.1";
    }
    protected String getAuthor() {
        return "sirjonasxx";
    }
}
