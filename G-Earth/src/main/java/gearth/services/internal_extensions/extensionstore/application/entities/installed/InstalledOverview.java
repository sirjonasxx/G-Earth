package gearth.services.internal_extensions.extensionstore.application.entities.installed;

import gearth.services.internal_extensions.extensionstore.GExtensionStore;
import gearth.services.internal_extensions.extensionstore.application.entities.ContentItem;
import gearth.services.internal_extensions.extensionstore.application.entities.HOverview;
import gearth.services.internal_extensions.extensionstore.repository.StoreRepository;
import gearth.services.internal_extensions.extensionstore.repository.models.StoreExtension;
import gearth.services.internal_extensions.extensionstore.tools.InstalledExtension;
import gearth.services.internal_extensions.extensionstore.tools.StoreExtensionTools;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InstalledOverview extends HOverview {

    private final StoreRepository storeRepository;

    public InstalledOverview(HOverview parent, int startIndex, int size, StoreRepository storeRepository) {
        super(parent, startIndex, size);
        this.storeRepository = storeRepository;
    }


    @Override
    public String buttonText() {
        return "Open folder";
    }

    @Override
    public boolean buttonEnabled() {
        return true;
    }

    @Override
    public List<? extends ContentItem> getContentItems() {
        List<InstalledExtension> installed = StoreExtensionTools.getInstalledExtension();
        installed.sort(Comparator.comparing(o -> new ComparableVersion(o.getVersion())));

        installed = installed.subList(startIndex, Math.min(startIndex + limit, installed.size()));
        Map<String, StoreExtension> nameToExt = new HashMap<>();
        // getExtensions() with no filtering includes outdated extensions
        storeRepository.getExtensions().forEach(e -> nameToExt.put(e.getTitle(), e));

        return installed.stream().map(i -> new StoreExtensionInstalledItem(nameToExt.get(i.getName()), i)).collect(Collectors.toList());
    }

    @Override
    public int getMaxAmount() {
        return StoreExtensionTools.getInstalledExtension().size();
    }

    @Override
    public void buttonClick(GExtensionStore gExtensionStore) {
        try {
            Desktop.getDesktop().open(new File(StoreExtensionTools.EXTENSIONS_PATH));
        } catch (Exception e) {
//            e.printStackTrace(); // no extensions installed yet, directory isnt created
        }
    }

    @Override
    public Header header() {
        return new Header() {
            @Override
            public String iconUrl() {
                return "images/overviews/success.png";
            }

            @Override
            public String title() {
                return "Installed Extensions";
            }

            @Override
            public String description() {
                return "Extensions that are already installed into G-Earth";
            }

            @Override
            public String contentTitle() {
                return "Installed extensions";
            }
        };
    }

    @Override
    public HOverview getNewPage(int startIndex, int size) {
        return new InstalledOverview(parent, startIndex, size, storeRepository);
    }

}
