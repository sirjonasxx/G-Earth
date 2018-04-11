package main.ui.scheduler;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import main.protocol.HMessage;
import main.protocol.HPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jonas on 07/04/18.
 */
public class ScheduleItem {

    private SimpleIntegerProperty indexProperty;
    private SimpleBooleanProperty pausedProperty;
    private SimpleObjectProperty<Interval> delayProperty;
    private SimpleObjectProperty<HPacket> packetProperty;
    private SimpleObjectProperty<HMessage.Side> destinationProperty;

    ScheduleItem (int index, boolean paused, Interval delay, HPacket packet, HMessage.Side destination) {
        this.indexProperty = new SimpleIntegerProperty(index);
        this.pausedProperty = new SimpleBooleanProperty(paused);
        this.delayProperty = new SimpleObjectProperty<>(delay);
        this.packetProperty = new SimpleObjectProperty<>(packet);
        this.destinationProperty = new SimpleObjectProperty<>(destination);
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

}
