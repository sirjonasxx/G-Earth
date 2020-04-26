package gearth.ui.scheduler;

import gearth.misc.listenerpattern.Observable;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.services.scheduler.Interval;
import gearth.services.scheduler.ScheduleItem;
import gearth.services.scheduler.listeners.OnBeingUpdatedListener;
import gearth.services.scheduler.listeners.OnDeleteListener;
import gearth.services.scheduler.listeners.OnEditListener;
import gearth.services.scheduler.listeners.OnUpdatedListener;

public class InteractableScheduleItem extends ScheduleItem {
    public InteractableScheduleItem(int index, boolean paused, Interval delay, HPacket packet, HMessage.Direction destination) {
        super(index, paused, delay, packet, destination);
    }

    public InteractableScheduleItem(String stringifyAbleRepresentation) {
        super(stringifyAbleRepresentation);
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

}
