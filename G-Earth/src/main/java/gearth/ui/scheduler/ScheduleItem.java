package gearth.ui.scheduler;

import gearth.misc.listenerpattern.Observable;
import gearth.ui.scheduler.listeners.OnBeingUpdatedListener;
import gearth.ui.scheduler.listeners.OnDeleteListener;
import gearth.ui.scheduler.listeners.OnEditListener;
import gearth.ui.scheduler.listeners.OnUpdatedListener;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import gearth.misc.StringifyAble;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jonas on 07/04/18.
 */
public class ScheduleItem implements StringifyAble {

    private SimpleIntegerProperty indexProperty;
    private SimpleBooleanProperty pausedProperty;
    private SimpleObjectProperty<Interval> delayProperty;
    private SimpleObjectProperty<HPacket> packetProperty;
    private SimpleObjectProperty<HMessage.Direction> destinationProperty;

    ScheduleItem (int index, boolean paused, Interval delay, HPacket packet, HMessage.Direction destination) {
        construct(index, paused, delay, packet, destination);
    }

    private void construct(int index, boolean paused, Interval delay, HPacket packet, HMessage.Direction destination) {
        this.indexProperty = new SimpleIntegerProperty(index);
        this.pausedProperty = new SimpleBooleanProperty(paused);
        this.delayProperty = new SimpleObjectProperty<>(delay);
        this.packetProperty = new SimpleObjectProperty<>(packet);
        this.destinationProperty = new SimpleObjectProperty<>(destination);
    }

    ScheduleItem(String stringifyAbleRepresentation) {
        constructFromString(stringifyAbleRepresentation);
    }

    public SimpleIntegerProperty getIndexProperty() {
        return indexProperty;
    }

    public SimpleBooleanProperty getPausedProperty() {
        return pausedProperty;
    }

    public SimpleObjectProperty<Interval> getDelayProperty() {
        return delayProperty;
    }

    public SimpleObjectProperty<HPacket> getPacketProperty() {
        return packetProperty;
    }

    public SimpleObjectProperty<HMessage.Direction> getDestinationProperty() {
        return destinationProperty;
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
        b       .append(indexProperty.get())
                .append("\t")
                .append(pausedProperty.get() ? "true" : "false")
                .append("\t")
                .append(delayProperty.get().toString())
                .append("\t")
                .append(packetProperty.get().toString())
                .append("\t")
                .append(destinationProperty.get().name());
        return b.toString();
    }

    @Override
    public void constructFromString(String str) {
        String[] parts = str.split("\t");
        if (parts.length == 5) {
            int index = Integer.parseInt(parts[0]);
            boolean paused = parts[1].equals("true");
            Interval delay = new Interval(parts[2]);
            HPacket packet = new HPacket(parts[3]);
            HMessage.Direction direction = parts[4].equals(HMessage.Direction.TOSERVER.name()) ? HMessage.Direction.TOSERVER : HMessage.Direction.TOCLIENT;

            construct(index, paused, delay, packet, direction);

        }
    }
}
