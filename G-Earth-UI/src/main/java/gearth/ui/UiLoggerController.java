package gearth.ui;

import gearth.protocol.HPacket;
import gearth.ui.logger.loggerdisplays.PacketLogger;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ResourceBundle;

public class UiLoggerController implements Initializable {
    public FlowPane flowPane;
    public BorderPane borderPane;
    public Label lblViewIncoming;
    public Label lblViewOutgoing;
    public CheckMenuItem chkViewIncoming;
    public CheckMenuItem chkViewOutgoing;
    public CheckMenuItem chkDisplayStructure;

    private StyleClassedTextArea area;

    private boolean viewIncoming = true;
    private boolean viewOutgoing = true;
    private boolean displayStructure = true;

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        area = new StyleClassedTextArea();
        area.getStyleClass().add("dark");
        area.setWrapText(true);

        VirtualizedScrollPane<StyleClassedTextArea> vsPane = new VirtualizedScrollPane<>(area);
        borderPane.setCenter(vsPane);
    }

    public void appendMessage(HPacket packet, int types) {
        boolean isBlocked = (types & PacketLogger.MESSAGE_TYPE.BLOCKED.getValue()) != 0;
        boolean isReplaced = (types & PacketLogger.MESSAGE_TYPE.REPLACED.getValue()) != 0;
        boolean isIncoming = (types & PacketLogger.MESSAGE_TYPE.INCOMING.getValue()) != 0;

        if (isIncoming && !viewIncoming) return;
        if (!isIncoming && !viewOutgoing) return;

        ArrayList<Element> elements = new ArrayList<>();

        String expr = packet.toExpression();

        if (isBlocked) elements.add(new Element("[Blocked]\n", "blocked"));
        else if (isReplaced) elements.add(new Element("[Replaced]\n", "replaced"));

        if (isIncoming) {
            // handle skipped eventually
            elements.add(new Element("Incoming[", "incoming"));
            elements.add(new Element(String.valueOf(packet.headerId()), ""));
            elements.add(new Element("]", "incoming"));

            elements.add(new Element(" <- ", ""));
            elements.add(new Element(packet.toString(), "incoming"));

            if (!expr.equals("") && displayStructure)
                elements.add(new Element("\n" + expr, "incoming"));
        } else {
            elements.add(new Element("Outgoing[", "outgoing"));
            elements.add(new Element(String.valueOf(packet.headerId()), ""));
            elements.add(new Element("]", "outgoing"));

            elements.add(new Element(" -> ", ""));
            elements.add(new Element(packet.toString(), "outgoing"));

            if (!expr.equals("") && displayStructure)
                elements.add(new Element("\n" + expr, "outgoing"));
        }

        elements.add(new Element("\n--------------------\n", ""));
        AppendLog(elements);
    }

    private void AppendLog(ArrayList<Element> elements) {
        StringBuilder sb = new StringBuilder();
        StyleSpansBuilder<Collection<String>> styleSpansBuilder = new StyleSpansBuilder<>(0);

        for (Element element : elements) {
            sb.append(element.text);
            styleSpansBuilder.add(Collections.singleton(element.className), element.text.length());
        }

        int oldLen = area.getLength();
        area.appendText(sb.toString());
        area.setStyleSpans(oldLen, styleSpansBuilder.create());

        area.moveTo(area.getLength());
        area.requestFollowCaret();
    }

    public void toggleViewIncoming() {
        viewIncoming = !viewIncoming;
        lblViewIncoming.setText("View Incoming: " + (viewIncoming ? "True" : "False"));
        chkViewIncoming.setSelected(viewIncoming);
    }

    public void toggleViewOutgoing() {
        viewOutgoing = !viewOutgoing;
        lblViewOutgoing.setText("View Outgoing: " + (viewOutgoing ? "True" : "False"));
        chkViewOutgoing.setSelected(viewOutgoing);
    }

    public void toggleDisplayStructure() {
        displayStructure = !displayStructure;
        chkDisplayStructure.setSelected(displayStructure);
    }
}

class Element {
    final String text;
    final String className;

    Element(String text, String className) {
        this.text = text;
        this.className = className;
    }
}