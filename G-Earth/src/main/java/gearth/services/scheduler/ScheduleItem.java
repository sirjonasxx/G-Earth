package gearth.services.scheduler;

import gearth.misc.listenerpattern.Observable;
import gearth.services.scheduler.listeners.OnBeingUpdatedListener;
import gearth.services.scheduler.listeners.OnDeleteListener;
import gearth.services.scheduler.listeners.OnEditListener;
import gearth.services.scheduler.listeners.OnUpdatedListener;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import gearth.misc.StringifyAble;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.beans.property.SimpleStringProperty;

/**
 * Created by Jonas on 07/04/18.
 */
public class ScheduleItem {

    private SimpleIntegerProperty indexProperty;
    private SimpleBooleanProperty pausedProperty;
    private SimpleObjectProperty<Interval> delayProperty;
    private SimpleObjectProperty<HMessage.Direction> destinationProperty;
    private SimpleStringProperty packetAsStringProperty;


    public ScheduleItem() {}
    public ScheduleItem (int index, boolean paused, Interval delay, String packetAsString, HMessage.Direction destination) {
        construct(index, paused, delay, packetAsString, destination);
    }

    protected void construct(int index, boolean paused, Interval delay, String packetAsString, HMessage.Direction destination) {
        this.indexProperty = new SimpleIntegerProperty(index);
        this.pausedProperty = new SimpleBooleanProperty(paused);
        this.delayProperty = new SimpleObjectProperty<>(delay);
        this.packetAsStringProperty = new SimpleStringProperty(packetAsString);
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

    public SimpleStringProperty getPacketAsStringProperty() {
        return packetAsStringProperty;
    }

    public SimpleObjectProperty<HMessage.Direction> getDestinationProperty() {
        return destinationProperty;
    }
}
