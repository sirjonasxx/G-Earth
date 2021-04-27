package gearth.ui.scheduler;

import com.tulskiy.keymaster.common.Provider;
import gearth.services.scheduler.Interval;
import gearth.services.scheduler.ScheduleItem;
import gearth.services.scheduler.Scheduler;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.ui.SubForm;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Jonas on 06/04/18.
 */
public class SchedulerController extends SubForm {

    private static final Interval defaultInterval = new Interval(0, 500);
    private static final HPacket defaultPacket = new HPacket(0);

    public VBox schedulecontainer;
    public GridPane header;
    public ScrollPane scrollpane;

    public Button btn_addoredit;

    public TextField txt_delay;
    public ToggleGroup scheduler_dest;
    public TextField txt_packet;
    public RadioButton rb_incoming;
    public RadioButton rb_outgoing;

    public Button btn_clear;
    public Button btn_save;
    public Button btn_load;

    public CheckBox cbx_hotkeys;

    private InteractableScheduleItem isBeingEdited = null;

    private Scheduler<InteractableScheduleItem> scheduler = null;


    public void initialize() {
        scrollpane.widthProperty().addListener(observable -> header.setPrefWidth(scrollpane.getWidth()));
        scheduler_dest.selectToggle(scheduler_dest.getToggles().get(0));

        txt_packet.textProperty().addListener(event -> Platform.runLater(this::updateUI));
        txt_delay.textProperty().addListener(event -> Platform.runLater(this::updateUI));

        btn_clear.setTooltip(new Tooltip("Clear all items"));
        btn_save.setTooltip(new Tooltip("Save to file"));
        btn_load.setTooltip(new Tooltip("Load from file"));

        updateUI();

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
    }

    @Override
    protected void onParentSet() {
        scheduler = new Scheduler<>(getHConnection());
    }

    private void switchPauseHotkey(int index) {
        if (cbx_hotkeys.isSelected() && index < scheduler.size()) {
            scheduler.get(index).getPausedProperty().set(!scheduler.get(index).getPausedProperty().get());
        }
    }

    public static boolean stringIsNumber(String str) {
        try {
            Integer.parseInt(str);
            return true;
        }
        catch (Exception e){
            return false;
        }
    }

    private void updateUI() {
        btn_addoredit.setDisable(!Interval.isValid(txt_delay.getText()) || new HPacket(txt_packet.getText()).isCorrupted());
    }

    public void scheduleBtnClicked(ActionEvent actionEvent) {
        if (isBeingEdited == null) {
            HPacket packet = new HPacket(txt_packet.getText());
            if (packet.isCorrupted()) return;

            InteractableScheduleItem newItem = new InteractableScheduleItem(
                    scheduler.size(),
                    false,
                    new Interval(txt_delay.getText()),
                    txt_packet.getText(),
                    rb_incoming.isSelected() ? HMessage.Direction.TOCLIENT : HMessage.Direction.TOSERVER);

            addItem(newItem);
        }
        else {

            isBeingEdited.getPacketAsStringProperty().set(txt_packet.getText());
            isBeingEdited.getDelayProperty().set(new Interval(txt_delay.getText()));
            isBeingEdited.getDestinationProperty().set(rb_incoming.isSelected() ? HMessage.Direction.TOCLIENT : HMessage.Direction.TOSERVER);
            isBeingEdited.isUpdatedTrigger();

            isBeingEdited = null;
            setInputDefault();
        }

    }

    private void addItem(InteractableScheduleItem newItem) {
        new ScheduleItemContainer(newItem, schedulecontainer, scrollpane);
        scheduler.add(newItem);


        newItem.onDelete(() -> {
            if (isBeingEdited == newItem) {
                setInputDefault();
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
                txt_packet.setText(newItem.getPacketAsStringProperty().get());
                txt_delay.setText(newItem.getDelayProperty().get().toString());
                rb_incoming.setSelected(newItem.getDestinationProperty().get() == HMessage.Direction.TOCLIENT);
                rb_outgoing.setSelected(newItem.getDestinationProperty().get() == HMessage.Direction.TOSERVER);

                isBeingEdited = newItem;
                btn_addoredit.setText("Edit");
                updateUI();
                newItem.onIsBeingUpdatedTrigger();
            }
            else {
                setInputDefault();
                isBeingEdited.isUpdatedTrigger();
                isBeingEdited = null;
            }
        });
    }

    private void setInputDefault() {
        txt_delay.setText(defaultInterval.toString());
        txt_packet.setText(defaultPacket.toString());
        rb_incoming.setSelected(true);
        rb_outgoing.setSelected(false);

        btn_addoredit.setText("Add");
        updateUI();
    }


    private void clear() {
        for (int i = scheduler.size() - 1; i >= 0; i--) {
            scheduler.get(i).delete();
        }
    }
    private void load(List<InteractableScheduleItem> list) {
        clear();

        for (InteractableScheduleItem item : list) {
            addItem(item);
        }
    }


    public void clearBtnClicked(ActionEvent actionEvent) {
        clear();
    }

    public void saveBtnClicked(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();

        //Set extension filter
        FileChooser.ExtensionFilter extFilter =
                new FileChooser.ExtensionFilter("SCHED files (*.sched)", "*.sched");
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setTitle("Save Schedule File");

        //Show save file dialog
        File file = fileChooser.showSaveDialog(parentController.getStage());

        if(file != null){
            try {
                FileWriter fileWriter = new FileWriter(file);
                BufferedWriter out = new BufferedWriter(fileWriter);

                for (int i = 0; i < scheduler.size(); i++) {
                    out.write(scheduler.get(i).stringify());
                    if (i != scheduler.size() - 1) out.write("\n");
                }

                out.flush();
                out.close();
                fileWriter.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }
    }

    public void loadBtnClicked(ActionEvent actionEvent) {
        List<InteractableScheduleItem> list = new ArrayList<>();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Schedule File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Schedule Files", "*.sched"));
        File selectedFile = fileChooser.showOpenDialog(parentController.getStage());
        if (selectedFile != null) {

            FileReader fr = null;
            try {
                fr = new FileReader(selectedFile);
                BufferedReader br = new BufferedReader(fr);
                String line = null;

                while ((line = br.readLine()) != null)
                {
                    list.add(new InteractableScheduleItem(line));
                }

                fr.close();
                br.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        load(list);

    }
}
