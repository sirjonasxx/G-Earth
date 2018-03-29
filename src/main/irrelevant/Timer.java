package main.irrelevant;

public class Timer {
    private long    time0 = System.nanoTime(), time1 = time0;
    private boolean ended = false;
    public void     start() { time0 = System.nanoTime(); ended = false; }
    public void     end() { time1 = System.nanoTime(); ended = true; }
    public double   delta() {
        if ( ! ended) { end(); }
        return 1e-6 * (time1 - time0); }
}