package p2p;

import java.io.Serializable;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.log4j.Logger;

/**
 * Monitoring of the AGC location and status. This thread monitors status of the
 * AGC. Once the connection is lost, it is also capable of initiating a new AGC
 * vote. If AGC is allowed on this node as well, a separate AGCCandidateThread
 * should be running that participates in the vote
 *
 * @author maara
 */
public class AGCMonitorThread extends Thread {
    static Logger logger = Logger.getLogger(AGCMonitorThread.class);

    public final long AGC_GAP_BETWEEN_MESSAGES = 10000; // time between AGC incoming messages; if exceeded, fire AGC_PING to verify AGC liveness
    public final long AGC_PING_RESPONSE_LIMIT = 2500; // time the AGC requires to respond to AGC_PING. If no response is received within limit, a vote is started

    private final Object lastContactLock = new Object();
    private volatile long lastContact;
    private volatile ConnectorID agcConnector;
    private volatile boolean terminated = false;

    private final NetworkConnector connector;
    private final ConcurrentLinkedQueue<MessageListener> nodeListeners = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<MessageListener> agcListeners = new ConcurrentLinkedQueue<>();

    public AGCMonitorThread(NetworkConnector connector) {
        this.connector = connector;
        this.lastContact = System.currentTimeMillis();

        setName("AGC Monitor thread");
    }

    private void logAgcContact(ConnectorID agcConnector) {
        synchronized (this.lastContactLock) {
            lastContact = System.currentTimeMillis();
            this.agcConnector = agcConnector;
        }
        // TODO: log and check history of AGC IDs in order to detect multiple AGCs in the same network
    }

    @Override
    public void run() {

        logger.info("Starting AGC monitor thread");
        
        // Register message listeners
        if (connector == null) {
            throw new NullPointerException("Network connector must be initialized prior to starting AGCMonitor thread");
        }

        MessageListener agcLocationListener = new MessageListener() {

            @Override
            public void onMessageArrived(CoUniverseMessage message) {
                if (! message.type.equals(MessageType.AGC_LOCATION_MESSAGE)) {
                    logger.warn("Received message is not an AGC_LOCATION_MESSAGE! " + message);
                    return;
                }
                
                ConnectorID agcLocation = message.sender;
                logAgcContact(agcLocation);
            }

            @Override
            public String toString() {
                return "AGC_Monitor listener for AGC_LOCATION_MESSGE";
            }
        };

        connector.addMessageListener(MessageType.AGC_LOCATION_MESSAGE, agcLocationListener);
        nodeListeners.add(agcLocationListener);

        // ping AGC in order to accelerate AGC discovery
        connector.sendMessageToGroup(
                new CoUniverseMessage(
                        MessageType.AGC_PING_MESSAGE, 
                        new Serializable[0],
                        connector.getConnectorID(),
                        null), 
                NodeGroupIdentifier.AGC_CAPABLE_NODES);

        // start the AGC monitoring routine
        while (!terminated) {
            long lastContact;
            synchronized (this.lastContactLock) {
                lastContact = this.lastContact;
            }
            long now = System.currentTimeMillis();

            if (now - lastContact < AGC_GAP_BETWEEN_MESSAGES) {
                try {
                    Thread.sleep(now - lastContact);
                } catch (InterruptedException ex) {
                    // we do not care
                }
                continue;
            }

            // time between AGC messages exceeded; ping AGC
            connector.sendMessageToGroup(
                    new CoUniverseMessage(
                            MessageType.AGC_PING_MESSAGE, 
                            new Serializable[0], 
                            connector.getConnectorID(),
                            null), 
                    NodeGroupIdentifier.AGC_CAPABLE_NODES);
            
            long pingSentTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - pingSentTime < AGC_PING_RESPONSE_LIMIT) {
                try {
                    Thread.sleep(500);
                    synchronized (this.lastContactLock) {
                        if (System.currentTimeMillis() - this.lastContact < AGC_GAP_BETWEEN_MESSAGES) {
                            // A new message was received meanwhile
                            break;
                        }
                    }
                } catch (InterruptedException ex) {
                    // we do not care
                }
            }

            // Verify that no message was received meanwhile
            synchronized (this.lastContactLock) {
                if (System.currentTimeMillis() - this.lastContact < AGC_GAP_BETWEEN_MESSAGES) {
                    continue;
                }
            }

            // No AGC response was received for PING. We assume AGC is down and initiate AGC vote
            connector.sendMessageToGroup(
                    new CoUniverseMessage(
                            MessageType.AGC_INIT_VOTE_MESSAGE, 
                            new Serializable[0],
                            connector.getConnectorID(),
                            null), 
                    NodeGroupIdentifier.AGC_CAPABLE_NODES);

            // TODO: if no AGC-capable node is present, what should we do?
        }

        for (MessageListener listener : nodeListeners) {
            connector.removeGlobalMessageListener(listener);
        }

        for (MessageListener listener : agcListeners) {
            connector.removeGlobalMessageListener(listener);
        }
    }

    public void terminate() {
        this.terminated = true;
    }

    public ConnectorID getAgcConnectorID() {
        return agcConnector;
    }

}
