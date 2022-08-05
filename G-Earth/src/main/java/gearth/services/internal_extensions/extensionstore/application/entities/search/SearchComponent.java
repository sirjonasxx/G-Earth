package gearth.services.internal_extensions.extensionstore.application.entities.search;

import gearth.GEarth;
import gearth.services.internal_extensions.extensionstore.GExtensionStore;
import gearth.services.internal_extensions.extensionstore.application.GExtensionStoreController;
import gearth.services.internal_extensions.extensionstore.application.entities.ContentItem;
import gearth.services.internal_extensions.extensionstore.repository.StoreRepository;
import gearth.services.internal_extensions.extensionstore.repository.querying.ExtensionOrdering;
import netscape.javascript.JSObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SearchComponent implements ContentItem {

    private StoreRepository repository;

    private volatile String searchKeyword;
    private volatile ExtensionOrdering ordering;

    private volatile Map<String, Boolean> clients;
    private volatile Map<String, Boolean> categories;
    private volatile Map<String, Boolean> frameworks;

    public SearchComponent(StoreRepository repository) {
        this.repository = repository;

        this.searchKeyword = "";
        this.ordering = ExtensionOrdering.ALPHABETICAL;
        this.clients = new HashMap<>();
        this.categories = new HashMap<>();
        this.frameworks = new HashMap<>();

        repository.getClients().forEach(c -> clients.put(c, true));
        repository.getCategories().forEach(c -> categories.put(c.getName(), true));
        repository.getFrameworks().forEach(c -> frameworks.put(c.getName(), true));
    }

    public void setOrdering(String ordering) {
        this.ordering = ExtensionOrdering.fromString(ordering);
    }

    public void setSearchKeyword(String searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public void setClient(String client, boolean enabled) {
        clients.put(client, enabled);
    }

    public void setCategory(String category, boolean enabled) {
        categories.put(category, enabled);
    }

    public void setFramework(String framework, boolean enabled) {
        frameworks.put(framework, enabled);
    }

    private void addFilterBoxHtml(StringBuilder htmlBuilder, String name, String title, Map<String, Boolean> data, String id) {
        htmlBuilder.append("<div class=\"filterBox\">");

        htmlBuilder.append(String.format("<p>%s</p>", title));
        List<String> items = new ArrayList<>(data.keySet());
        for (int i = 0; i < items.size(); i++) {
            String item = items.get(i);
            String checkboxId = name.toLowerCase() + i;
            htmlBuilder.append(String.format("<input type=\"checkbox\" id=\"%s\"%s onchange=\"%s.set%s(&quot;%s&quot;, this.checked);\">",
                    checkboxId,
                    data.get(item) ? " checked" : "",
                    id,
                    name,
                    item
            ));
            htmlBuilder.append(String.format("<label for=\"%s\">%s</label><br>", checkboxId, item));
        }

        htmlBuilder.append("</div>");
    }


    @Override
    public void addHtml(int i, GExtensionStore gExtensionStore) {

        String id = "search" + i + "_" + System.currentTimeMillis();

        StringBuilder htmlBuilder = new StringBuilder();

        htmlBuilder
                .append("<div class=\"searchContainer\">")
                .append("<div class=\"searchInnerContainer\">")

                .append("<div class=\"centeredFlex\">")
                .append("<label for=\"keyword\">").append(GEarth.translation.getString("ext.store.search.bykeyword")).append(":</label>")
                .append(String.format("<input id=\"keyword\" value=\"%s\" name=\"keyword\" class=\"inputBox\" type=\"text\" " +
                        "oninput=\"%s.setSearchKeyword(this.value);\">", searchKeyword, id))
                .append("</div>")

                .append("<div class=\"centeredFlex\">")
                .append("<label for=\"ordering\">").append(GEarth.translation.getString("ext.store.search.ordering")).append("</label>");

        // add ordering stuff
        htmlBuilder.append(String.format("<select class=\"inputBox\" name=\"ordering\" id=\"ordering\" " +
                "onchange=\"%s.setOrdering(this.value);\">", id));
        repository.getOrderings().forEach(o -> {
            htmlBuilder.append(
                    String.format("<option value=\"%s\"%s>%s</option>",
                            o.getOrderName(),
                            o.getOrderName().equals(ordering.getOrderName()) ? " selected" : "",
                            o.getOrderName()
                    )
            );
        });

        htmlBuilder
                .append("</select>")
                .append("</div>")

                .append("<div class=\"filterStuff\">");

        addFilterBoxHtml(htmlBuilder, "Client", GEarth.translation.getString("ext.store.search.filter.clients") + ":", clients, id);
        addFilterBoxHtml(htmlBuilder, "Category", GEarth.translation.getString("ext.store.search.filter.categories") + ":", categories, id);
        addFilterBoxHtml(htmlBuilder, "Framework", GEarth.translation.getString("ext.store.search.filter.frameworks") + ":", frameworks, id);

        htmlBuilder
                .append("</div>")
                .append("<br><p>").append(GEarth.translation.getString("ext.store.search.info.automaticosfiltering")).append("</p>")

                .append("</div>")
                .append("</div>");


        String searchHtml = htmlBuilder.toString();
        GExtensionStoreController controller = gExtensionStore.getController();

        controller.getWebView().getEngine().executeScript("document.getElementById('" + controller.getContentItemsContainer() + "').innerHTML += '" + searchHtml + "';");

        JSObject window = (JSObject) controller.getWebView().getEngine().executeScript("window");
        window.setMember(id, this);

    }


    public StoreRepository getRepository() {
        return repository;
    }

    public List<String> getCategories() {
        return categories.keySet().stream().filter(c -> categories.get(c)).collect(Collectors.toList());
    }

    public List<String> getClients() {
        return clients.keySet().stream().filter(c -> clients.get(c)).collect(Collectors.toList());
    }

    public List<String>  getFrameworks() {
        return frameworks.keySet().stream().filter(f -> frameworks.get(f)).collect(Collectors.toList());
    }

    public ExtensionOrdering getOrdering() {
        return ordering;
    }

    public String getSearchKeyword() {
        return searchKeyword;
    }
}
