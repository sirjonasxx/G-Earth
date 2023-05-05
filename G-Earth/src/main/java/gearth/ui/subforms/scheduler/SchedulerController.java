package gearth.ui.subforms.scheduler;

import com.tulskiy.keymaster.common.Provider;
import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.services.scheduler.Interval;
import gearth.services.scheduler.Scheduler;
import gearth.ui.GEarthProperties;
import gearth.ui.SubForm;
import gearth.ui.translations.LanguageBundle;
import gearth.ui.translations.TranslatableString;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created by Jonas on 06/04/18.
 */
public class SchedulerController extends SubForm implements Initializable {

    private static final Interval defaultInterval = new Interval(0, 500);
    private static final HPacket defaultPacket = new HPacket("Chat", HMessage.Direction.TOCLIENT, -1, "Frank loves G-Earth", 0, 33, 0, 0);

    public VBox root;
    public GridPane headerGrid;
    public ScrollPane scrollpane;

    public Button addOrEditButton;

    public TextField packetDelayField;
    public TextField packetExpressionField;

    public ToggleGroup packetTypeOptions;
    public RadioButton packetIncomingOption;
    public RadioButton packetOutgoingOption;

    public Button clearButton;
    public Button saveButton;
    public Button loadButton;

    public CheckBox enableHotkeysBox;

    public Label
            tableIndexLabel,
            tablePacketLabel,
            tableIntervalLabel,
            tableDestinationLabel,
            tableEditLabel,
            packetExpressionLabel,
            packetIntervalLabel;

    private TranslatableString addOrEditString;

    private InteractableScheduleItem isBeingEdited = null;

    private Scheduler<InteractableScheduleItem> scheduler = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        scrollpane.widthProperty().addListener(observable -> headerGrid.setPrefWidth(scrollpane.getWidth()));
        packetTypeOptions.selectToggle(packetTypeOptions.getToggles().get(0));

        packetExpressionField.textProperty().addListener(event -> Platform.runLater(this::updateUI));
        packetDelayField.textProperty().addListener(event -> Platform.runLater(this::updateUI));

        clearButton.setOnAction(a -> clear());

        //register hotkeys
        //disable some output things
        PrintStream err = System.err;
        System.setErr(new PrintStream(new OutputStream() {
            public void write(int b) {
            }
        }));

        Provider provider = Provider.getCurrentProvider(false);
        for (int i = 0; i < 10; i++) {
            int[] ii = {i};
            provider.register(KeyStroke.getKeyStroke("control shift " + ii[0]), hotKey -> switchPauseHotkey(ii[0]));
        }
        System.setErr(err);

        initLanguageBinding();
        setInputDefault(true);
    }

    @Override
    protected void onParentSet() {
        scheduler = new Scheduler<>(getHConnection());
        final InvalidationListener updateUI = observable -> Platform.runLater(this::updateUI);
        GEarthProperties.enableDeveloperModeProperty.addListener(updateUI);
        getHConnection().stateProperty().addListener(updateUI);
        updateUI();
    }

    @FXML
    public void onClickAddOrEditButton() {
        if (isBeingEdited == null) {
            final HPacket packet = new HPacket(packetExpressionField.getText());
            if (packet.isCorrupted())
                return;
            final InteractableScheduleItem newItem = new InteractableScheduleItem(
                    scheduler.size(),
                    false,
                    new Interval(packetDelayField.getText()),
                    packetExpressionField.getText(),
                    packetIncomingOption.isSelected() ? HMessage.Direction.TOCLIENT : HMessage.Direction.TOSERVER);
            addItem(newItem);
        }
        else {
            isBeingEdited.getPacketAsStringProperty().set(packetExpressionField.getText());
            isBeingEdited.getDelayProperty().set(new Interval(packetDelayField.getText()));
            isBeingEdited.getDestinationProperty().set(packetIncomingOption.isSelected() ? HMessage.Direction.TOCLIENT : HMessage.Direction.TOSERVER);
            isBeingEdited.isUpdatedTrigger();
            isBeingEdited = null;
            setInputDefault(false);
        }
    }

    @FXML
    public void onClickSaveButton() {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(LanguageBundle.get("tab.scheduler.button.save.windowtitle"));
        fileChooser.getExtensionFilters().add(getExtensionFilter());
        final File file = fileChooser.showSaveDialog(parentController.getStage());
        if (file != null) {
            final List<String> strings = new ArrayList<>();
            for (int i = 0; i < scheduler.size(); i++) {
                strings.add(scheduler.get(i).stringify());
                if (i != scheduler.size() - 1)
                    strings.add("\n");
            }
            try {
                Files.write(file.toPath(), strings);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void onClickLoadButton() {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(LanguageBundle.get("tab.scheduler.button.load.windowtitle"));
        fileChooser.getExtensionFilters().addAll(getExtensionFilter());
        final File selectedFile = fileChooser.showOpenDialog(parentController.getStage());
        if (selectedFile != null) {
            try {
                clear();
                Files.readAllLines(selectedFile.toPath())
                        .stream()
                        .map(InteractableScheduleItem::new)
                        .forEach(this::addItem);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void switchPauseHotkey(int index) {
        if (enableHotkeysBox.isSelected() && index < scheduler.size()) {
            scheduler.get(index).getPausedProperty().set(!scheduler.get(index).getPausedProperty().get());
        }
    }

    private void updateUI() {

        final HConnection connection = getHConnection();

        if (connection == null)
            return;

        final HMessage.Direction direction = packetIncomingOption.isSelected() ? HMessage.Direction.TOCLIENT : HMessage.Direction.TOSERVER;
        final HPacket packet = new HPacket(packetExpressionField.getText());
        final boolean isPacketOk = connection.canSendPacket(direction, packet);

        addOrEditButton.setDisable(!Interval.isValid(packetDelayField.getText()) || !isPacketOk);
    }

    private void addItem(InteractableScheduleItem newItem) {
        new ScheduleItemContainer(newItem, root, scrollpane);
        scheduler.add(newItem);
        newItem.onDelete(() -> {
            if (isBeingEdited == newItem) {
                setInputDefault(false);
                isBeingEdited = null;
            }
            scheduler.remove(newItem);
            for (int i = 0; i < scheduler.size(); i++) {
                scheduler.get(i).getIndexProperty().set(i);
            }
        });
        newItem.onEdit(() -> {
            if (isBeingEdited != null) {
                isBeingEdited.isUpdatedTrigger();
            }

            if (isBeingEdited != newItem) {
                packetExpressionField.setText(newItem.getPacketAsStringProperty().get());
                packetDelayField.setText(newItem.getDelayProperty().get().toString());
                packetIncomingOption.setSelected(newItem.getDestinationProperty().get() == HMessage.Direction.TOCLIENT);
                packetOutgoingOption.setSelected(newItem.getDestinationProperty().get() == HMessage.Direction.TOSERVER);

                isBeingEdited = newItem;
                addOrEditString.setKey(0, "tab.scheduler.button.edit");
                updateUI();
                newItem.onIsBeingUpdatedTrigger();
            }
            else {
                setInputDefault(false);
                isBeingEdited.isUpdatedTrigger();
                isBeingEdited = null;
            }
        });
    }

    private void setInputDefault(boolean showDummyPacket) {
        packetDelayField.setText(defaultInterval.toString());
        packetExpressionField.setText(showDummyPacket ? defaultPacket.toExpression() : "");
        packetIncomingOption.setSelected(true);
        packetOutgoingOption.setSelected(false);

        addOrEditString.setKey(0, "tab.scheduler.button.add");
        updateUI();
    }


    private void clear() {
        for (int i = scheduler.size() - 1; i >= 0; i--)
            scheduler.get(i).delete();
    }

    private void initLanguageBinding() {
        addOrEditString = new TranslatableString("%s", "tab.scheduler.button.add");
        addOrEditButton.textProperty().bind(addOrEditString);

        clearButton.textProperty().bind(new TranslatableString("%s", "tab.scheduler.button.clear"));
        clearButton.setTooltip(new Tooltip());
        clearButton.getTooltip().textProperty().bind(new TranslatableString("%s", "tab.scheduler.button.clear.tooltip"));

        saveButton.textProperty().bind(new TranslatableString("%s", "tab.scheduler.button.save"));
        saveButton.setTooltip(new Tooltip());
        saveButton.getTooltip().textProperty().bind(new TranslatableString("%s", "tab.scheduler.button.save.tooltip"));

        loadButton.textProperty().bind(new TranslatableString("%s", "tab.scheduler.button.load"));
        loadButton.setTooltip(new Tooltip());
        loadButton.getTooltip().textProperty().bind(new TranslatableString("%s", "tab.scheduler.button.load.tooltip"));

        tableIndexLabel.textProperty().bind(new TranslatableString("%s", "tab.scheduler.table.index"));
        tablePacketLabel.textProperty().bind(new TranslatableString("%s", "tab.scheduler.table.packet"));
        tableIntervalLabel.textProperty().bind(new TranslatableString("%s", "tab.scheduler.table.interval"));
        tableDestinationLabel.textProperty().bind(new TranslatableString("%s", "tab.scheduler.table.destination"));
        tableEditLabel.textProperty().bind(new TranslatableString("%s", "tab.scheduler.table.edit"));

        packetExpressionLabel.textProperty().bind(new TranslatableString("%s:", "tab.scheduler.setup.packet"));
        packetIntervalLabel.textProperty().bind(new TranslatableString("%s:", "tab.scheduler.setup.interval"));

        packetIncomingOption.textProperty().bind(new TranslatableString("%s", "tab.scheduler.direction.in"));
        packetOutgoingOption.textProperty().bind(new TranslatableString("%s", "tab.scheduler.direction.out"));

        enableHotkeysBox.textProperty().bind(new TranslatableString("%s", "tab.scheduler.hotkeys"));
    }

    private static FileChooser.ExtensionFilter getExtensionFilter() {
        return new FileChooser.ExtensionFilter(LanguageBundle.get("tab.scheduler.filetype"), "*.sched");
    }
}
