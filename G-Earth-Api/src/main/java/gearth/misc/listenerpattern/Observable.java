package gearth.misc.listenerpattern;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Observable<Listener> {

    private Consumer<Listener> defaultConsumer = null;

    public Observable(Consumer<Listener> defaultConsumer) {
        this.defaultConsumer = defaultConsumer;
    }

    public Observable() {
        // no default consumer
        this(listener -> {});
    }

    private List<Listener> listeners = new ArrayList<>();
    public void addListener(Listener listener) {
        listeners.add(listener);
    }
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }
    public void fireEvent(Consumer<Listener> consumer) {
        for (int i = listeners.size() - 1; i >= 0; i--) {
            consumer.accept(listeners.get(i));
        }
    }
    public void fireEvent() {
        fireEvent(defaultConsumer);
    }

}
