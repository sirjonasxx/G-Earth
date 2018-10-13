package g_earth.extensions.examples.blockreplacepackets;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import g_earth.extensions.ExtensionForm;
import g_earth.extensions.ExtensionInfo;
import g_earth.ui.GEarthController;

/**
 * Created by Jonas on 22/09/18.
 */


@ExtensionInfo(
        Title = "iManipulate",
        Description = "Block &/ replace packets",
        Version = "0.1",
        Author = "sirjonasxx"
)
public class BlockAndReplacePackets extends ExtensionForm {

    public TextField txt_replacement;
    public ComboBox<String> cmb_type;
    public TextField txt_id;
    public Button btn_add;

    public static void main(String[] args) {
        ExtensionForm.args = args;
        launch(args);
    }

    //initialize javaFX elements
    public void initialize() {
        cmb_type.getItems().addAll("Block OUT", "Block IN", "Replace OUT", "Replace IN");
        cmb_type.getSelectionModel().selectFirst();
    }

    @Override
    protected void initExtension() {

    }

    @Override
    public void setStageData(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(BlockAndReplacePackets.class.getResource("blockreplace.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Packet blocker &/ replacer");
        primaryStage.setScene(new Scene(root, 580, 262));
        primaryStage.getScene().getStylesheets().add(GEarthController.class.getResource("bootstrap3.css").toExternalForm());
    }
}
