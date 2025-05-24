package gearth.misc.listenerpattern;

import java.util.function.Consumer;

public class ObservableObject<T> extends Observable<Consumer<T>> {

    private T object;

    public ObservableObject(T object) {
        super();
        this.object = object;
    }
    
    public void setObject(T object) {
        this.object = object;
        fireEvent(c -> c.accept(object));
    }

    public T getObject() {
        return object;
    }
}
