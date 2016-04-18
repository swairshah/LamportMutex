/**
 * Created by swair on 6/17/14.
 */
public class LamportClock {
    private int d  = 1;
    private int clock = 0;
    public LamportClock() {}
    public LamportClock(int d) {
        this.d = d;
    }

    public synchronized void local_event() { clock += d; }

    public synchronized void msg_event(int msg_clock) {
        this.local_event();
        if (msg_clock + d >= this.clock) {
            clock = msg_clock + d;
        }
    }
    public int increment() {return d;}
    public int peek() { return clock; }
}
