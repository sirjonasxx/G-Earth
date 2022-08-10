package gearth.services.internal_extensions.extensionstore.application;

import gearth.services.internal_extensions.extensionstore.GExtensionStore;
import gearth.services.internal_extensions.extensionstore.application.entities.ContentItem;
import gearth.services.internal_extensions.extensionstore.application.entities.HOverview;
import gearth.services.internal_extensions.extensionstore.application.entities.categories.CategoryOverview;
import gearth.services.internal_extensions.extensionstore.application.entities.installed.InstalledOverview;
import gearth.services.internal_extensions.extensionstore.application.entities.queriedoverviews.ByDateOverview;
import gearth.services.internal_extensions.extensionstore.application.entities.queriedoverviews.ByRatingOverview;
import gearth.services.internal_extensions.extensionstore.application.entities.queriedoverviews.ByUpdateOverview;
import gearth.services.internal_extensions.extensionstore.application.entities.search.SearchOverview;
import gearth.services.internal_extensions.extensionstore.repository.StoreRepository;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.w3c.dom.Element;
import org.w3c.dom.events.EventTarget;

import java.net.URL;
import java.util.*;
import java.util.function.Supplier;

public class GExtensionStoreController implements Initializable {
    private static GExtensionStoreController instance;

    private GExtensionStore extensionStore = null;

    private volatile boolean UIInitialized = false;
    private volatile boolean initialized = false;

    public BorderPane borderPane;
    private WebView webView;

    private LinkedList<HOverview> currentOverviews = new LinkedList<>();
    private final String contentItemsContainer = "content_items_container";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;

        webView = new WebView();
        borderPane.setCenter(webView);

        webView.getEngine().getLoadWorker().stateProperty().addListener((observableValue, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                webView.getEngine().setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:54.0) Gecko/20100101 Firefox/54.0");

                JSObject window = (JSObject) webView.getEngine().executeScript("window");
                window.setMember("app", extensionStore);

                Element by_update_link = webView.getEngine().getDocument().getElementById("overview_by_update");
                Element by_rating_link = webView.getEngine().getDocument().getElementById("overview_by_rating");
                Element by_category_link = webView.getEngine().getDocument().getElementById("overview_by_category");
                Element installed_link = webView.getEngine().getDocument().getElementById("overview_installed");
                Element seach_link = webView.getEngine().getDocument().getElementById("search_page");

                Map<Element, Supplier<HOverview>> hOverviewSupplier = new HashMap<>();
                hOverviewSupplier.put(by_update_link, () -> new ByUpdateOverview(null, 0, GExtensionStore.PAGESIZE, getStoreRepository()));
                hOverviewSupplier.put(by_rating_link, () -> new ByRatingOverview(null, 0, GExtensionStore.PAGESIZE, getStoreRepository()));
                hOverviewSupplier.put(by_category_link, () -> new CategoryOverview(null, 0, GExtensionStore.PAGESIZE, getStoreRepository()));
                hOverviewSupplier.put(installed_link, () -> new InstalledOverview(null, 0, GExtensionStore.PAGESIZE, getStoreRepository()));
                hOverviewSupplier.put(seach_link, () -> new SearchOverview(null, getStoreRepository()));

                Arrays.asList(by_update_link, by_rating_link, by_category_link, installed_link, seach_link).forEach(l ->
                    ((EventTarget) l).addEventListener("click", event -> {
                        if (initialized) setRootOverview(hOverviewSupplier.get(l).get());
                    }, true));


                Element first_btn = webView.getEngine().getDocument().getElementById("first_btn");
                Element prev_btn = webView.getEngine().getDocument().getElementById("prev_btn");
                Element next_btn = webView.getEngine().getDocument().getElementById("next_btn");
                Element last_btn = webView.getEngine().getDocument().getElementById("last_btn");

                Map<Element, Supplier<Integer>> newStartIndex = new HashMap<>();
                newStartIndex.put(first_btn, () -> 0);
                newStartIndex.put(prev_btn, () -> getCurrentOverview().getStartIndex() - GExtensionStore.PAGESIZE);
                newStartIndex.put(next_btn, () -> getCurrentOverview().getStartIndex() + GExtensionStore.PAGESIZE);
                newStartIndex.put(last_btn, () -> {
                    int lastPageSize = getCurrentOverview().getMaxAmount() % GExtensionStore.PAGESIZE;
                    if (lastPageSize == 0) lastPageSize = GExtensionStore.PAGESIZE;
                    return getCurrentOverview().getMaxAmount() - lastPageSize;
                });

                Arrays.asList(first_btn, prev_btn, next_btn, last_btn).forEach(l ->
                    ((EventTarget) l).addEventListener("click", event ->{
                        if (!initialized || l.getAttribute("class").contains("gdisabled")) return;
                        overwriteCurrentOverview(getCurrentOverview().getNewPage(newStartIndex.get(l).get(), GExtensionStore.PAGESIZE));
                        }, true));


                Element return_btn = webView.getEngine().getDocument().getElementById("return_btn");
                Element generic_btn = webView.getEngine().getDocument().getElementById("generic_btn");

                ((EventTarget) return_btn).addEventListener("click", event -> popOverview(), true);
                ((EventTarget) generic_btn).addEventListener("click", event -> {
                    if (initialized && getCurrentOverview() != null && getCurrentOverview().buttonEnabled()) {
                        getCurrentOverview().buttonClick(extensionStore);
                    }
                }, true);



                UIInitialized = true;
                maybeInitialized();
            }
        });

        webView.getEngine().load(GExtensionStoreController.class.getResource("webview/index.html").toString());

    }


    private void setRootOverview(HOverview overview) {
        currentOverviews.clear();
        pushOverview(overview);
    }

    private void overwriteCurrentOverview(HOverview overview) {
        currentOverviews.removeLast();
        pushOverview(overview);
    }

    public void pushOverview(HOverview overview) {
        currentOverviews.add(overview);
        setOverview(true);
    }

    public void reloadOverview() {
        setOverview(false);
    }

    private void popOverview() {
        if (currentOverviews.size() > 1) {
            currentOverviews.removeLast();
            setOverview(true);
        }
    }

    private List<? extends ContentItem> currentContentItems; // store so java doesn't garbage collect it!

    public void setOverview(boolean scrollTop) {
        if (initialized) {
            Platform.runLater(() -> {
                HOverview overview = currentOverviews.getLast();

                Element content_items_container = webView.getEngine().getDocument().getElementById(contentItemsContainer);
                WebUtils.clearElement(content_items_container);

                currentContentItems = overview.getContentItems();
                for (int i = 0; i < currentContentItems.size(); i++) {
                    currentContentItems.get(i).addHtml(i, extensionStore);
                }
                if (scrollTop) {
                    webView.getEngine().executeScript("document.getElementById('" + contentItemsContainer + "').scrollTop = 0");
                }

                Element first_btn = webView.getEngine().getDocument().getElementById("first_btn");
                Element prev_btn = webView.getEngine().getDocument().getElementById("prev_btn");
                Element next_btn = webView.getEngine().getDocument().getElementById("next_btn");
                Element last_btn = webView.getEngine().getDocument().getElementById("last_btn");
                WebUtils.removeClass(first_btn, "gdisabled");
                WebUtils.removeClass(prev_btn, "gdisabled");
                WebUtils.removeClass(next_btn, "gdisabled");
                WebUtils.removeClass(last_btn, "gdisabled");

                boolean isLast = overview.getMaxAmount() <= overview.getAmount() + overview.getStartIndex();
                boolean isFirst = overview.getStartIndex() < GExtensionStore.PAGESIZE;
                if (isLast) {
                    WebUtils.addClass(next_btn, "gdisabled");
                    WebUtils.addClass(last_btn, "gdisabled");
                }
                if (isFirst) {
                    WebUtils.addClass(first_btn, "gdisabled");
                    WebUtils.addClass(prev_btn, "gdisabled");
                }
                int thispage = Math.max(1, 1 + (overview.getStartIndex() / GExtensionStore.PAGESIZE));
                int lastpage = Math.max(1, 1 + ((overview.getMaxAmount() - 1) / GExtensionStore.PAGESIZE));
                webView.getEngine().executeScript("document.getElementById('paging_lbl').innerHTML = '" + thispage + " / " + lastpage + "';");


                Element return_btn = webView.getEngine().getDocument().getElementById("return_btn");
                Element generic_btn = webView.getEngine().getDocument().getElementById("generic_btn");
                WebUtils.removeClass((Element) return_btn.getParentNode(), "invisible");
                WebUtils.removeClass((Element) generic_btn.getParentNode(), "invisible");

                if (currentOverviews.size() < 2) WebUtils.addClass((Element) return_btn.getParentNode(), "invisible");
                if (overview.buttonText() == null) WebUtils.addClass((Element) generic_btn.getParentNode(), "invisible");
                else webView.getEngine().executeScript("document.getElementById('generic_btn').innerHTML = '" + overview.buttonText() + "';");


                WebUtils.removeClass((Element) generic_btn.getParentNode(), "gdisabled");
                if (!overview.buttonEnabled()) WebUtils.addClass((Element) generic_btn.getParentNode(), "gdisabled");

                webView.getEngine().executeScript(String.format("setHeading(\"%s\", \"%s\", \"%s\")",
                        WebUtils.escapeHtmlNoBackslash(overview.header().iconUrl()),
                        WebUtils.escapeMessageAndQuotes(overview.header().title()),
                        WebUtils.escapeMessageAndQuotes(overview.header().description())
                ));
                webView.getEngine().executeScript(String.format("setContentTitle(\"%s\")",
                        WebUtils.escapeMessageAndQuotes(overview.header().contentTitle())
                ));
            });
        }
    }

    public void maybeInitialized() {
        if (UIInitialized && extensionStore != null && extensionStore.getRepository() != null) {
            onFullInitialize();
        }
    }

    private void onFullInitialize() {
        initialized = true;
        setRootOverview(new ByUpdateOverview(null, 0, GExtensionStore.PAGESIZE, getStoreRepository()));
    }

    public void gExtensionStore(GExtensionStore gExtensionStore) {
        this.extensionStore = gExtensionStore;
    }

    public GExtensionStore getExtensionStore() {
        return extensionStore;
    }

    public StoreRepository getStoreRepository() {
        return extensionStore.getRepository();
    }

    public WebView getWebView() {
        return webView;
    }

    public HOverview getCurrentOverview() {
        return currentOverviews.getLast();
    }

    public String getContentItemsContainer() {
        return contentItemsContainer;
    }

    public static void reloadPage() {
        instance.webView.getEngine().reload();
    }
}
