/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package p2p;

import agc.ApplicationGroupController;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.Logger;

/**
 *
 * @author maara
 */
public class AGCController {
    static Logger logger = Logger.getLogger(AGCController.class);
    
    private final NetworkConnector connector;
    
    public static final long AGC_LOCATION_MESSAGE_TIMEOUT = 5000;
    private final AtomicLong lastAGCLocationPublish = new AtomicLong(0);
    private volatile boolean isRunning = false;
    
    private MessageListener agcPingListener, nodeUpdateListener;
    
    private ApplicationGroupController agc = null;
    
    public AGCController(NetworkConnector connector) {
        this.connector = connector;
    }
    
    private void publishLocation() {
        logger.info("publics location");
        if (System.currentTimeMillis() - lastAGCLocationPublish.get() < AGC_LOCATION_MESSAGE_TIMEOUT) {
            // the message was published recently
            logger.info("published recently, aborting");
            return;
        }
        
        synchronized(lastAGCLocationPublish) {
            if (System.currentTimeMillis() - lastAGCLocationPublish.get() < AGC_LOCATION_MESSAGE_TIMEOUT) {
                logger.info("published right now, aborting");
                return;
            }
            
            lastAGCLocationPublish.set(System.currentTimeMillis());
        }
        
        // If the AGC was already stopped, then do not respond for these messages
        if (! isRunning) {
            logger.info("AGC is not running, aborting publish");
            return;
        }
        
        connector.sendMessageToGroup(
                new CoUniverseMessage(
                        MessageType.AGC_LOCATION_MESSAGE, 
                        new Serializable[]{},
                        connector.getConnectorID(),
                        null), 
                NodeGroupIdentifier.ALL_NODES);
        logger.info("Published location successfully");
    }
    
    /**
     * Start the AGC with all its routines. After this call ends, the AGC will
     *   be fully deployed at the local node
     */
    public void start() {
        
        logger.info("Starting AGC thread");

        synchronized (this) {
            if (isRunning) return;
        
            // register message listeners
            agcPingListener = new MessageListener() {

                @Override
                public void onMessageArrived(CoUniverseMessage message) {
                    publishLocation();
                }

                @Override
                public String toString() {
                    return "AGC_Thread PING responder";
                }
            };
            connector.addMessageListener(MessageType.AGC_PING_MESSAGE, agcPingListener);

            // TODO: register topology update message listeners
            nodeUpdateListener = new MessageListener() {

                @Override
                public void onMessageArrived(CoUniverseMessage message) {
                    System.out.println("Received node update message from node " + message.content[0]);;
                }

                @Override
                public String toString() {
                    return "AGC_Thread PING responder";
                }
            };
            connector.addMessageListener(MessageType.NEW_ENDPOINT_NODE_MESSAGE, nodeUpdateListener);

            // TODO: obtain current topology view; e.g., AGC candidates may be 
            //    gathering the topology knowledge during idle?

            agc = new ApplicationGroupController(connector);
            agc.startAGC();
            
            isRunning = true;
        }
        
        publishLocation();
    }
    
    public synchronized void stop() {
        // TODO: Invoke stop() upon application termination!
        
        if (! isRunning) return;
        
        isRunning = false;
        agc.stopAGC();
        
        connector.removeMessageListener(agcPingListener, MessageType.AGC_PING_MESSAGE);
        connector.removeMessageListener(nodeUpdateListener, MessageType.NEW_ENDPOINT_NODE_MESSAGE);
        
        agcPingListener = null;
        nodeUpdateListener = null;
    }
}
