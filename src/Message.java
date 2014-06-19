import java.io.Serializable;
import java.util.Comparator;

/**
 * Created by swair on 6/17/14.
 */
public class Message implements Serializable, Comparable<Message> {
    final int from,to;
    final int clock;
    final String type;
    final String content;
    private Message(MessageBuilder builder) {
        this.from = builder.from; this.to = builder.to;
        this.clock = builder.clock;
        this.type = builder.type;
        this.content = builder.content;
    }
    @Override
    public int compareTo(Message that) {
        if      (this.clock > that.getClock()) return +1;
        else if (this.clock < that.getClock()) return -1;
        else { //Break ties with pid;
            if(this.getSender() > that.getSender()) return +1;
            else return -1;
            //PID SHOULD NEVER BE EQUAL
        }
    }

    public int getSender() {return from;}
    public int getReceiver() {return to;}
    public int getClock() {return clock;}
    public String getType() {return type;}
    public String getContent() {return content;}

    public static class MessageBuilder {
        private int to,from;
        private int clock;
        private String type;
        private String content = "";
        public MessageBuilder() {}
        public MessageBuilder from(int from) {this.from = from; return this;}
        public MessageBuilder to(int to) {this.from = to; return this;}
        public MessageBuilder clock(int clock) {this.clock = clock; return this;}
        public MessageBuilder type(String type) {this.type = type; return this;}
        public MessageBuilder content(String content) {this.content = content; return this;}
        public Message build() {return new Message(this);}
    }

    public static void main(String[] args) {
        Message msg = new Message.MessageBuilder()
                .to(1)
                .from(0)
                .clock(11)
                .type("application").build();
        System.out.println(msg.getClock());
    }
}
