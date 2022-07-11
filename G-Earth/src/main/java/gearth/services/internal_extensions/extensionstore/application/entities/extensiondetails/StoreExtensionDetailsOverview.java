package gearth.services.internal_extensions.extensionstore.application.entities.extensiondetails;

import gearth.services.extension_handler.extensions.extensionproducers.ExtensionProducerFactory;
import gearth.services.internal_extensions.extensionstore.GExtensionStore;
import gearth.services.internal_extensions.extensionstore.application.GExtensionStoreController;
import gearth.services.internal_extensions.extensionstore.application.WebUtils;
import gearth.services.internal_extensions.extensionstore.application.entities.ContentItem;
import gearth.services.internal_extensions.extensionstore.application.entities.HOverview;
import gearth.services.internal_extensions.extensionstore.repository.StoreRepository;
import gearth.services.internal_extensions.extensionstore.repository.models.StoreExtension;
import gearth.services.internal_extensions.extensionstore.tools.InstalledExtension;
import gearth.services.internal_extensions.extensionstore.tools.StoreExtensionTools;
import gearth.ui.titlebar.TitleBarController;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class StoreExtensionDetailsOverview extends HOverview {


    private final StoreRepository storeRepository;

    private final StoreExtension extension;
    private final Optional<InstalledExtension> installedExtension;


    public StoreExtensionDetailsOverview(HOverview parent, int startIndex, int limit, StoreExtension extension, StoreRepository storeRepository) {
        super(parent, startIndex, limit);
        this.extension = extension;
        this.storeRepository = storeRepository;

        List<InstalledExtension> installed = StoreExtensionTools.getInstalledExtension();
        // assure highest version comes first
        installed.sort((o1, o2) -> new ComparableVersion(o2.getVersion()).compareTo(new ComparableVersion(o1.getVersion())));
        installedExtension = installed.stream().filter(i -> i.getName().equals(extension.getTitle())).findFirst();
    }

    // 0 = not installed
    // 1 = installed
    // 2 = needs update
    private int mode() {
        if(installedExtension.isPresent()) {
            InstalledExtension i = installedExtension.get();
            if (new ComparableVersion(i.getVersion()).compareTo(new ComparableVersion(extension.getVersion())) < 0) {
                return 2;
            }
            return 1;
        }
        return 0;
    }

    @Override
    public String buttonText() {
        int mode = mode();
//        return mode == 2 ? "Update" : "Install";
        return mode == 0 ? "Install" : (mode == 1 ? "Installed" : "Update");
    }

    @Override
    public boolean buttonEnabled() {
        return mode() != 1;
    }

    @Override
    public List<? extends ContentItem> getContentItems() {
        return Collections.singletonList(new StoreExtensionDetailsItem(extension));
    }

    @Override
    public int getMaxAmount() {
        return 1;
    }


    private void setButtonEnable(GExtensionStore gExtensionStore, boolean enabled) {
        GExtensionStoreController c = gExtensionStore.getController();
        Element generic_btn  = c.getWebView().getEngine().getDocument().getElementById("generic_btn");

        WebUtils.removeClass((Element) generic_btn.getParentNode(), "gdisabled");
        if (!enabled) WebUtils.addClass((Element) generic_btn.getParentNode(), "gdisabled");
    }

    private void awaitPopup(String mode) {
        popup(Alert.AlertType.WARNING,
                String.format("%s extension", mode),
                String.format("%s extension [%s]", mode, extension.getTitle()),
                String.format("Press \"OK\" and wait while the extension is being %sed", mode.toLowerCase()));
    }

    private void successPopup(String mode) {
        popup(Alert.AlertType.INFORMATION,
                String.format("%s completed", mode),
                String.format("%s completed [%s]", mode, extension.getTitle()),
                String.format("Extension %s completed successfully", mode.toLowerCase()));
    }

    private void errorPopup(String mode, String error) {
        popup(Alert.AlertType.ERROR,
                String.format("%s failed", mode),
                String.format("%s failed [%s]", mode, extension.getTitle()),
                String.format("%s failed with the following message: %s", mode, error));
    }

    private void popup(Alert.AlertType alertType, String title, String header, String context) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(context);

        try {
            TitleBarController.create(alert).showAlertAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void buttonClick(GExtensionStore gExtensionStore) {
        int mode = mode();
        if (mode == 1) return;

        String modeString = mode() == 0 ? "Install" : "Update";
        HOverview selff = this;

        StoreExtensionTools.InstallExtListener listener =  new StoreExtensionTools.InstallExtListener() {
            @Override
            public void success(String installationFolder) {
                Platform.runLater(() -> successPopup(modeString));
                StoreExtensionTools.executeExtension(installationFolder, ExtensionProducerFactory.getExtensionServer().getPort());
            }

            @Override
            public void fail(String reason) {
                Platform.runLater(() -> {
                    errorPopup(modeString, reason);
                    if (gExtensionStore.getController().getCurrentOverview() == selff) {
                        setButtonEnable(gExtensionStore, true);
                    }
                });

            }
        };

        setButtonEnable(gExtensionStore, false);
        awaitPopup(modeString);
        if (mode() == 0) {
            StoreExtensionTools.installExtension(extension.getTitle(), storeRepository, listener);
        }
        else if (mode() == 2) {
            StoreExtensionTools.updateExtension(extension.getTitle(), storeRepository, listener);
        }
    }

    @Override
    public Header header() {
        return new Header() {
            @Override
            public String iconUrl() {
                return storeRepository.getResourceUrl("store", "extensions", extension.getTitle(), "icon.png");
            }

            @Override
            public String title() {
                return extension.getTitle();
            }

            @Override
            public String description() {
                return extension.getDescription();
            }

            @Override
            public String contentTitle() {
                return extension.getTitle();
            }
        };
    }

    @Override
    public HOverview getNewPage(int startIndex, int size) {
        return null; // impossible
    }
}
