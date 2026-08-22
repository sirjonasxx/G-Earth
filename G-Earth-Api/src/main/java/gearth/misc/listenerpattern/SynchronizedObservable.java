package gearth.misc.listenerpattern;

import java.util.function.Consumer;

public class SynchronizedObservable<Listener> extends Observable<Listener> {

    public SynchronizedObservable(Consumer<Listener> defaultConsumer) {
        super(defaultConsumer);
    }

    public SynchronizedObservable() {
        super();
    }

    @Override
    public void fireEvent(Consumer<Listener> consumer) {
        synchronized (this) {
            super.fireEvent(consumer);
        }
    }

}
