package gearth.ui.subforms.extensions;

import gearth.services.extension_handler.ExtensionHandler;
import gearth.services.extension_handler.extensions.ExtensionListener;
import gearth.services.extension_handler.extensions.implementations.network.NetworkExtensionServer;
import gearth.services.extension_handler.extensions.implementations.network.NetworkExtensionAuthenticator;
import gearth.services.extension_handler.extensions.implementations.network.executer.ExecutionInfo;
import gearth.services.extension_handler.extensions.implementations.network.executer.ExtensionRunner;
import gearth.services.extension_handler.extensions.implementations.network.executer.ExtensionRunnerFactory;
import gearth.services.g_python.GPythonShell;
import gearth.ui.SubForm;
import gearth.ui.subforms.extensions.logger.ExtensionLogger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * Created by Jonas on 06/04/18.
 */

public class ExtensionsController extends SubForm {


    public Button btn_install;
    public TextField ext_port;
    public VBox extensioncontainer;
    public GridPane header_ext;
    public ScrollPane scroller;
    public Button btn_viewExtensionConsole;
    public Button btn_gpython;

    private ExtensionRunner extensionRunner = null;
    private ExtensionHandler extensionHandler;
    private NetworkExtensionServer networkExtensionsProducer; // needed for port
    private ExtensionLogger extensionLogger = null;




    public void initialize() {
        scroller.widthProperty().addListener(observable -> header_ext.setPrefWidth(scroller.getWidth()));
        extensionLogger = new ExtensionLogger();
    }

    protected void onParentSet() {
        ExtensionItemContainerProducer producer = new ExtensionItemContainerProducer(extensioncontainer, scroller);
        extensionHandler = new ExtensionHandler(getHConnection());
        extensionHandler.getObservable().addListener((e -> {
            Platform.runLater(() -> producer.extensionConnected(e));
        }));

        //noinspection OptionalGetWithoutIsPresent
        networkExtensionsProducer
                = (NetworkExtensionServer) extensionHandler.getExtensionProducers().stream()
                .filter(producer1 -> producer1 instanceof NetworkExtensionServer)
                .findFirst().get();


        producer.setPort(networkExtensionsProducer.getPort());
        ext_port.setText(networkExtensionsProducer.getPort()+"");
//        System.out.println("Extension server registered on port: " + extensionsRegistrer.getPort());

        extensionRunner = ExtensionRunnerFactory.get();
        extensionRunner.runAllExtensions(networkExtensionsProducer.getPort());


        extensionHandler.getObservable().addListener(e -> e.getExtensionObservable().addListener(new ExtensionListener() {
            @Override
            protected void log(String text) {
                extensionLogger.log(text);
            }
        }));
    }


    public void installBtnClicked(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Install extension");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("G-Earth extensions", ExecutionInfo.ALLOWED_EXTENSION_TYPES));
        File selectedFile = fileChooser.showOpenDialog(parentController.getStage());
        if (selectedFile != null) {
            extensionRunner.installAndRunExtension(selectedFile.getPath(), networkExtensionsProducer.getPort());
        }
    }

    public void extConsoleBtnClicked(ActionEvent actionEvent) {
        if (!extensionLogger.isVisible()) {
            extensionLogger.show();
        }
        else {
            extensionLogger.hide();
        }
    }

    @Override
    protected void onTabOpened() {
        updateGPythonStatus();
    }

    public void updateGPythonStatus() {
        if (!pythonShellLaunching) {
            btn_gpython.setDisable(!parentController.extraController.useGPython());
        }
    }


    private volatile int gpytonShellCounter = 1;
    private volatile boolean pythonShellLaunching = false;
    public void gpythonBtnClicked(ActionEvent actionEvent) {
        pythonShellLaunching = true;
        Platform.runLater(() -> btn_gpython.setDisable(true));
        GPythonShell shell = new GPythonShell(
                "Scripting shell " + gpytonShellCounter++,
                networkExtensionsProducer.getPort(),
                NetworkExtensionAuthenticator.generatePermanentCookie()
        );
        shell.launch((b) -> {
            pythonShellLaunching = false;
            Platform.runLater(this::updateGPythonStatus);
        });
    }

    public ExtensionHandler getExtensionHandler() {
        return extensionHandler;
    }
}
