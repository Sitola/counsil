/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package myJXTA;

import edu.emory.mathcs.backport.java.util.Collections;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.jxta.pipe.PipeService;
import net.jxta.socket.JxtaMulticastSocket;
import org.apache.log4j.Logger;
import p2p.CoUniverseMessage;
import p2p.NetworkConnector;
import p2p.NodeGroupIdentifier;

/**
 *
 * @author maara
 */
public class MyJXTAMulticastServices {

    static Logger logger = Logger.getLogger(MyJXTAMulticastServices.class);
    
    public static final int SOCKET_RECEIVE_TIMEOUT = 10000;
    
    private final MyJXTAConnector connector;

    // Read-only; As groups are known at the start, there is no need to write here
    private final Map<NodeGroupIdentifier, JxtaMulticastSocket> multicastSocketMap;
    // Writes are synchronized, reads are not
    private final ConcurrentHashMap<NodeGroupIdentifier, DatagramListenerThread> socketListenerThreads = new ConcurrentHashMap<>();

    private class DatagramListenerThread extends Thread {
        
        private volatile boolean terminated = false;
        private final DatagramSocket socket;
        private final MyJXTAConnector connector;
        private final String socketName;
        
        public DatagramListenerThread(MyJXTAConnector connector, DatagramSocket socket, String socketName) {
            this.socket = socket;
            this.connector = connector;
            this.socketName = socketName;
        }
        
        public void terminate() {
            terminated = true;
            this.interrupt();
        }
        
        @Override
        public void run() {
            
            logger.info("Starting multicast receiving thread for socket " + socket);
            
            try {
                socket.setSoTimeout(SOCKET_RECEIVE_TIMEOUT);
            } catch (SocketException ex) {
                logger.warn("Failed to set timeout of a socket!");
            }
            
            while (! terminated) {

                byte buffer[] = new byte[MyJXTAConnector.MAX_DATAGRAM_SIZE_BYTES];
                DatagramPacket packet = new DatagramPacket(buffer, MyJXTAConnector.MAX_DATAGRAM_SIZE_BYTES);
                
                ByteArrayInputStream bis = null;
                ObjectInput in = null;

                try {
                    socket.receive(packet);
                    
                    if (terminated) return;

                    bis = new ByteArrayInputStream(packet.getData());
                    in = new ObjectInputStream(bis);

                    Object message = in.readObject();
                    
                    if (! (message instanceof CoUniverseMessage)) {
                        logger.warn("Received object not instance of CoUniverseMessage (is actually " + message.getClass() + "); Ignoring message");
                    } else {
                        NetworkConnector.logIngoingMessage(socketName, (CoUniverseMessage) message);
                        connector.receiveMessage((CoUniverseMessage) message);
                    }

                } catch (IOException | ClassNotFoundException ex) {
                    logger.error("Multicast receiver thread encountered an exception;");
                    logger.error(ex);
                    ex.printStackTrace(System.out);
                } finally {
                    
                    try {
                        if (bis != null) bis.close();
                        if (in != null) in.close();
                    } catch (IOException ex) {}
                }                
            }
            
        }
        
    }
    
    public MyJXTAMulticastServices(MyJXTAConnector connector) throws IOException {
        this.connector = connector;
        HashMap<NodeGroupIdentifier, JxtaMulticastSocket> tempMulticastSocketMap = new HashMap<>();
        
        // Create multicast sockets for all known types
        // TODO: lazy initialization might be better
        for (NodeGroupIdentifier nid : NodeGroupIdentifier.values()) {
            JxtaMulticastSocket socket = new JxtaMulticastSocket(
                connector.getUniversePeerGroup(), 
                MyJXTAUtils.getPipeAdvertisement(
                        connector.getUniversePeerGroup(), 
                        nid.identifier,
                        PipeService.PropagateType));
            tempMulticastSocketMap.put(nid, socket);
        }
        
        multicastSocketMap = Collections.unmodifiableMap(tempMulticastSocketMap);
    }
    
    public NetworkConnector getConnector() {
        return connector;
    }
    
    public void startListeningToGroup(NodeGroupIdentifier groupID) {
        
        synchronized(socketListenerThreads) {
        
            if (socketListenerThreads.get(groupID) != null) {
                logger.debug("Attempted to start listener thread for group " + groupID + ": Listener already started, ignoring repeated invocation");
                return;
            }
            
            DatagramListenerThread listener = new DatagramListenerThread(
                    connector,
                    multicastSocketMap.get(groupID), 
                    "Group " + groupID
            );

            listener.start();

            socketListenerThreads.put(groupID, listener);
        }
    }
    
    public void stopListeningToGroup(NodeGroupIdentifier groupID) {
        
        synchronized(socketListenerThreads) {
            DatagramListenerThread listener = socketListenerThreads.get(groupID);

            if (listener == null) {
                logger.debug("There is no listener for group " + groupID + " to stop! Ignoring operation.");
                return;
            }
            
            socketListenerThreads.remove(groupID);

            listener.terminate();
        }
    }
    
    public void sendMessageToGroup(CoUniverseMessage message, NodeGroupIdentifier groupIdentifier) {
    
        JxtaMulticastSocket socket = multicastSocketMap.get(groupIdentifier);
        
        if (socket == null) {
            logger.error("Multicast socket for " + groupIdentifier + " not found!");
            return;
        }
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;

        try {
        
            out = new ObjectOutputStream(bos);
            out.writeObject(message);
            byte[] messageBytes = bos.toByteArray();

            // TODO: Split oversized messages to multiple ones
            if (messageBytes.length > MyJXTAConnector.MAX_DATAGRAM_SIZE_BYTES) {
                throw new IllegalArgumentException("Message is too long (" + messageBytes.length + " bytes)");
            }
            
            DatagramPacket data = new DatagramPacket(messageBytes, messageBytes.length);
            
            logger.info("Sending message over to channel <" + groupIdentifier.identifier + ">: " + message + " (thread " + Thread.currentThread().getName() + ")");
            NetworkConnector.logOutgoingMessage("Group " + groupIdentifier, message);
            socket.send(data);
            
        } catch (IOException ex) {
            logger.error("Failed to send message;");
            logger.error(ex);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                  bos.close();
            } catch (IOException ex) {
            }
        }
        
    }
    
}
