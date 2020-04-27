package gearth.ui.extensions.logger;

import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.net.URL;
import java.util.*;

public class ExtensionLoggerController implements Initializable {
    public BorderPane borderPane;

    private Stage stage = null;
    private StyleClassedTextArea area;

    private volatile boolean initialized = false;
    private final List<Element> appendOnLoad = new ArrayList<>();


    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        area = new StyleClassedTextArea();
        area.getStyleClass().add("white");
        area.setWrapText(true);
        area.setEditable(false);

        VirtualizedScrollPane<StyleClassedTextArea> vsPane = new VirtualizedScrollPane<>(area);
        borderPane.setCenter(vsPane);

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
            StringBuilder sb = new StringBuilder();
            StyleSpansBuilder<Collection<String>> styleSpansBuilder = new StyleSpansBuilder<>(0);

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
        s = cleanTextContent(s);
        ArrayList<Element> elements = new ArrayList<>();

        String classname, text;
        if (s.startsWith("[") && s.contains("]")) {
            classname = s.substring(1, s.indexOf("]"));
            text = s.substring(s.indexOf("]") + 1);
        }
        else {
            classname = "black";
            text = s;
        }

        if (text.contains(" --> ")) {
            int index = text.indexOf(" --> ") + 5;
            String extensionAnnouncement = text.substring(0, index);
            text = text.substring(index);
            elements.add(new Element(extensionAnnouncement, "black"));
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
        this.stage = stage;
    }

    private static String cleanTextContent(String text)
    {
//        // strips off all non-ASCII characters
//        text = text.replaceAll("[^\\x00-\\x7F]", "");
//
//        // erases all the ASCII control characters
        text = text.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");

        // removes non-printable characters from Unicode
//        text = text.replaceAll("\\p{C}", "");

        return text.trim();
    }

}
