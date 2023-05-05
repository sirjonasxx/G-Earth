package gearth.misc;

import gearth.GEarth;
import javafx.event.ActionEvent;
import javafx.scene.control.Hyperlink;

public final class HyperLinkUtil {

    public static void showDocumentOnClick(Hyperlink link) {
        link.setOnAction((ActionEvent event) -> {
            final Hyperlink hyperlink = (Hyperlink) event.getTarget();
            final String url = hyperlink.getTooltip().getText();
            GEarth.main.getHostServices().showDocument(url);
            event.consume();
        });
    }
}
