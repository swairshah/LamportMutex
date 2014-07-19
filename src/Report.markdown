% Lamport's Mutual Exclution Algorithm Project
% Swair Shah
% 29-Jun-2014

## Code Overview

### LampotClock
The class LamportClock implements the logical clock functionality. Node has an
instance variable for its own LamportClock. Node calls `local_event` and 
`msg_event(msg)` methods on its LamportClock whenever necessary. 
LamportClock can be declared with a custom 'd' value. with `new LamportClock(d)`.

### LamportMutex Algorithm
This class implements the mutex algorithm. Node has an instance variable for its own
mutex and node passes along request,reply,release messages to its mutex.
Mutex has a modified ArrayList to store sorted request messages.
A priority queue works better but I went with an ArrayList so that I can follow
the execution by printing the queue at every request/release. and anticipate
critical section execution based on release message sequence.

In order to change to a PriorityQueue, just change the instance variable,
`requestList` and inside the way to pop the `requestList` in
`release_request` method. and the same code works fine with a Priority Queue.

When a node requests critical section to its mutex, the mutex initializes 
a `pending_replies` list. The node execution blocks meanwhile, but the node
does accept all the messages sent to it. The node goes into a while loop
until its granted critical section, this only happens when node's own
request is at the top of the `requestList` and there are no pending replies
in the `pending_replies` list.

### Node
Node class extends thread and has a run method, which does the processing.
Based on a random number generations, node decides to either multicast
application message, or request critical section.

Node has a listener thread to listen for incoming messages. This thread
has a reference to node and upon receipt of any message it calls the
`deliver_message` method of the `node_ref`. So even when the main thread of
node is busy executing or waiting for critical section, all the incoming
messages are delivered.

when node sends a message it opens a socket and stores this 
sockets in a HashMap `chan_map`. Every message sends reuses these sockets, if the
socket is closed then node creates a new socket and puts it in the
HashMap

Node's own mutex is stored in an instance variable. All protocol messages
are passed on to the mutex and mutex does the corresponding action depending
upon the type of the message.

### Message
The message class is serializable. The messages sent are serialized objects,
containing receiver, sender, timestamp, type and optionally content (which in
this implementation is not used). Message class has its own instance methods
to get sender, receiver, timestamp and type. 

_Messages are 'comparable' and the comparision is done based on the clock value
in the messages, if the clock is same the tie is broken based on pid
of the senders_

```java
public int compareTo(Message that) {
    if      (this.clock > that.getClock()) return +1;
    else if (this.clock < that.getClock()) return -1;
    else { //Break ties with pid;
        if(this.getSender() > that.getSender()) return +1;
        else return -1;
        //PID SHOULD NEVER BE EQUAL
    }
}
```

### Config File
Config file format is like this:
* pid ipaddress port clockValue

The clock value at the end is optional. If it is absent then
the default clock value is 1 for that node. 

### Log Files
Each node generates a log file. For example node with pid 1 generates,
`node1.log`. This logfile contains total number of protocol messages per
critical section and corresponding time. All nodes also write to a shared 
logfile called shared.log.

### How to compile and run each version?
The code is in a git repository. It has two branches,
`master` branch contains version 1 implementation and 
`Ver2` branch contains version 2. 

```bash
swair-ThinkPad:~/work/systems/LamportMutex/src > git branch -v
* master ae0c125 incorporate custom time increment changes in Node class
  ver2   683a068 version 2,
```

Checkout any of the
branch and compile the project from an IDE (Intellij-Idea is preferred, 
as project was created in the same, thus its straightforward to import it
in Idea IDE), or change the classpath to the src folder and run
```bash 
javac Node.java
```

On any node, config file contains the IP address and pid.

To run, for example node with pid 2:

```bash 
java -jar LamportMutex.jar 2 <configfile>
```
or

```bash
java Node 2 <configfile>
```

## Data Collection and Analysis of Version 1 and 2

### Version 1 and 2 Implementations

In Version 1, for each received request message, the node immediately
sends a reply back. 

For version 2, we maintain a map of pid and latest sent message timestamp
to the node with that pid. At every request message, check this map
if the latest timestamp is greater than that of the request message,
don't send a reply. 

In case when the node is the requestor, it considers __all__ messages
sent to it __after__ its request, as potential reply message and
informs the mutex.

### Data Collection
The code snippet for data collection is below:

```java
long startTime = System.currentTimeMillis();
int proto_messages_before = this.total_protocol_msgs;

//multicast("request"); -- do that inside LamportMutex
while(!mutex.request_crit_section()) {
    //keep checking if we can enter crit or not.
}
long endTime = System.currentTimeMillis();
long duration = (endTime - startTime);
execute_crit();
mutex.release_request();
multicast("release");
int proto_messages_after = this.total_protocol_msgs;
this.delay_per_crit = duration;
this.count_per_crit = proto_messages_after - proto_messages_before;
```
we start a time counter just before requesting critical section.
and keep track of total protocol messages exchanged before this instance.

just after exiting critical section, we check the difference in time
and the difference in exchanged protocol messages and log that to the
node's personal log. Example:

| proto_msgs    | delay_duration
| ------------- |:-------------:
| 27            | 7719
| 26            | 12157
| 27            | 8525

*Data collection code is same in Version 1 and Version 2*.

### Analysis
In version 1, per critical section execution, for an N-node cluster
we expect,

* (n-1) Request Message sent by the node +
* (n-1) Replies from other nodes +
* (n-1) Releaes Messsages sent by the node = 3(n-1) messages

In version 2, any application message or protocol message
sent by other nodes after the request is made, can 
act as a reply. So the number of messages are expected to be
less than `3(n-1)`.

```bash
#!/bin/bash
for i in {0..9}
do tail -21 node$i.log | head -20 | awk '{sum+=$1} END {print sum/NR}'
done | awk '{sum+=$1} END {print sum/NR}'
```

Running the version 2 5 times on a cluster of 10 nodes,
and getting the average messages per node, and averaging
each result gives less than 30. The observed averages in
5 runs were as follows:

1. 26.94
2. 26.81
3. 26.79
4. 26.78
5. 26.86

The average of 5 runs is 26.83, which is less than 27 (the
average in Version 1) as expected.

The shared.log has enter and exit times for critical sections 
for each node. It is evident that no two nodes have intersecting
times and subsequent timestamps are in increasing order as
expected.
