package gearth.services.scheduler;

import gearth.ui.scheduler.SchedulerController;

/**
 * Created by Jonas on 11/04/18.
 */
public class Interval {

    private int offset;
    private int delay;

    public Interval(int offset, int delay) {
        this.offset = offset;
        this.delay = delay;
    }

    public Interval(String interval) {
        String[] split = interval.split("\\+");

        if (split.length == 0 || split.length > 2) {
            delay = -1;
            offset = -1;
            return;
        }
        if (!SchedulerController.stringIsNumber(split[0]) || (split.length == 2 && !SchedulerController.stringIsNumber(split[1]))) {
            delay = -1;
            offset = -1;
            return;
        }

        delay = Integer.parseInt(split[0]);
        offset = split.length == 2 ? Integer.parseInt(split[1]) : 0;

        if (delay <= 0 || offset < 0 || offset > delay) {
            delay = -1;
            offset = -1;
        }
    }


    public int getDelay() {
        return delay;
    }

    public int getOffset() {
        return offset;
    }

    public String toString() {
        return delay + "+" + offset;
    }

    public static boolean isValid(String s) {
        Interval test = new Interval(s);
        return (test.delay != -1);
    }
}
