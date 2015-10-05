/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package p2p;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.Logger;

/**
 * Thread monitoring AGC voting activities. Once a vote is initiated, the thread
 * is woken up and logs the voting tickets passing by. If the AGC does not report
 * itself within VOTE_DURATION_TIME, the new AGC is elected among all received 
 * voting tickets. If the local node is elected, the NetworkConnector.startAGC()
 * method is invoked.
 * 
 * @author maara
 */
public class AGCCandidateThread extends Thread {
    
    public static class VoteTicket implements Serializable {
        
        public final ConnectorID identifier;
        public final int capacity;

        public VoteTicket(ConnectorID identifier, int capacity) {
            this.identifier = identifier;
            this.capacity = capacity;
        }
        
        @Override
        public String toString() {
            return "Vote ticket for " + identifier + " with capacity " + capacity;
        }
    }
    
    static Logger logger = Logger.getLogger(AGCCandidateThread.class);
    
    public static final long VOTE_DURATION_TIME = 5000;
    public static final long AFTER_VOTE_CHILLOUT_TIME = 5000;
    public static final long VOTE_RELATED_PUBLISH_TIMEOUT = 2000;
    
    
    private final AtomicLong lastVoteRelatedPublish = new AtomicLong(0);
    
    private final NetworkConnector connector;
    private final Object lock = new Object();
    private volatile boolean voteInProgress = false;
    private AtomicLong lastVoteCancel = new AtomicLong(System.currentTimeMillis());
    
    private final LinkedList<VoteTicket> voteTicketQueue = new LinkedList<>();
    
    private volatile boolean terminated = false;
    
    public AGCCandidateThread(NetworkConnector connector) {
        this.connector = connector;
        setName("AGC Candidate thread");
    }
    
    public void publishVoteInformation() {
        
        logger.debug("Executing publishVoteInformation() ...");

        if (System.currentTimeMillis() - lastVoteRelatedPublish.get() < VOTE_RELATED_PUBLISH_TIMEOUT) {
            // My vote information was published recently, no need to resend
            logger.debug("    publishVoteInformation() failed: published recently");
            return;
        }

        synchronized(lastVoteRelatedPublish) {
            if (System.currentTimeMillis() - lastVoteRelatedPublish.get() < VOTE_RELATED_PUBLISH_TIMEOUT) {
                // Someone beat me to publish vote information, no need to resend
                logger.debug("    publishVoteInformation() failed: published just now");
                return;
            }
            lastVoteRelatedPublish.set(System.currentTimeMillis());
        }
        
        // I have the right to publish the local vote information
        logger.debug("  publishing vote informations");

        if (connector.isAGCRunning()) {
            connector.sendMessageToGroup(
                    new CoUniverseMessage(
                            MessageType.AGC_LOCATION_MESSAGE, 
                            new Serializable[]{connector.getConnectorID()},
                            connector.getConnectorID(),
                            null),
                    NodeGroupIdentifier.ALL_NODES);
            logger.debug("  published AGC_LOCATION");
            return;
        }
        
        connector.sendMessageToGroup(
                new CoUniverseMessage(
                        MessageType.AGC_VOTE_TICKET_MESSAGE, 
                        new Serializable[]{new VoteTicket(
                                connector.getConnectorID(), 
                                connector.isAGCEnabled() ? 1 : 0)},
                        connector.getConnectorID(),
                        null), 
                NodeGroupIdentifier.AGC_CAPABLE_NODES);
        logger.debug("  published vote ticket");
    }
    
    public void terminate() {
        terminated = true;
        synchronized(lock) {
            lock.notify();
        }
    }
    
    private boolean isVoteCanceled() {
        long now = System.currentTimeMillis();
        long lastVoteCancel = this.lastVoteCancel.get();
        
        if (now - lastVoteCancel < AFTER_VOTE_CHILLOUT_TIME) {
            return true;
        }
        
        return false;
    }
    
    private void cancelVote() {
        if (isVoteCanceled()) return;
        
        this.lastVoteCancel.set(System.currentTimeMillis());
        voteInProgress = false;
    }
    
    @Override
    public void run() {
        
        logger.info("Starting AGC Candidate thread");
        
        // register message listeners
        MessageListener voteInitListener = new MessageListener() {

            @Override
            public void onMessageArrived(CoUniverseMessage message) {
                if (! message.type.equals(MessageType.AGC_INIT_VOTE_MESSAGE)) {
                    logger.warn("VoteTicketListener: Received message of invalid type: " + message);
                }
                
                publishVoteInformation();
                
                synchronized(lock) {
                    if (! isVoteCanceled()) {
                        voteInProgress = true;
                        lock.notifyAll();
                    }
                }
            }
            
            @Override
            public String toString() {
                return "Message listener of AGC_Candidate, used for AGC_INIT_VOTE_MESSAGE";
            }
        };
        connector.addMessageListener(MessageType.AGC_INIT_VOTE_MESSAGE, voteInitListener);

        MessageListener voteTicketListener = new MessageListener() {

            @Override
            public void onMessageArrived(CoUniverseMessage message) {
                if (! message.type.equals(MessageType.AGC_VOTE_TICKET_MESSAGE)) {
                    logger.warn("VoteTicketListener: Received message of invalid type: " + message);
                }
                
                publishVoteInformation();
                
                synchronized (lock) {
                    if (! isVoteCanceled()) {
                        voteTicketQueue.offer((VoteTicket) message.content[0]);
                        voteInProgress = true;
                        lock.notifyAll();
                    }
                }
            }

            @Override
            public String toString() {
                return "Message listener of AGC_Candidate, used for AGC_VOTE_TICKET_MESSAGE";
            }
        };
        connector.addMessageListener(MessageType.AGC_VOTE_TICKET_MESSAGE, voteTicketListener);

        MessageListener agcLocationListener = new MessageListener() {
            @Override
            public void onMessageArrived(CoUniverseMessage message) {
                if (! message.type.equals(MessageType.AGC_LOCATION_MESSAGE)) {
                    logger.warn("AGC Location listener: Received message of invalid type: " + message);
                }
                
                synchronized(lock) {
                    cancelVote();
                    lock.notifyAll();
                }
            }

            @Override
            public String toString() {
                return "Message listener of AGC_Candidate, used for AGC_LOCATION_MESSAGE";
            }
        };
        connector.addMessageListener(MessageType.AGC_LOCATION_MESSAGE, agcLocationListener);
        
        connector.startReceivingFromGroup(NodeGroupIdentifier.AGC_CAPABLE_NODES);
        
        // =====================================================================
        // start election loop
        while (! terminated) {
            
            if (! voteInProgress) {
                synchronized(lock) {
                    if (! voteInProgress) {
                        try {
                            lock.wait();
                        } catch (InterruptedException ex) {}
                    }
                }
            }
            
            if (! voteInProgress) continue;
            
            long voteStart = System.currentTimeMillis();
            
            while (System.currentTimeMillis() - voteStart < VOTE_DURATION_TIME) {
                try {
                    Thread.sleep(System.currentTimeMillis() - voteStart);
                } catch (InterruptedException ex) {}
            }
            
            if (isVoteCanceled()) {
                voteInProgress = false;
                continue;
            }
            
            synchronized(lock) {
                VoteTicket winner = null;
                
                while (! voteTicketQueue.isEmpty()) {
                    VoteTicket candidate = voteTicketQueue.pop();
                    if (candidate == null) continue;
                    
                    if (winner == null) winner = candidate;
                    
                    if (candidate.capacity > winner.capacity) {
                        winner = candidate;
                    } else if (candidate.capacity == winner.capacity) {
                        if (candidate.identifier.compareTo(winner.identifier) > 0) {
                            winner = candidate;
                        }
                    }
                }
                
                if (winner != null) {
                
                    logger.info("Election finished! Selected candidate is <" + winner + ">");

                    // At this point, winner contains voting ticket for the next AGC
                    if (connector.getConnectorID().equals(winner.identifier)) {
                        connector.startAGC();
                    }
                } else {
                    logger.error("Election finished! NO AGC WAS ELECTED!");
                }
                
                
            }
            
            // ignore vote messages that arrive within a certain time
            long voteEnd = System.currentTimeMillis();
            while (System.currentTimeMillis() - voteEnd < AFTER_VOTE_CHILLOUT_TIME) {
                try {
                    Thread.sleep(System.currentTimeMillis() - voteStart);
                } catch (InterruptedException ex) {}
            }
            
            synchronized(lock) {
                voteInProgress = false;
                voteTicketQueue.clear();
                lock.notifyAll();
            }
            
            
            
        }
        
        // AGCCandidate thread terminating; remove listeners
        connector.removeMessageListener(voteInitListener, MessageType.AGC_INIT_VOTE_MESSAGE);
        connector.removeMessageListener(voteTicketListener, MessageType.AGC_VOTE_TICKET_MESSAGE);
        connector.removeMessageListener(agcLocationListener, MessageType.AGC_LOCATION_MESSAGE);
    }
    
}
