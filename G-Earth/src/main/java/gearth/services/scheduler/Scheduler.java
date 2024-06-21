package gearth.services.scheduler;

import gearth.extensions.parsers.HDirection;
import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.HPacketFormat;
import gearth.protocol.connection.HClient;
import gearth.protocol.connection.HState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Scheduler<T extends ScheduleItem> {

    private List<T> scheduleItems = new ArrayList<>();

    public Scheduler(final HConnection connection) {
        new Thread(() -> {
            long t = System.currentTimeMillis();
            long changed = 1;

            Set<ScheduleItem> set = new HashSet<>();

            while (true) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (connection.getState() != HState.CONNECTED) {
                    continue;
                }

                final HClient clientType = connection.getClientType();

                set.clear();
                for (int i = size() - 1; i >= 0; i--) {
                    set.add(get(i));
                }

                for (ScheduleItem item : set) {
                    if (!item.getPausedProperty().get()) {
                        Interval cur = item.getDelayProperty().get();
                        for (int i = 0; i < changed; i++) {
                            if ((t - i) % cur.getDelay() == cur.getOffset()) {
                                final HMessage.Direction direction = item.getDestinationProperty().get();
                                final HPacketFormat format = HPacketFormat.getFormat(clientType, direction);
                                final HPacket hPacket = format.createPacket(item.getPacketAsStringProperty().get());

                                if (direction == HMessage.Direction.TOSERVER) {
                                    connection.sendToServer(hPacket);
                                } else {
                                    connection.sendToClient(hPacket);
                                }
                            }
                        }
                    }
                }

                long newT = System.currentTimeMillis();
                changed = newT - t;
                t = newT;
            }
        }).start();
    }

    public int size() {
        return scheduleItems.size();
    }

    public T get(int i) {
        return scheduleItems.get(i);
    }

    public void add(T item) {
        scheduleItems.add(item);
    }

    public void remove(T item) {
        scheduleItems.remove(item);
    }
}
