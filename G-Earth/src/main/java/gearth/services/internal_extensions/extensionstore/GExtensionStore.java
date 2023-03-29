package gearth.services.internal_extensions.extensionstore;

import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.services.internal_extensions.extensionstore.application.GExtensionStoreController;
import gearth.services.internal_extensions.extensionstore.repository.StoreFetch;
import gearth.services.internal_extensions.extensionstore.repository.StoreRepository;
import javafx.application.HostServices;

@ExtensionInfo(
        Title = "G-ExtensionStore",
        Description = "Get your extensions here",
        Version = "1.0",
        Author = "sirjonasxx"
)
public class GExtensionStore extends ExtensionForm {

    public static final int MAX_PAGES = 20;

    private StoreRepository repository = null;
    private GExtensionStoreController extensionStoreController = null;

    public GExtensionStore() {
        StoreFetch.fetch(new StoreFetch.StoreFetchListener() {
            @Override
            public void success(StoreRepository storeRepository) {
                repository = storeRepository;
                if (extensionStoreController != null) {
                    extensionStoreController.maybeInitialized();
                }
            }

            @Override
            public void fail(String reason) {
                System.out.println("failed fetching store repository");
            }
        });
    }



    public void setgExtensionStoreController(GExtensionStoreController gExtensionStoreController) {
        this.extensionStoreController = gExtensionStoreController;
        gExtensionStoreController.maybeInitialized();
    }

    public GExtensionStoreController getController() {
        return extensionStoreController;
    }

    public StoreRepository getRepository() {
        return repository;
    }

    @Override
    protected boolean canLeave() {
        return false;
    }

    @Override
    public HostServices getHostServices() {
        return super.getHostServices();
    }
}
