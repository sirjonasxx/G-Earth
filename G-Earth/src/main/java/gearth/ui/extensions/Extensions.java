package gearth.ui.extensions;

import gearth.services.extensionserver.ExtensionServer;
import gearth.services.extensionserver.extensions.extensionproducers.ExtensionProducer;
import gearth.services.extensionserver.extensions.network.NetworkExtensionsProducer;
import gearth.services.extensionserver.extensions.network.executer.ExecutionInfo;
import gearth.services.extensionserver.extensions.network.executer.ExtensionRunner;
import gearth.services.extensionserver.extensions.network.executer.ExtensionRunnerFactory;
import gearth.ui.SubForm;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.function.Predicate;

/**
 * Created by Jonas on 06/04/18.
 */

public class Extensions extends SubForm {


    public Button btn_install;
    public Button btn_remove;
    public TextField ext_port;
    public VBox extensioncontainer;
    public GridPane header_ext;
    public ScrollPane scroller;

    private ExtensionRunner extensionRunner = null;
    private ExtensionServer extensionServer;
    private NetworkExtensionsProducer networkExtensionsProducer; // needed for port


    public void initialize() {
        scroller.widthProperty().addListener(observable -> header_ext.setPrefWidth(scroller.getWidth()));
    }

    protected void onParentSet() {
        ExtensionItemContainerProducer producer = new ExtensionItemContainerProducer(extensioncontainer, scroller);
        extensionServer = new ExtensionServer(getHConnection());
        extensionServer.onExtensionConnected((e -> {
            Platform.runLater(() -> producer.extensionConnected(e));
        }));

        //noinspection OptionalGetWithoutIsPresent
        networkExtensionsProducer
                = (NetworkExtensionsProducer) extensionServer.getExtensionProducers().stream()
                .filter(producer1 -> producer1 instanceof NetworkExtensionsProducer)
                .findFirst().get();


        producer.setPort(networkExtensionsProducer.getPort());
        ext_port.setText(networkExtensionsProducer.getPort()+"");
//        System.out.println("Extension server registered on port: " + extensionsRegistrer.getPort());

        extensionRunner = ExtensionRunnerFactory.get();
        extensionRunner.runAllExtensions(networkExtensionsProducer.getPort());
    }


    public void installBtnClicked(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Install extension");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("G-Earth extensions", ExecutionInfo.ALLOWEDEXTENSIONTYPES));
        File selectedFile = fileChooser.showOpenDialog(parentController.getStage());
        if (selectedFile != null) {
            extensionRunner.installAndRunExtension(selectedFile.getPath(), networkExtensionsProducer.getPort());
        }
    }
}
