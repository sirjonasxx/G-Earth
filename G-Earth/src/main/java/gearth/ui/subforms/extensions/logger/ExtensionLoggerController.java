package gearth.ui.subforms.extensions.logger;

import gearth.misc.StringUtils;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.net.URL;
import java.util.*;

public class ExtensionLoggerController implements Initializable {

    public BorderPane borderPane;

    private StyleClassedTextArea area;

    private volatile boolean initialized = false;
    private final List<Element> appendOnLoad = new ArrayList<>();

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        area = new StyleClassedTextArea();
        area.getStyleClass().add("themed-background");
        area.setWrapText(true);
        area.setEditable(false);

        VirtualizedScrollPane<StyleClassedTextArea> vsPane = new VirtualizedScrollPane<>(area);
        borderPane.setCenter(vsPane);
        vsPane.getStyleClass().add("themed-background");
        borderPane.getStyleClass().add("themed-background");

        synchronized (appendOnLoad) {
            initialized = true;
            if (!appendOnLoad.isEmpty()) {
                appendLog(appendOnLoad);
                appendOnLoad.clear();
            }
        }
    }

    private synchronized void appendLog(List<Element> elements) {
        Platform.runLater(() -> {
            final StringBuilder sb = new StringBuilder();
            final StyleSpansBuilder<Collection<String>> styleSpansBuilder = new StyleSpansBuilder<>(0);

            for (Element element : elements) {
                sb.append(element.text);

                styleSpansBuilder.add(Collections.singleton(element.className), element.text.length());
            }

            int oldLen = area.getLength();
            area.appendText(sb.toString());
//            System.out.println(sb.toString());
            area.setStyleSpans(oldLen, styleSpansBuilder.create());

//            if (autoScroll) {
                area.moveTo(area.getLength());
                area.requestFollowCaret();
//            }
        });
    }

    void log(String s) {
        s = StringUtils.cleanTextContent(s);
        ArrayList<Element> elements = new ArrayList<>();

        String classname, text;
        if (s.startsWith("[") && s.contains("]")) {
            classname = s.substring(1, s.indexOf("]"));
            text = s.substring(s.indexOf("]") + 1);
        }
        else {
            classname = "label";
            text = s;
        }

        if (text.contains(" --> ")) {
            int index = text.indexOf(" --> ") + 5;
            String extensionAnnouncement = text.substring(0, index);
            text = text.substring(index);
            elements.add(new Element(extensionAnnouncement, "label"));
        }
        elements.add(new Element(text + "\n", classname.toLowerCase()));

        synchronized (appendOnLoad) {
            if (initialized) {
                appendLog(elements);
            }
            else {
                appendOnLoad.addAll(elements);
            }
        }
    }

    void setStage(Stage stage) {
    }

}
