import java.util.ArrayList;
import java.util.Collections;
import java.util.InputMismatchException;
import java.util.List;

/**
 * Created by swair on 6/17/14.
 */
public class LamportMutex {
    private Node node_ref;
    List<Message> requestList;

    private boolean request_made;
    public List<Integer> pending_replies;

    public LamportMutex(Node n) {
        node_ref = n;
        requestList = Collections.synchronizedList(new ArrayList<Message>() {
            public synchronized boolean add(Message msg) {
                boolean ret = super.add(msg);
                Collections.sort(requestList);
                return ret;
            }
        });
    }

    public synchronized void queue_request(Message msg) {
        requestList.add(msg);
    }

    public synchronized void release_request(Message msg) {
        int pid = msg.getSender();
        /* the first msg in the list should be the one
           to be released. otherwise something is wrong
         */
        if (requestList.get(0).getSender() == pid) {
            requestList.remove(0);
        }
        else {
            System.err.println("release message wasn't from the process first in the queue");
        }
    }

    public synchronized void release_request() {
        /*
        this is called when the local node
        is done executing crit section
         */
        request_made = false;
        requestList.remove(0);
    }


    public synchronized boolean request_crit_section() {
        /* on true, node can enter the crit section,
           on false node can not. and then node blocks exec
           till it gets critical section.
        */
        if (!request_made) {
            request_made = true;
            pending_replies = new ArrayList<>(node_ref.other_pids);
            /*
            we need our data structures prepared before we start getting
            replies from other nodes!,
            so once that is done, multicast request.
             */
            node_ref.multicast("request");
        }
        if(requestList.get(0).getSender() == node_ref.getPid()) {
            /* we're highest priority!, now wait for all replies */
            if (pending_replies.isEmpty()) {
                /*
                Go ahead! execute critical section
                after you're done, call release_request with no arguments
                 */
                return true;
            }
        }
        return false;
    }

    public synchronized void reply_request(Message msg) {
        if(request_made) {
            System.out.println("pending replies: "+pending_replies);
            System.out.println("request queue:"+requestList);
            pending_replies.remove(new Integer(msg.getSender()));
        }
        else {
            System.err.println("got reply but we don't have a pending request, something is wrong");
        }
    }

    public static void main(String[] args) {
        /*
        Message m1 = new Message.MessageBuilder()
                .clock(6).build();
        Message m2 = new Message.MessageBuilder()
                .clock(2).build();
        Message m3 = new Message.MessageBuilder()
                .clock(5).from(0).build();
        LamportMutex mutex = new LamportMutex();
        mutex.requestList.add(m1);
        mutex.requestList.add(m2);
        mutex.requestList.add(m3);
        mutex.requestList.remove(m2);
        for(Message m:mutex.requestList) {
            System.out.println(m.getClock());
        }
        */
    }

}
