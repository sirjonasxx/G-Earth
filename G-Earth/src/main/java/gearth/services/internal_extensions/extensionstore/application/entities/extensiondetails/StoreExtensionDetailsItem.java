package gearth.services.internal_extensions.extensionstore.application.entities.extensiondetails;

import gearth.services.internal_extensions.extensionstore.GExtensionStore;
import gearth.services.internal_extensions.extensionstore.application.GExtensionStoreController;
import gearth.services.internal_extensions.extensionstore.application.WebUtils;
import gearth.services.internal_extensions.extensionstore.application.entities.ContentItem;
import gearth.services.internal_extensions.extensionstore.application.entities.HOverview;
import gearth.services.internal_extensions.extensionstore.repository.models.ExtCategory;
import gearth.services.internal_extensions.extensionstore.repository.models.StoreExtension;
import gearth.services.internal_extensions.extensionstore.tools.EncodingUtil;
import netscape.javascript.JSObject;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class StoreExtensionDetailsItem implements ContentItem {

    private static final String OUTFIT_URL = "https://www.habbo{hotel}/habbo-imaging/avatarimage?&user={user}&direction=2&head_direction=2";

    private final StoreExtension storeExtension;
    private GExtensionStore gExtensionStore = null;

    private String id;

    public StoreExtensionDetailsItem(StoreExtension storeExtension) {
        this.storeExtension = storeExtension;
    }


    private String getContents() {
        StringBuilder contentBuilder = new StringBuilder();

        String descriptionQuoted = Arrays.stream(storeExtension.getDescription().split("\r\n|\r|\n"))
                .map(s -> "> " + s + "\n").collect(Collectors.joining());

        if (storeExtension.getReadme() != null) {
            descriptionQuoted = descriptionQuoted + "> \n> --url:README-" + storeExtension.getReadme() + "\n";
        }

        contentBuilder
                .append(String.format("*%s*", storeExtension.getTitle())).append(" - v").append(storeExtension.getVersion()).append("\n\n")
                .append("*Description*\n").append(descriptionQuoted).append("\n \n")
                .append("*Author(s):* ").append(storeExtension.getAuthors().stream().map(StoreExtension.Author::getName).collect(Collectors.joining(", "))).append("\n\n")
                .append("*Categories:* ").append(storeExtension.getCategories().stream().map(ExtCategory::getName).collect(Collectors.joining(", "))).append("\n\n");

        contentBuilder.append("*Technical information*").append("\n")
                .append("> Language: ").append(storeExtension.getLanguage()).append("\n")
                .append("> Source: --url:Click Here-").append(storeExtension.getSource()).append("\n")
                .append("> Framework: ").append(storeExtension.getFramework().getFramework().getName()).append(" - v").append(storeExtension.getFramework().getVersion()).append("\n")
                .append("> Systems: ").append(String.join(", ", storeExtension.getCompatibility().getSystems())).append("\n \n");

        contentBuilder.append("*Compatible clients:* ").append(String.join(", ", storeExtension.getCompatibility().getClients())).append("\n\n");

        if (storeExtension.getFramework().getFramework().isInstallationRequired()) {
            contentBuilder.append("Warning: the framework requires --url:additional installations-")
                    .append(storeExtension.getFramework().getFramework().getInstallationInstructions()).append(" !\n\n");
        }
        if (!storeExtension.isStable()) {
            contentBuilder.append("Warning: this extension has been marked unstable!\n\n");
        }

        contentBuilder.append("\n*Screenshot: *").append("\n")
                .append("--img:").append(gExtensionStore.getRepository().getResourceUrl("store", "extensions", storeExtension.getTitle(), "screenshot.png"));

        return contentBuilder.toString();
    }

    public void openUrl(String url) {
        gExtensionStore.getHostServices().showDocument(url);
    }


    private String contentsInHtml() {
        String comment = WebUtils.escapeMessage(getContents());
        List<String> lines = new ArrayList<>(Arrays.asList(comment.split("<br>")));

        boolean isquoting = false;
        boolean justEndedQuoting = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            if (!isquoting && line.startsWith("&gt;")) {
                isquoting = true;
                line = "<div class=\"cbc_quote\">" + line.substring(line.startsWith("&gt; ") ? 5 : 4);
            }
            else if (isquoting && line.startsWith("&gt;")) {
                line = line.substring(line.startsWith("&gt; ") ? 5 : 4);
            }
            else if (isquoting && !line.startsWith("&gt;")) {
                justEndedQuoting = true;
                isquoting = false;
                String prev = lines.get(i - 1);
                lines.set(i-1, prev.substring(0, prev.length() - 4) + "</div>");
            }

            if (justEndedQuoting && line.length() == 0) continue;
            justEndedQuoting = false;

            line = line.replaceAll("\\*([^*]*)\\*", "<b>$1</b>")
                    .replaceAll("_([^_<>]*)_", "<i>$1</i>")
                    .replaceAll("(^| |>)@([^ <>]*)($| |<)", "$1<u>$2</u>$3")
                    .replaceAll("--img:([^ ]*)", "<img src=\"$1\" alt=\"extension screenshot\">")
                    .replaceAll("--url:([^-]*)-(https?:[^ ]*)",
                            String.format("<a href=\"#\" onClick=\"%s.openUrl(&quot;$2&quot;)\">$1</a>", id));
//                    .replaceAll("([^\";])(https?:[^ ]*)",
//                            String.format("$1<a href=\"#\" onClick=\"%s.openUrl(&quot;$1&quot;)\">$2</a>", id));

            lines.set(i, line + (i == lines.size() - 1 ? "" : "<br>"));
        }

        return String.join("", lines);
    }

    @Override
    public void addHtml(int i, GExtensionStore gExtensionStore) {
        this.gExtensionStore = gExtensionStore;

        StoreExtension.Author mainAuthor = storeExtension.getAuthors().get(0);

        String avatarLook = "";
        if (mainAuthor.getHotel() != null && mainAuthor.getUsername() != null) {
            avatarLook = OUTFIT_URL
                    .replace("{hotel}", mainAuthor.getHotel())
                    .replace("{user}", mainAuthor.getUsername());
        }

        id = "extdetail" + i + "_" + System.currentTimeMillis();

        StringBuilder htmlBuilder = new StringBuilder()
                .append("<div id=\"").append(id).append("\" class=\"comment_item content_item\">")

                .append("<div class=\"comment_header\">")
                .append("<div class=\"ch_timeago\">").append(WebUtils.elapsedSince(storeExtension.getUpdateDate())).append(" ago</div>")
                .append("</div>")

                .append("<div class=\"comment_body comment_open\">")
                .append("<div class=\"cb_author\">")
                .append("<div class=\"cba_name\">").append(WebUtils.escapeMessage(mainAuthor.getName())).append("</div>")
                .append("<div class=\"cba_text\">").append(mainAuthor.getReputation()).append(" reputation</div>")
                .append("<div class=\"cba_text\">").append(mainAuthor.getExtensionsCount()).append(" releases</div>")
                .append("<div class=\"cba_look\"><img src=\"").append(avatarLook).append("\" alt=\"\"></div>") // todo look
                .append("</div>")
                .append("<div class=\"cb_content\">").append(contentsInHtml()).append("</div>")
                .append("</div>")

                .append("</div>")
                .append("<div class=\"comment_itemcontent_item\"></div>");

        String forum = htmlBuilder.toString();
        GExtensionStoreController c = gExtensionStore.getController();
        c.getWebView().getEngine().executeScript("document.getElementById('" + c.getContentItemsContainer() + "').innerHTML += '" + forum + "';");

        JSObject window = (JSObject) c.getWebView().getEngine().executeScript("window");
        window.setMember(id, this);
    }
}
