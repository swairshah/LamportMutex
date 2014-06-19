
import sun.misc.resources.Messages_sv;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Created by swair on 6/17/14.
 */
public class Node implements Runnable {
    private class NodeLookup {
        public HashMap<String,String> table;
        public NodeLookup(HashMap<String,String> table) {
            this.table = table;
        }

        public String getIP(int pid) {
            String ip_port = table.get(Integer.toString(pid));
            return ip_port.split(":")[0];
        }

        public int getPort(int pid) {
            String ip_port = table.get(Integer.toString(pid));
            return Integer.parseInt(ip_port.split(":")[1]);
        }
    }
    private LamportClock localclock;
    private LamportMutex mutex;
    private int pid;
    private int port;
    private boolean requested_crit = false;
    private NodeLookup lookup; // "pid" -> "ip:port"
    public final List<Integer> other_pids;

    public Node(int pid, String ConfigFile) {
        this.pid = pid;
        this.lookup = new NodeLookup(ConfigReader.getLookup(ConfigFile));
        this.port = lookup.getPort(pid);
        this.localclock = new LamportClock();
        this.other_pids = new ArrayList<Integer>();
        for(String i :lookup.table.keySet()) {
            int id = Integer.parseInt(i);
            if(id!=this.pid) {
                other_pids.add(id);
            }
        }
        this.mutex = new LamportMutex(this);
    }

    public int getPid() { return this.pid; }
    public int getPort() { return this.port;}

    public void run_listener() {
        Thread listener = new NodeListenerThread(this);
        listener.start();
    }

    public void deliver_message(Message msg) {
        this.localclock.msg_event(msg.getClock());
        //System.out.println(msg.getType()+" from: "+msg.getSender());

        if      (msg.getType() == "request") {
            mutex.queue_request(msg);
            /*
            in version 1, send reply to all requests whatsoever
             */
            send_message(msg.getSender(),"reply");
        }
        else if (msg.getType() == "release") {
            mutex.release_request(msg);
        }
        else if (msg.getType() == "reply") {
            mutex.reply_request(msg);
        }
    }

    public void send_message(int receiver, String type) {
        //System.out.println("sending message from:"+ pid+" to "+receiver);o
        Message msg = new Message.MessageBuilder()
                .to(receiver)
                .from(this.pid)
                .clock(this.localclock.peek())
                .type("application").build();
        String receiver_ip = lookup.getIP(receiver);
        int receiver_port = lookup.getPort(receiver);

        try (Socket sock = new Socket(receiver_ip, receiver_port)) {
            OutputStream out = sock.getOutputStream();
            ObjectOutputStream outstream = new ObjectOutputStream(out);
            outstream.writeObject(msg);
            outstream.close();
            out.close();
            sock.close();

            this.localclock.local_event();

        } catch (IOException ex) {
            System.err.println("can't send message" + ex);
        }
    }

    public void multicast(String type) {
        for(String pid_str: lookup.table.keySet()) {
            int pid_int = Integer.parseInt(pid_str);
            if (pid_int == this.pid) {
                continue;
            }
            else {
                send_message(pid_int,type);
            }
        }
    }

    private void execute_crit() {
        try {
            Thread.sleep(200);
        } catch(InterruptedException ex) {}
    }

    @Override
    public void run() {
        try {
            Thread.sleep(3000);//till I start other processes;
        } catch (InterruptedException ex) {}

        run_listener();
        while(true) {
            Random rand = new Random();
            int sleeptime = rand.nextInt(101 - 10) + 10;

            try {
                Thread.sleep(sleeptime*10);
            } catch (InterruptedException ex) {
                System.err.println(ex);
            }

            int decider = rand.nextInt(101 - 1) + 1;
            if (decider >= 1 && decider <= 90) {
                multicast("application");
            } else {
                multicast("request");
                while(!mutex.request_crit_section()) {
                    System.out.println("waiting for crit section");
                }
                execute_crit();
                multicast("release");
                mutex.release_request();
            }
        }
    }

    public static void main(String[] args) {
        //System.out.println(args[1]);
        Node n = new Node(Integer.parseInt(args[0]),args[1]);
        Thread t = new Thread(n);
        t.start();
    }
}
