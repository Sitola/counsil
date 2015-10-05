/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package p2p;

import edu.emory.mathcs.backport.java.util.Collections;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;

/**
 *
 * @author maara
 */
public abstract class NetworkConnector {
    
    static Logger logger = Logger.getLogger(NetworkConnector.class);
    
    private static final boolean logCommunication = false;
    private static final long sessionStart = System.currentTimeMillis();
    private static PrintStream communicationLogStream = null;
    static {
        if (logCommunication) {
            try {
                long now = System.currentTimeMillis();
                String fileName = "communicationLog"+(new SimpleDateFormat(".dd-MM-yy.HH-mm").format(new Date(now)))+".csv";
                PrintStream ps = new PrintStream(new File(fileName));
                
                ps.print("Time, Channel, Direction, Message type, Sender, Receiver, Content\n");
                communicationLogStream = ps;
            } catch (FileNotFoundException ex) {
                communicationLogStream = null;
            }
        }
    }
    
    public static void logIngoingMessage(String channel, CoUniverseMessage message) {
        if (! logCommunication || communicationLogStream == null) return;
        
        StringBuilder sb = new StringBuilder();
        sb.append(System.currentTimeMillis() - sessionStart).append(", ");
        sb.append(channel).append(", ");
        sb.append("Incoming, ");
        sb.append(message.type.getMsgTypeId()).append(", ");
        sb.append(message.sender == null ? "null" : message.sender.getId()).append(", ");
        sb.append(message.receiver == null ? "null" : message.receiver.getId()).append(", ");
        sb.append(Arrays.toString(message.content)).append("\n");
        
        communicationLogStream.append(sb);
    }
    
    public static void logOutgoingMessage(String channel, CoUniverseMessage message) {
        if (! logCommunication || communicationLogStream == null) return;
        
        StringBuilder sb = new StringBuilder();
        sb.append(System.currentTimeMillis() - sessionStart).append(", ");
        sb.append(channel).append(", ");
        sb.append("Outgoing, ");
        sb.append(message.type.getMsgTypeId()).append(", ");
        sb.append(message.sender == null ? "null" : message.sender.getId()).append(", ");
        sb.append(message.receiver == null ? "null" : message.receiver.getId()).append(", ");
        sb.append(Arrays.toString(message.content)).append("\n");
        
        communicationLogStream.append(sb);
    }
    
    
    
    private final ConnectorID connectorID;
    
    private class ReceivingThread extends Thread {

        private volatile boolean terminated = false;
        
        public void terminate() {
            terminated = true;
        }
        
        @Override
        public void run() {
            while (receivingEnabled.get() && (! terminated)) {

                try {
                    CoUniverseMessage message = receivedMessageQueue.take();

                    StringBuilder sb = new StringBuilder("Received message " + message + ". Invoking listeners");
                    if (message == null) continue;

                    logger.debug("Received message " + message + ". Listeners = " + getMessageListeners(message.type));
                    for (MessageListener listener : getMessageListeners(message.type)) {
                        logger.debug("  Invoking listener " + listener);
                        listener.onMessageArrived(message);
                        sb.append("\n    invoked listener: " + listener);
                    }
                    
//                    logger.info(sb);
                } catch (InterruptedException ex) {
                } catch (RuntimeException ex) {
                    logger.error("Receiver thread encountered unexpected exception! Aborting to prevent inconsistency", ex);
                    return;
                }
            }
        }
        
    }
    
    private final ConcurrentHashMap<MessageType, CopyOnWriteArrayList<MessageListener>> messageListenerMap = new ConcurrentHashMap<>();
    // TODO: add ConnectorID-specific listeners?
    
    private final LinkedBlockingQueue<CoUniverseMessage> receivedMessageQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean receivingEnabled = new AtomicBoolean(false);
    private ReceivingThread receivingThread;
    
    private final AtomicBoolean agcAllowed = new AtomicBoolean(false); 
    private AGCMonitorThread agcMonitorThread;
    private AGCCandidateThread agcCandidateThread;
    private AGCController agcController;
    private final Object agcLock = new Object();

    protected NetworkConnector(ConnectorID localNodeID) {
        this.connectorID = localNodeID;
    }
    
    /**
     * Join the universe. After this method has returned, the universe established
     * and other methods can be invoked.
     * @param localNodeID
     * @param allowAGC Is the local node allowed to run AGC, if necessary?
     */
    public final void joinUniverse(boolean allowAGC) {
        joinUniverseInternal();
        
        agcMonitorThread = new AGCMonitorThread(this);
        agcMonitorThread.start();
        
        if (allowAGC && (getConnectorID() != null) ) {
            agcAllowed.set(true);
            agcCandidateThread = new AGCCandidateThread(this);
            agcCandidateThread.start();
        }
        
        startReceiving();
    }
    
    protected abstract void joinUniverseInternal();
    
    public abstract void leaveUniverseInternal();
    
    /**
     * Terminate all connections and disconnect from the universe
     */
    public final void leaveUniverse() {
        receivingThread.terminate();
        if (agcMonitorThread != null) agcMonitorThread.terminate();
        if (agcCandidateThread != null) agcCandidateThread.terminate();

        leaveUniverseInternal();
    }
    
    /**
     * Send the given object to AGC.
     * @param message 
     * @throws java.io.IOException Upon failure of sending the message
     */
    public void sendMessageToAgc(CoUniverseMessage message) throws IOException {

// TODO: The pipe mechanism is somehow broken; Fix it and use pipes to send messages to AGC.        
//        try {
//            ConnectorID agcID = agcMonitorThread.getAgcConnectorID();
//
//            if (agcID == null) return;
//
//            message.receiver = agcID;
//
//            // Duplicate pipe creation calls are ignored, therefore the call is safe
//            createClientPipe(agcID);
//
//            logger.debug("Sending message " + message.type.getMsgTypeId() + " to AGC at node " + agcID);
//
//            sendMessageToNode(message);
//        } catch (IOException ex) {
//            System.out.println("Exception raised: " + ex);
//            throw ex;
//        }

        sendMessageToGroup(message, NodeGroupIdentifier.AGC_CAPABLE_NODES);
    }

    /**
     * Broadcast a message to all nodes within the specified group of interest.
     * @param message Message to be sent
     * @param targetGroupIdentifier Identifier 
     */
    public abstract void sendMessageToGroup(CoUniverseMessage message, NodeGroupIdentifier targetGroupIdentifier);
    
    /**
     * Send message to a particular network node. A client pipe to that node 
     *   must have been created previously.
     * @param message Message to be sent. Its receiver attribute determines target of this message
     */
    public abstract void sendMessageToNode(CoUniverseMessage message) throws IOException;
    
    /**
     * Send message to all nodes connected as clients to the local pipe server.
     *   The pipe server must be running for this node
     * @param message Message to send to all clients
     */
    public abstract void sendMessageToClients(CoUniverseMessage message);
    
    /**
     * Start receiving messages destined for the specified NodePeerGroup
     * @param groupId Identification of the group to be listened to
     * @return true on success
     */
    public abstract void startReceivingFromGroup(NodeGroupIdentifier groupId);

    /**
     * Start the pipe server for local node. Other nodes can connect to this pipe
     *   and send messages to this node directly.
     * @throws IOException 
     */
    public abstract void startLocalPipeServer() throws IOException;
    
    /**
     * Stop pipe server for the local node. All pipes opened for this node as 
     *   server will be closed. Pipes created as clients remain opened.
     */
    public abstract void stopLocalPipeServer();
    
    /**\
     * Create pipe to the specified node. The pipe server must be running on
     *   the target node
     * @param id ConnectorID of the node running the pipe server to connect to
     * @throws IOException 
     */
    public abstract void createClientPipe(ConnectorID id) throws IOException;
    
    /**
     * Add listener for a specified message type
     * @param type Message type to register listener to
     * @param listener Listener to be registered
     */
    public void addMessageListener(MessageType type, MessageListener listener) {
        if (messageListenerMap.get(type) == null) messageListenerMap.put(type, new CopyOnWriteArrayList<MessageListener>());
        messageListenerMap.get(type).add(listener);
    }
    
    /**
     * Add listener to all known message types. The listener will be called 
     * every time a recognizable message is received.
     * @param listener Listener to be added
     */
    public void addGlobalMessageListener(MessageListener listener) {
        for (MessageType type : MessageType.values()) {
            addMessageListener(type, listener);
        }
    }
    
    /**
     * Remove the specified listener for a specified message type
     * @param listener Listener to be removed
     * @param type Message type the listener should not be invoked by anymore.
     * @return True if the listener was found and successfully removed
     */
    public void removeMessageListener(MessageListener listener, MessageType type) {
        if (messageListenerMap.get(type) == null) return;
        messageListenerMap.get(type).remove(listener);
    }
    
    /**
     * Unbind the listener from all known message types at once
     * @param listener Listener not to be invoked anymore
     * @return List of message types the listener was successfully unbound from
     */
    public void removeGlobalMessageListener(MessageListener listener) {
        for (MessageType type : MessageType.values()) {
            removeMessageListener(listener, type);
        }
    }
    
    /**
     * Get all message listeners for the specified MessageType
     * @param type
     * @return List of all <t>MessageListeners</t> for the specified <t>MessageType</t>
     */
    List<MessageListener> getMessageListeners(MessageType type) {
        List<MessageListener> ret = messageListenerMap.get(type);
        if (ret == null) return new LinkedList<>();
        return Collections.unmodifiableList(ret);
    }
    
    /**
     * Start the message receiving mechanism. Before starting it, the 
     * receiveMessage(...) method throws IllegalStateException
     */
    public void startReceiving() {
        if (receivingEnabled.get()) return;
        
        synchronized (receivingEnabled) {
            
            if (receivingEnabled.get()) return;
            
            receivingThread = new ReceivingThread();
            
            receivingEnabled.set(true);
            receivingThread.start();
        }
        
        startReceivingFromGroup(NodeGroupIdentifier.ALL_NODES);
    }
    
    /**
     * Stop the receiving mechanism. While receiving is stopped (default state), 
     * the receiveMessage(...) method throws IllegalStateException
     */
    public void stopReceiving() {
        if (! receivingEnabled.get()) return;
        
        synchronized(receivingEnabled) {
            if (! receivingEnabled.get()) return;
            
            receivingThread.terminate();
            receivingEnabled.set(false);
            receivingThread.interrupt();
        }
        
    }
    
    /**
     * Pass the message to all appropriate listeners.
     * @param message Message tht was received
     * @return List of invoked listeners
     */
    public void receiveMessage(CoUniverseMessage message) {
        
        if (! receivingEnabled.get()) {
            throw new IllegalStateException("Receiving messages not allowed! Enable it by startReceiving() first");
        }
        
        if (message.sender == null) {
            logger.warn("Received message has no sender set! (" + message + ")");
        }

        // TODO: add the shout-and-echo module here;
        
        if (! receivedMessageQueue.offer(message)) {
            logger.warn("Failed to add received message to the processing queue! message = " + message);
        }
    }
    
    public void allowAgc() {
        agcAllowed.set(true);
    }
    
    public void denyAgc() {
        agcAllowed.set(false);
        
        // TODO: if running, terminate AGC
    }
    
    protected void startAGC() {
        if (! isAGCEnabled()) {
            throw new UnsupportedOperationException("The local node is not allowed to run AGC");
        }

        if (agcController != null) {
            // AGC is already running
            return;
        }
        
        if (getConnectorID() == null) throw new NullPointerException("Cannot start AGC thread with null connector!");
        
        synchronized (agcLock) {
            if (agcController != null) return;
            
            agcController = new AGCController(this);
            agcController.start();
        }
    }
    
    protected void stopAGC() {
        agcController.stop();
    }
    
    public ConnectorID getConnectorID() {
        return this.connectorID;
    }
    
    public boolean isAGCRunning() {
        return false;
        // TODO: check
    }
    
    public boolean isAGCEnabled() {
        return agcAllowed.get();
    }
    
    public ConnectorID getAGCConnectorID() {
        return agcMonitorThread.getAgcConnectorID();
    }
}
