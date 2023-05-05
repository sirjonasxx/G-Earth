package gearth.ui.subforms.extra;

import gearth.GEarth;
import gearth.misc.BindingsUtil;
import gearth.misc.HyperLinkUtil;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.proxy.ProxyProviderFactory;
import gearth.protocol.connection.proxy.SocksConfiguration;
import gearth.services.always_admin.AdminService;
import gearth.services.g_python.GPythonVersionUtils;
import gearth.ui.GEarthProperties;
import gearth.ui.SubForm;
import gearth.ui.titlebar.TitleBarController;
import gearth.ui.translations.LanguageBundle;
import gearth.ui.translations.TranslatableString;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Created by Jonas on 06/04/18.
 * TODO: add setup link to g-earth wiki
 */
public class ExtraController extends SubForm implements SocksConfiguration, Initializable {

    public static final String INFO_URL_GPYTHON = "https://github.com/sirjonasxx/G-Earth/wiki/G-Python-qtConsole";

    public TextArea notepadTextArea;

    public CheckBox alwaysOnTopBox;
    public Hyperlink troubleshootingLink;

    public CheckBox enableClientSideStaffPermissionsBox;
    public CheckBox enableDeveloperModeBox;

    public CheckBox enableGPythonBox;
    public CheckBox enableAdvancedBox;

    public GridPane advancedPane;
    public CheckBox advancedDisableDecryptionBox;
    public CheckBox advancedEnableDebugBox;
    public CheckBox advancedUseSocksBox;
    public GridPane advancedSocksInfoGrid;
    public TextField advancedSocketProxyIpField;

    public Label notepadLabel, advancedSocksProxyIpLabel;

    private AdminService adminService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        troubleshootingLink.setTooltip(new Tooltip("https://github.com/sirjonasxx/G-Earth/wiki/Troubleshooting"));
        HyperLinkUtil.showDocumentOnClick(troubleshootingLink);

        BindingsUtil.setAndBindBiDirectional(notepadTextArea.textProperty(), GEarthProperties.notesProperty);

        advancedSocketProxyIpField.textProperty().set(GEarthProperties.getSocksHost()+":"+GEarthProperties.getSocksPort());
        GEarthProperties.socksHostProperty.bind(Bindings.createStringBinding(this::getSocksHost, advancedSocketProxyIpField.textProperty()));
        GEarthProperties.socksPortProperty.bind(Bindings.createIntegerBinding(this::getSocksPort, advancedSocketProxyIpField.textProperty()));
        advancedSocksInfoGrid.disableProperty().bind(GEarthProperties.enableSocksProperty.not());

        BindingsUtil.setAndBindBiDirectional(advancedUseSocksBox.selectedProperty(), GEarthProperties.enableSocksProperty);
        ProxyProviderFactory.setSocksConfig(this);

        BindingsUtil.setAndBindBiDirectional(advancedEnableDebugBox.selectedProperty(), GEarthProperties.enableDebugProperty);
        BindingsUtil.setAndBindBiDirectional(advancedDisableDecryptionBox.selectedProperty(), GEarthProperties.disablePacketDecryptionProperty);
        BindingsUtil.setAndBindBiDirectional(alwaysOnTopBox.selectedProperty(), GEarthProperties.alwaysOnTopProperty);
        BindingsUtil.setAndBindBiDirectional(enableDeveloperModeBox.selectedProperty(), GEarthProperties.enableDeveloperModeProperty);
        BindingsUtil.setAndBindBiDirectional(enableClientSideStaffPermissionsBox.selectedProperty(), GEarthProperties.alwaysAdminProperty);
        BindingsUtil.setAndBindBiDirectional(enableGPythonBox.selectedProperty(), GEarthProperties.enableGPythonProperty);

        initLanguageBinding();
    }

    @Override
    protected void onParentSet() {
        adminService = new AdminService(enableClientSideStaffPermissionsBox.isSelected(), getHConnection());
        getHConnection().addTrafficListener(1, message -> adminService.onMessage(message));
        getHConnection().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == HState.CONNECTED)
                adminService.onConnect();
            if (oldValue == HState.NOT_CONNECTED || newValue == HState.NOT_CONNECTED)
                updateAdvancedUI();
        });
        enableAdvancedBox.selectedProperty().addListener(observable -> updateAdvancedUI());
        updateAdvancedUI();
    }

    @Override
    public boolean useSocks() {
        return advancedUseSocksBox.isSelected();
    }

    @Override
    public int getSocksPort() {
        String socksString = advancedSocketProxyIpField.getText();
        if (socksString.contains(":")) {
            return Integer.parseInt(socksString.split(":")[1]);
        }
        return 1337;
    }

    @Override
    public String getSocksHost() {
        return advancedSocketProxyIpField.getText().split(":")[0];
    }

    @Override
    public boolean onlyUseIfNeeded() {
//        return cbx_socksUseIfNeeded.isSelected();
        return false;
    }
    @FXML
    public void onClickGPythonButton() {
        if (enableGPythonBox.isSelected()) {
            new Thread(() -> {
                Platform.runLater(() -> {
                    enableGPythonBox.setSelected(false);
                    enableGPythonBox.setDisable(true);
                });
                if (!GPythonVersionUtils.validInstallation()) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR, LanguageBundle.get("tab.extra.options.pythonscripting.alert.title"), ButtonType.OK);
                        alert.setTitle(LanguageBundle.get("tab.extra.options.pythonscripting.alert.title"));

                        FlowPane fp = new FlowPane();
                        Label lbl = new Label(LanguageBundle.get("tab.extra.options.pythonscripting.alert.content") +
                                System.lineSeparator() + System.lineSeparator() +
                                LanguageBundle.get("tab.extra.options.pythonscripting.alert.moreinformation"));
                        Hyperlink link = new Hyperlink(INFO_URL_GPYTHON);
                        fp.getChildren().addAll( lbl, link);
                        link.setOnAction(event -> {
                            GEarth.main.getHostServices().showDocument(link.getText());
                            event.consume();
                        });

                        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                        alert.getDialogPane().setContent(fp);
                        try {
                            TitleBarController.create(alert).showAlert();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        enableGPythonBox.setDisable(false);
                    });
                }
                else {
                    Platform.runLater(() -> {
                        enableGPythonBox.setSelected(true);
                        enableGPythonBox.setDisable(false);
                        parentController.extensionsController.updateGPythonStatus();
                    });
                }
            }).start();


        }

    }

    @FXML
    public void onClickDeveloperModeBox() {
        if (enableDeveloperModeBox.isSelected()) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING, LanguageBundle.get("tab.extra.options.developmode.alert.title"), ButtonType.NO, ButtonType.YES);
                alert.setTitle(LanguageBundle.get("tab.extra.options.developmode.alert.title"));

                Label lbl = new Label(LanguageBundle.get("tab.extra.options.developmode.alert.content"));

                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                alert.getDialogPane().setContent(lbl);

                try {
                    Optional<ButtonType> result = TitleBarController.create(alert).showAlertAndWait();
                    if (!result.isPresent() || result.get() == ButtonType.NO) {
                        enableDeveloperModeBox.setSelected(false);
                    }
                    else {
                        setDevelopMode(true);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        else {
            setDevelopMode(false);
        }
    }

    @FXML
    public void onClickClientSideStaffPermissionsBox() {
        adminService.setEnabled(enableClientSideStaffPermissionsBox.isSelected());
    }

    public boolean useGPython() {
        return enableGPythonBox.isSelected();
    }

    private void setDevelopMode(boolean enabled) {
        enableDeveloperModeBox.setSelected(enabled);
    }

    private void updateAdvancedUI() {
        if (!enableAdvancedBox.isSelected()) {
            advancedEnableDebugBox.setSelected(false);
            advancedUseSocksBox.setSelected(false);
            if (getHConnection().getState() == HState.NOT_CONNECTED) {
                advancedDisableDecryptionBox.setSelected(false);
            }
        }
        advancedPane.setDisable(!enableAdvancedBox.isSelected());

        advancedDisableDecryptionBox.setDisable(getHConnection().getState() != HState.NOT_CONNECTED);
    }

    private void initLanguageBinding() {
        troubleshootingLink.textProperty().bind(new TranslatableString("%s", "tab.extra.troubleshooting"));

        notepadLabel.textProperty().bind(new TranslatableString("%s:", "tab.extra.notepad"));
        advancedSocksProxyIpLabel.textProperty().bind(new TranslatableString("%s:", "tab.extra.options.advanced.proxy.ip"));

        alwaysOnTopBox.textProperty().bind(new TranslatableString("%s", "tab.extra.options.alwaysontop"));

        enableDeveloperModeBox.textProperty().bind(new TranslatableString("%s", "tab.extra.options.developmode"));
        enableClientSideStaffPermissionsBox.textProperty().bind(new TranslatableString("%s", "tab.extra.options.staffpermissions"));
        enableGPythonBox.textProperty().bind(new TranslatableString("%s", "tab.extra.options.pythonscripting"));
        enableAdvancedBox.textProperty().bind(new TranslatableString("%s", "tab.extra.options.advanced"));

        advancedUseSocksBox.textProperty().bind(new TranslatableString("%s", "tab.extra.options.advanced.socks"));
        advancedDisableDecryptionBox.textProperty().bind(new TranslatableString("%s", "tab.extra.options.advanced.disabledecryption"));
        advancedEnableDebugBox.textProperty().bind(new TranslatableString("%s", "tab.extra.options.advanced.debugstdout"));
    }
}
