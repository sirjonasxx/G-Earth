package extensions.blockreplacepackets;

import gearth.extensions.Extension;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.ui.GEarthController;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import javafx.stage.WindowEvent;

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
    public Button btn_add;
    public volatile ComboBox<String> cmb_side;
    public TextField txt_value;

    public static void main(String[] args) {
        ExtensionForm.args = args;
        launch(args);
    }

    //initialize javaFX elements
    public void initialize() {
        cmb_type.getItems().addAll("Block packet", "Replace packet", "Replace integer", "Replace string", "Replace substring");
        cmb_type.getSelectionModel().selectFirst();

        cmb_side.getItems().addAll("Incoming", "Outgoing");
        cmb_side.getSelectionModel().selectFirst();

        cmb_side.getSelectionModel().selectedItemProperty().addListener(observable -> Platform.runLater(this::refreshOptions));
        cmb_type.getSelectionModel().selectedItemProperty().addListener(observable -> Platform.runLater(this::refreshOptions));
        txt_replacement.textProperty().addListener(event -> Platform.runLater(this::refreshOptions));
        txt_value.textProperty().addListener(event -> Platform.runLater(this::refreshOptions));

        refreshOptions();
        cmb_type.requestFocus();

    }

    private void refreshOptions() {
        txt_replacement.setDisable(cmb_type.getSelectionModel().getSelectedItem().startsWith("Block"));
        if (cmb_side.getItems().size() == 2 && !cmb_type.getSelectionModel().getSelectedItem().endsWith("packet")) {
            cmb_side.getItems().add("All");
        }
        else if (cmb_side.getItems().size() == 3 && cmb_type.getSelectionModel().getSelectedItem().endsWith("packet")) {
            if (cmb_side.getSelectionModel().getSelectedItem() != null && cmb_side.getSelectionModel().getSelectedItem().equals("All")) {
                cmb_side.getSelectionModel().selectFirst();
            }
            cmb_side.getItems().remove(2);
        }

        boolean isValid = false;
        String val = txt_value.getText();
        String repl = txt_replacement.getText();
        String type = cmb_type.getSelectionModel().getSelectedItem();
        String side = cmb_side.getSelectionModel().getSelectedItem();

        if (side == null) {
            isValid = false;
        }
        else if (type.equals("Block packet")) {
            try {
                int v = Integer.parseInt(val);
                isValid = (v < (Short.MAX_VALUE * 2 + 2) && v > 0);
            }
            catch (Exception e) {
                isValid = false;
            }
        }
        else {
            if (type.endsWith("packet")) {
                try {
                    int v = Integer.parseInt(val);
                    isValid = (v < (Short.MAX_VALUE * 2 + 2) && v > 0);
                    if (isValid) {
                        HPacket packet = new HPacket(repl);
                        isValid = !packet.isCorrupted();
                    }
                }
                catch (Exception e) {
                    isValid = false;
                }
            }
            else if (type.endsWith("string")) {
                isValid = !val.equals("") && !repl.equals("") && !val.equals(repl);
            }
            else if (type.endsWith("integer")) {
                try {
                    int v1 = Integer.parseInt(val);
                    int v2 = Integer.parseInt(repl);
                    isValid = (v1 != v2);
                }
                catch (Exception e) {
                    isValid = false;
                }
            }
        }

        btn_add.setDisable(!isValid);

        String[] spl = type.split(" ");
        if (repl.equals("") && spl[0].equals("Replace")) {
            if (spl[1].equals("packet")) {
                txt_replacement.setPromptText("Enter a packet here");
            }
            else if (spl[1].equals("integer")) {
                txt_replacement.setPromptText("Enter an integer here");
            }
            else if (spl[1].endsWith("string")) {
                txt_replacement.setPromptText("Enter a string here");
            }
        }
        else {
            txt_replacement.setPromptText("");
        }

        if (val.equals("")) {
            if (spl[1].equals("packet")) {
                txt_value.setPromptText("Enter the headerID");
            }
            else if (spl[1].equals("integer")) {
                txt_value.setPromptText("Enter an integer");
            }
            else if (spl[1].endsWith("string")) {
                txt_value.setPromptText("Enter a string");
            }
        }
        else {
            txt_value.setPromptText("");
        }
    }

    @Override
    protected void initExtension() {
        intercept(HMessage.Side.TOSERVER, message -> System.out.println("just testing"));
    }

    @Override
    public ExtensionForm launchForm(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(BlockAndReplacePackets.class.getResource("blockreplace.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Packet blocker &/ replacer");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.getScene().getStylesheets().add(GEarthController.class.getResource("/gearth/ui/bootstrap3.css").toExternalForm());

        return loader.getController();
    }

    @Override
    protected void onShow() {
        Platform.runLater(() -> cmb_type.requestFocus());
    }

    public void click_btnAddRule(ActionEvent actionEvent) {

    }
}
