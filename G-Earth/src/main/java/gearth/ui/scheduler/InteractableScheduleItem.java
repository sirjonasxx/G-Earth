package gearth.ui.scheduler;

import gearth.misc.StringifyAble;
import gearth.misc.listenerpattern.Observable;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.services.scheduler.Interval;
import gearth.services.scheduler.ScheduleItem;
import gearth.services.scheduler.listeners.OnBeingUpdatedListener;
import gearth.services.scheduler.listeners.OnDeleteListener;
import gearth.services.scheduler.listeners.OnEditListener;
import gearth.services.scheduler.listeners.OnUpdatedListener;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

public class InteractableScheduleItem extends ScheduleItem implements StringifyAble {


    public InteractableScheduleItem(int index, boolean paused, Interval delay, String inputPacket, HMessage.Direction destination) {
        super(index, paused, delay, inputPacket, destination);
    }

    public InteractableScheduleItem(String stringifyAbleRepresentation) {
        constructFromString(stringifyAbleRepresentation);
    }

    private Observable<OnDeleteListener> onDeleteObservable = new Observable<>(OnDeleteListener::onDelete);
    public void onDelete(OnDeleteListener listener) {
        onDeleteObservable.addListener(listener);
    }
    public void delete() {
        onDeleteObservable.fireEvent();
    }

    private Observable<OnEditListener> onEditObservable = new Observable<>(OnEditListener::onEdit);
    public void onEdit(OnEditListener listener) {
        onEditObservable.addListener(listener);
    }
    public void edit() {
        onEditObservable.fireEvent();
    }

    private Observable<OnUpdatedListener> onUpdatedObservable = new Observable<>(OnUpdatedListener::onUpdated);
    public void onIsupdated(OnUpdatedListener listener) {
        onUpdatedObservable.addListener(listener);
    }
    public void isUpdatedTrigger() {
        onUpdatedObservable.fireEvent();
    }

    private Observable<OnBeingUpdatedListener> onBeingUpdatedObservable = new Observable<>(OnBeingUpdatedListener::onBeingUpdated);
    public void onIsBeingUpdated(OnBeingUpdatedListener listener) {
        onBeingUpdatedObservable.addListener(listener);
    }
    public void onIsBeingUpdatedTrigger() {
        onBeingUpdatedObservable.fireEvent();
    }


    @Override
    public String stringify() {
        StringBuilder b = new StringBuilder();
        b       .append(getIndexProperty().get())
                .append("\t")
                .append(getPausedProperty().get() ? "true" : "false")
                .append("\t")
                .append(getDelayProperty().get().toString())
                .append("\t")
                .append(getDestinationProperty().get().name())
                .append("\t")
                .append(getPacketAsStringProperty().get());
        return b.toString();
    }

    @Override
    public void constructFromString(String str) {
        String[] parts = str.split("\t");
        if (parts.length == 5) {
            int index = Integer.parseInt(parts[0]);
            boolean paused = parts[1].equals("true");
            Interval delay = new Interval(parts[2]);
            HMessage.Direction direction = parts[3].equals(HMessage.Direction.TOSERVER.name()) ? HMessage.Direction.TOSERVER : HMessage.Direction.TOCLIENT;
            String packetAsString = parts[4];

            construct(index, paused, delay, packetAsString, direction);
        }
    }

}
