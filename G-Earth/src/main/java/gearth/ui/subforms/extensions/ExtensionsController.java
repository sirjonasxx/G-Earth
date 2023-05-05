package gearth.ui.subforms.extensions;

import gearth.services.extension_handler.ExtensionHandler;
import gearth.services.extension_handler.extensions.ExtensionListener;
import gearth.services.extension_handler.extensions.implementations.network.NetworkExtensionAuthenticator;
import gearth.services.extension_handler.extensions.implementations.network.NetworkExtensionServer;
import gearth.services.extension_handler.extensions.implementations.network.executer.ExecutionInfo;
import gearth.services.extension_handler.extensions.implementations.network.executer.ExtensionRunner;
import gearth.services.extension_handler.extensions.implementations.network.executer.ExtensionRunnerFactory;
import gearth.services.g_python.GPythonShell;
import gearth.ui.GEarthProperties;
import gearth.ui.SubForm;
import gearth.ui.subforms.extensions.logger.ExtensionLogger;
import gearth.ui.translations.LanguageBundle;
import gearth.ui.translations.TranslatableString;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Jonas on 06/04/18.
 */
public class ExtensionsController extends SubForm implements Initializable {

    public Button installButton;
    public TextField portField;

    public GridPane headerGridPane;
    public ScrollPane contentScrollPane;
    public VBox contentBox;

    public Button viewLogsButton;
    public Button openGPythonShellButton;

    public Label
            tableTitleLabel,
            tableDescriptionLabel,
            tableAuthorLabel,
            tableVersionLabel,
            tableEditLabel,
            portLabel;

    private ExtensionRunner extensionRunner = null;
    private ExtensionHandler extensionHandler;
    private NetworkExtensionServer networkExtensionsProducer; // needed for port
    private ExtensionLogger extensionLogger = null;

    private final AtomicInteger gpytonShellCounter = new AtomicInteger(1);
    private final AtomicBoolean pythonShellLaunching = new AtomicBoolean(false);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        extensionLogger = new ExtensionLogger();

        headerGridPane.prefWidthProperty().bind(contentScrollPane.widthProperty());
        installButton.disableProperty().bind(GEarthProperties.enableDeveloperModeProperty.not());

        initLanguageBinding();
    }

    @Override
    protected void onParentSet() {
        ExtensionItemContainerProducer producer = new ExtensionItemContainerProducer(contentBox, contentScrollPane);
        extensionHandler = new ExtensionHandler(getHConnection());
        extensionHandler.getObservable().addListener((e -> Platform.runLater(() -> producer.extensionConnected(e))));

        //noinspection OptionalGetWithoutIsPresent
        networkExtensionsProducer
                = (NetworkExtensionServer) extensionHandler.getExtensionProducers().stream()
                .filter(producer1 -> producer1 instanceof NetworkExtensionServer)
                .findFirst().get();

        producer.setPort(networkExtensionsProducer.getPort());
        portField.setText(Integer.toString(networkExtensionsProducer.getPort()));

        extensionRunner = ExtensionRunnerFactory.get();
        extensionRunner.runAllExtensions(networkExtensionsProducer.getPort());

        extensionHandler.getObservable().addListener(e ->
                e.getExtensionObservable().addListener(new ExtensionListener() {
                    @Override
                    public void log(String text) {
                        extensionLogger.log(text);
                    }
                })
        );
    }

    @Override
    protected void onTabOpened() {
        updateGPythonStatus();
    }

    @FXML
    public void onClickInstallButton() {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(LanguageBundle.get("tab.extensions.button.install.windowtitle"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(LanguageBundle.get("tab.extensions.button.install.filetype"), ExecutionInfo.ALLOWED_EXTENSION_TYPES));
        final File selectedFile = fileChooser.showOpenDialog(parentController.getStage());
        if (selectedFile != null)
            extensionRunner.installAndRunExtension(selectedFile.getPath(), networkExtensionsProducer.getPort());
    }

    @FXML
    public void onClickLogsButton() {
        if (!extensionLogger.isVisible())
            extensionLogger.show();
        else
            extensionLogger.hide();
    }

    @FXML
    public void onClickGPythonShellButton() {
        pythonShellLaunching.set(true);
        Platform.runLater(() -> openGPythonShellButton.setDisable(true));
        GPythonShell shell = new GPythonShell(
                String.format("%s %d",
                        LanguageBundle.get("tab.extensions.button.pythonshell.windowtitle"),
                        gpytonShellCounter.getAndIncrement()
                ),
                networkExtensionsProducer.getPort(),
                NetworkExtensionAuthenticator.generatePermanentCookie()
        );
        shell.launch((b) -> {
            pythonShellLaunching.set(false);
            Platform.runLater(this::updateGPythonStatus);
        });
    }

    public void updateGPythonStatus() {
        if (!pythonShellLaunching.get())
            openGPythonShellButton.setDisable(!parentController.extraController.useGPython());
    }

    public ExtensionHandler getExtensionHandler() {
        return extensionHandler;
    }

    private void initLanguageBinding() {
        tableTitleLabel.textProperty().bind(new TranslatableString("%s", "tab.extensions.table.title"));
        tableDescriptionLabel.textProperty().bind(new TranslatableString("%s", "tab.extensions.table.description"));
        tableAuthorLabel.textProperty().bind(new TranslatableString("%s", "tab.extensions.table.author"));
        tableVersionLabel.textProperty().bind(new TranslatableString("%s", "tab.extensions.table.version"));
        tableEditLabel.textProperty().bind(new TranslatableString("%s", "tab.extensions.table.edit"));

        portLabel.textProperty().bind(new TranslatableString("%s:", "tab.extensions.port"));

        openGPythonShellButton.textProperty().bind(new TranslatableString("%s", "tab.extensions.button.pythonshell"));
        viewLogsButton.textProperty().bind(new TranslatableString("%s", "tab.extensions.button.logs"));
        installButton.textProperty().bind(new TranslatableString("%s", "tab.extensions.button.install"));
    }
}
