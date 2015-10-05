/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package myJXTA;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.jxta.endpoint.Message;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.util.JxtaBiDiPipe;
import net.jxta.util.JxtaServerPipe;
import org.apache.log4j.Logger;
import p2p.CoUniverseMessage;
import p2p.NetworkConnector;

/**
 * Currently, a peer can run just one instance of PipeServer. Other clients connect
 *   to it by opening pipe described only by id.getId() of the node running server.
 *   To extend to multiple parallel server pipe listeners, use a different
 *   pipe descriptor than id.getId() and clients can connect to it using that name.
 * @author maara
 */
public class MyJXTAPipeServices {
    // TODO: Currently the PipeServices are not used, as there were some bugs for the demo.
    //   Fix them and use them instead of multicast for AGC->Node communcation.
    
    static final Logger logger = Logger.getLogger(MyJXTAPipeServices.class);

    private final MyJXTAConnector connector;
    private final MyJXTAConnectorID localNodeID;
    
    // Synchronized writes, asynchronous reads
    private final ConcurrentHashMap<MyJXTAConnectorID, JxtaBiDiPipe> clientToPipeMap = new ConcurrentHashMap<>();
    
    private final Object pipeServerLock = new Object();
    private PipeServerThread serverThread = null;
    
    private class PipeServerThread extends Thread {

        private final JxtaServerPipe serverPipe;
        
        // all access must be synchronized
        private final LinkedList<JxtaBiDiPipe> openedPipes = new LinkedList<>();
        
        private volatile boolean terminated = false;
        
        public PipeServerThread(JxtaServerPipe serverPipe) {
            this.serverPipe = serverPipe;
        }
        
        public void terminate() {
            terminated = true;
            try {
                serverPipe.close();
            } catch (IOException ex) {
                logger.debug("Failed to close pipe server " + localNodeID + ": " + ex);
            }
            
            synchronized(openedPipes) {
                for (JxtaBiDiPipe pipe : openedPipes) {
                    if (pipe != null) {
                        try {
                            pipe.close();
                        } catch (IOException ex) {}
                    }
                }
                
                openedPipes.clear();
            }
        }
        
        List<JxtaBiDiPipe> getOpenedPipes() {
            synchronized(openedPipes) {
                return new LinkedList<>(openedPipes);
            }
        }
        
        @Override
        public void run() {
            
            System.out.println("Listening on server pipe " + serverPipe);
            
            while (! (terminated || serverPipe.isClosed()) ) {
                try {
                    logger.info("Starting to listen for clients on server pipe " + serverPipe);
                    JxtaBiDiPipe pipe = serverPipe.accept();
                    
                    if (pipe == null) {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ex) {}
                        logger.debug("Received incoming pipe is null!");
                        continue;
                    }
                    
                    PipeMsgListener listener = new PipeMsgListener() {
                        
                        @Override
                        public void pipeMsgEvent(PipeMsgEvent pme) {
                            receiveMessage(pme.getMessage());
                        }
                    };
                    
                    pipe.setMessageListener(listener);
                    synchronized(openedPipes) {
                        openedPipes.add(pipe);
                    }
                    
                } catch (SocketTimeoutException ex) {
                    // Socket timed out, ignore
                } catch (IOException ex) {
                    // Something happend
                    logger.warn(ex);
                    ex.printStackTrace(System.out);
                }
                
            }
            
            terminate();
        }
    }

    public MyJXTAPipeServices(MyJXTAConnector connector) {
        this.connector = connector;
        this.localNodeID = (MyJXTAConnectorID) connector.getConnectorID();
    }
    
    // == Server pipe methods ==================================================
    /**
     * Open a server pipe listener. Other nodes can connect to this server
     * using connectPipe(serverNodeID). 
     * @throws java.io.IOException Cannot initialize the specified pipe
     */
    public void startServerPipeListener() throws IOException {
        
        synchronized(pipeServerLock) {

            if (serverThread != null) {
                logger.debug("PipeServer already started, ignoring repeated call.");
                return;
            }
            
            JxtaServerPipe serverPipe = new JxtaServerPipe(connector.getUniversePeerGroup(), MyJXTAUtils.getPipeAdvertisement(
                    connector.getUniversePeerGroup(), 
                    localNodeID.getId(),
                    PipeService.UnicastType));
            logger.debug("BiDirectional server pipe created for pipe <" + localNodeID.getId() + ">: " + serverPipe);
            serverPipe.setPipeTimeout(20000);

            serverThread = new PipeServerThread(serverPipe);
            
            serverThread.start();

            logger.debug("Receiving on server pipe <" + localNodeID.toString() + "> started");

            // TODO: create advertisement and publish it
        }
    }
    
    public void stopServerPipeListener(String pipeName) {
        
        synchronized (pipeServerLock) {
            if (serverThread == null) {
                logger.debug("Attempt to close pipe server failed: server not running");
                return;
            }
            serverThread.terminate();
        }
    }

    /**
     * Send a message to all clients connected to the local pipe server
     * @param couniverseMessage Message to be sent
     */
    public void sendServerMessage(CoUniverseMessage couniverseMessage) {

        List<JxtaBiDiPipe> pipeList = null;
        
        synchronized(pipeServerLock) {
            if (serverThread == null) {
                logger.warn("Sending server message while server is not running! Invocation ignored");
                return;
            }
            pipeList = serverThread.getOpenedPipes();
        }
        
        if (pipeList == null) {
            logger.warn("Failed to retreive pipe list from the server! Aborting send of message " + couniverseMessage);
            return;
        }
        
        for (JxtaBiDiPipe pipe : pipeList) {
            try {
                if (pipe == null) continue;
                
                Message jxtaMessage = MyJXTAUtils.encodeMessage(couniverseMessage);
                
                pipe.sendMessage(jxtaMessage);
            } catch (IOException ex) {}
        }
    }
    
    // == Client pipe methods ==================================================
    /**
     * Blocking creation of a pipe. If such pipe already exists, the call is ignored
     * @param serverID connectorID of the node to connect to
     * @param timeout Maximum waiting time (0 = indefinitely)
     * @throws IOException Upon failure of connection establishment
     */
    public void connectClientPipe(MyJXTAConnectorID serverID, int timeout) throws IOException {

        if (serverID == null) {
            logger.warn("Cannot create client pipe to NULL! Ignoring operation");
            return;
        }
        
        // The call may be used often to ensure a pipe is opened; Double checking
        //   is therefore used to avoid unnecessary locking
        if (clientToPipeMap.get(serverID) != null) {
            // pipe already created, no need to create another
            return;
        }
        
        synchronized(clientToPipeMap) {
            
            if (clientToPipeMap.get(serverID) != null) {
                // pipe already created, no need to create another
                return;
            }
            
            PipeAdvertisement ad = MyJXTAUtils.getPipeAdvertisement(
                    connector.getUniversePeerGroup(), 
                    serverID.getId(), 
                    PipeService.UnicastType);

            logger.debug("Client opening pipe to " + serverID.toString());

            JxtaBiDiPipe pipe = new JxtaBiDiPipe(
                    connector.getUniversePeerGroup(), 
                    ad, 
                    timeout,
                    new PipeMsgListener() {
                            @Override
                            public void pipeMsgEvent(PipeMsgEvent pme) {
                                receiveMessage(pme.getMessage());
                            }
            });
            logger.debug("Client pipe succesfully connected to server " + serverID.toString());
        
            clientToPipeMap.put(serverID, pipe);
        }
    }
    
    public void disconnectPipe(MyJXTAConnectorID targetID) {

        synchronized(clientToPipeMap) {
            JxtaBiDiPipe pipe = clientToPipeMap.get(targetID);

            try {
                if (pipe != null) pipe.close();
            } catch (IOException ex) { }

            // Clean all closed pipes while we are at it
            for (MyJXTAConnectorID id : clientToPipeMap.keySet()) {
                pipe = clientToPipeMap.get(id);
                if ( pipe == null || (! pipe.isBound()) ) {
                    clientToPipeMap.remove(id);
                }
            }
        }
    }
    
    /**
     * Send message to a given node. Client pipe must be opened
     * @param couniverseMessage Message to be sent. The receiver attribute of this message specifies the server to send it to
     * @throws IOException 
     */
    public void sendMessage(CoUniverseMessage couniverseMessage) throws IOException {
        
        MyJXTAConnectorID serverID = (MyJXTAConnectorID) couniverseMessage.receiver;
        
        JxtaBiDiPipe pipe = clientToPipeMap.get(serverID);
        
        if (pipe == null) {
            logger.warn("Failed to locate pipe to " + serverID + "! Opening it now ...");
            connectClientPipe(serverID, 5000);
            
            pipe = clientToPipeMap.get(serverID);
            
            if (pipe == null) {
                logger.debug("Failed to open pipe to <" + serverID.toString() + ">! Message not sent.");
                throw new IOException("Failed to open pipe to <" + serverID.toString() + ">! Message not sent.");
            }
        }
        
        if (! pipe.isBound()) {
            logger.debug("The pipe to " + serverID.toString() + " is not bound! Message not sent.");
            throw new IOException("The pipe to " + serverID.toString() + " is not bound! Message not sent.");
            // NOTE: The call is not synchronous, hence we do not know whether 
            //   the pipe was already closed or was not opened yet
        }
        
        Message jxtaMessage = MyJXTAUtils.encodeMessage(couniverseMessage);

        NetworkConnector.logOutgoingMessage("Pipe", couniverseMessage);
        pipe.sendMessage(jxtaMessage);
    }
    
    // == Others ===============================================================
    public void receiveMessage(Message jxtaMessage) {
        
        CoUniverseMessage couniverseMessage = MyJXTAUtils.decodeMessage(jxtaMessage);
        
        if (couniverseMessage.sender == null) {
            logger.warn("Received message with no sender set! " + couniverseMessage);
        }
        
        NetworkConnector.logIngoingMessage("Pipe", couniverseMessage);
        connector.receiveMessage(couniverseMessage);
    }
} 
