package gearth.app.extensions.extra.tools;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Predicate;

public class AwaitingPacket {
    private final String headerName;
    private final HMessage.Direction direction;
    private HPacket packet;
    private boolean received = false;
    private final ArrayList<Predicate<HPacket>> conditions = new ArrayList<>();
    private final long start;
    private long minWait = 0;

    public AwaitingPacket(String headerName, HMessage.Direction direction, long maxWaitingTimeMillis) {
        if (maxWaitingTimeMillis < 30) {
            maxWaitingTimeMillis = 30;
        }

        AwaitingPacket self = this;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                self.received = true;
            }
        }, maxWaitingTimeMillis);

        this.start = System.currentTimeMillis();

        this.direction = direction;
        this.headerName = headerName;
    }

    public String getHeaderName() {
        return this.headerName;
    }

    public HMessage.Direction getDirection() {
        return this.direction;
    }

    public AwaitingPacket setMinWaitingTime(long millis) {
        this.minWait = millis;

        return this;
    }

    @SafeVarargs
    public final AwaitingPacket addConditions(Predicate<HPacket>... conditions) {
        this.conditions.addAll(Arrays.asList(conditions));

        return this;
    }

    protected void setPacket(HPacket packet) {
        this.packet = packet;
        this.received = true;
    }

    protected HPacket getPacket() {
        return this.packet;
    }

    protected boolean test(HMessage hMessage) {
        for (Predicate<HPacket> condition : this.conditions) {
            hMessage.getPacket().resetReadIndex();
            if(!condition.test(hMessage.getPacket())) {
                return false;
            }
        }

        return true;
    }

    protected boolean isReady() {
        return this.received && this.start + this.minWait < System.currentTimeMillis();
    }
}
