package gearth.ui.scheduler;

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

    private SimpleIntegerProperty indexProperty = null;
    private SimpleBooleanProperty pausedProperty = null;
    private SimpleObjectProperty<Interval> delayProperty = null;
    private SimpleObjectProperty<HPacket> packetProperty = null;
    private SimpleObjectProperty<HMessage.Side> destinationProperty = null;

    ScheduleItem (int index, boolean paused, Interval delay, HPacket packet, HMessage.Side destination) {
        construct(index, paused, delay, packet, destination);
    }

    private void construct(int index, boolean paused, Interval delay, HPacket packet, HMessage.Side destination) {
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

    public SimpleObjectProperty<HMessage.Side> getDestinationProperty() {
        return destinationProperty;
    }


    private List<InvalidationListener> onDeleteListeners = new ArrayList<>();
    public void onDelete(InvalidationListener listener) {
        onDeleteListeners.add(listener);
    }
    public void delete() {
        for (int i = onDeleteListeners.size() - 1; i >= 0; i--) {
            onDeleteListeners.get(i).invalidated(null);
        }
    }

    private List<InvalidationListener> onEditListeners = new ArrayList<>();
    public void onEdit(InvalidationListener listener) {
        onEditListeners.add(listener);
    }
    public void edit() {
        for (int i = onEditListeners.size() - 1; i >= 0; i--) {
            onEditListeners.get(i).invalidated(null);
        }
    }

    private List<InvalidationListener> onIsupdatedListeners = new ArrayList<>();
    public void onIsupdated(InvalidationListener listener) {
        onIsupdatedListeners.add(listener);
    }
    public void isUpdatedTrigger() {
        for (int i = onIsupdatedListeners.size() - 1; i >= 0; i--) {
            onIsupdatedListeners.get(i).invalidated(null);
        }
    }

    private List<InvalidationListener> OnIsBeingUpdatedListeners = new ArrayList<>();
    public void onIsBeingUpdated(InvalidationListener listener) {
        OnIsBeingUpdatedListeners.add(listener);
    }
    public void onIsBeingUpdatedTrigger() {
        for (int i = OnIsBeingUpdatedListeners.size() - 1; i >= 0; i--) {
            OnIsBeingUpdatedListeners.get(i).invalidated(null);
        }
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
            HMessage.Side side = parts[4].equals(HMessage.Side.TOSERVER.name()) ? HMessage.Side.TOSERVER : HMessage.Side.TOCLIENT;

            construct(index, paused, delay, packet, side);

        }
    }
}
