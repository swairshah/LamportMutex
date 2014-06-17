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

    public void local_event() { clock += d; }

    public void msg_event(int msg_clock) {
        this.local_event();
        if (msg_clock >= this.clock) {
            clock = msg_clock;
        }
    }

    public int peek() { return clock; }
}
