/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package agc;

import java.lang.Thread.State;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import networkRepresentation.EndpointNetworkNode;
import networkRepresentation.GeneralNetworkLink;
import networkRepresentation.GeneralNetworkNode;
import networkRepresentation.LogicalNetworkLink;
import networkRepresentation.NetworkTopologyStatus;
import networkRepresentation.PartiallyKnownNetworkTopology;
import networkRepresentation.PhysicalNetworkNode;
import networkRepresentation.UnknownNetworkNode;
import networkRepresentation.sampleTopologies.Glif2014DemoTopology;
import networkRepresentation.sampleTopologies.SampleTopologyBuilder;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import p2p.MessageType;

/**
 * Creates a separate daemon thread with exclusive access to the topology. 
 * Other threads pass requests to that thread, either blocking or future tasks.
 * 
 * @author maara
 */
public class TopologyHolder {
    static final Logger logger = Logger.getLogger("TopologyHolder");
    static {logger.setLevel(Level.ALL);}
        
    private PartiallyKnownNetworkTopology topology;
    
    private final BlockingQueue<Task> taskQueue = new LinkedBlockingQueue<>();
    
    private final TopologyUpdaterThread workerThread;
    
    // <editor-fold desc="Task definitions">
    private class Task {
        volatile Thread thread;
        final Object lock = new Object();
        volatile boolean completed = false;
        
        public Task() {
            thread = Thread.currentThread();
        }
    }
    
    private class UpdateTask extends Task{
        volatile MessageType type;
        volatile List<Object> contents;

        public UpdateTask(MessageType type, Object[] contents) {
            this.type = type;
            this.contents = Arrays.asList(contents);    
        }
    }
    
    private class SetTopologyTask extends Task {
        volatile PartiallyKnownNetworkTopology topology;

        public SetTopologyTask(PartiallyKnownNetworkTopology topology) {
            this.topology = topology;
        }
    }

    private class SnapshotRequest extends Task {
        volatile PartiallyKnownNetworkTopology topology;
    }
    // </editor-fold>
    
    private class TopologyUpdaterThread extends Thread {
        
        public TopologyUpdaterThread() {
            setDaemon(true);
        }
        
        volatile boolean paused = false;

        private void infoOnException(Throwable e, String s) {
            if (logger.isDebugEnabled()) {
                e.printStackTrace(System.err);
            }
            logger.error(s);
        }        
        
        /**
         * Process update topology message.
         * @param type
         * @param objects 
         * @return True iff the update was successful
         */
        public boolean updateTopology(MessageType type, Object[] objects) {
            
            if (type.equals(MessageType.NODE_UNREACHABLE_MESSAGE)) { // <editor-fold>
                logger.info("NetworkNode Unreachable via NetworkLink message received.");
                GeneralNetworkLink inactiveNetworkLink;

                try {
                    inactiveNetworkLink = (GeneralNetworkLink) objects[0];
                    // Set the link (linkFromNode, linkToNode) where linkToNode has interface with remote IP inactive
                    logger.info("Setting link " + inactiveNetworkLink + " inactive.");
                    /* TODO: inactivate both physical and logical links */
                    topology.switchNetworkLinkDown(inactiveNetworkLink);
                    
                    if (inactiveNetworkLink instanceof LogicalNetworkLink) {
                        LogicalNetworkLink inactiveLogicallink = (LogicalNetworkLink) inactiveNetworkLink;
                        LogicalNetworkLink reverseLink = null;
                        for (LogicalNetworkLink llink : topology.getLogicalLinks()) {
                            if (llink.getFromInterface().equals(inactiveLogicallink.getToInterface()) && 
                                    llink.getToInterface().equals(inactiveLogicallink.getFromInterface())) {
                                reverseLink = llink;
                                break;
                            }
                        }
                        
                        // TODO: More sophisticated approach may be needed,
                        //   if the link is single-directional (e.g., the other
                        //   node is behind NAT)
                        if (reverseLink != null) {
                            topology.switchNetworkLinkDown(reverseLink);
                        }
                    }
                } catch (Exception e) {
                    infoOnException(e, "Failed to decode incoming Message.");
                    return false;
                }
                // Only a logical link has changed; This does not influence any other nodes except for the fromNode and AGC, therefore no global topology update is required
                // sendNetworkTopology();

                return true;
            } // </editor-fold>
            
            if (type.equals(MessageType.NODE_REACHABLE_MESSAGE)) { // <editor-fold>
                logger.info("NetworkNode Reachable via NetworkLink message received.");
                GeneralNetworkLink activeNetworkLink;
                try {
                    activeNetworkLink = (GeneralNetworkLink) objects[0];
                    // Set the link (linkFromNode, linkToNode) where linkToNode has interface with remote IP inactive
                    logger.info("Setting link " + activeNetworkLink + " active.");
                    topology.switchNetworkLinkUp(activeNetworkLink);
                    return true;
                } catch (Exception e) {
                    infoOnException(e, "Failed to decode incoming MessageElement.");
                }
                // Only a logical link has changed; This does not influence any other nodes except for the fromNode and AGC, therefore no global topology update is required
                // sendNetworkTopology();
                
                return false;
            }// </editor-fold>
            
            if (type.equals(MessageType.NEW_ENDPOINT_NODE_MESSAGE)) { // <editor-fold>
                logger.info("New NetworkNode message received.");
                GeneralNetworkNode newNetworkNode;
                try {
                    newNetworkNode = (EndpointNetworkNode) objects[0];
                    logger.info("Adding new endpoint network node " + newNetworkNode.getNodeName() + " to the network topology");
                    if (topology == null) throw new NullPointerException("Topology is null!");
                    topology.addNetworkNode((EndpointNetworkNode) newNetworkNode);
                    logger.info("New EndpointNetworkNode " + newNetworkNode.getNodeName() + " message processed.");
                    logger.info("  Topology contains " + topology.getEndpointNodes().size() + " nodes and " + topology.getLogicalLinks().size() + " logical links");
                    return true;
                } catch (Exception e) {
                    infoOnException(e, "Failed to process incomming NEW_ENDPOINT_NODE_MESAGE.");
                }
                return false;
            } // </editor-fold>
            
            if (type.equals(MessageType.REMOVE_NODE_MESSAGE)) { // <editor-fold>
                logger.info("NetworkNode Removed message received.");
                GeneralNetworkNode networkNodeToRemove;
                
                networkNodeToRemove = (GeneralNetworkNode) objects[0];
                // Remove the node from the networkTopology
                logger.debug("Removing network node " + networkNodeToRemove.getNodeName() + " from the network topology.");
                try {
                    switch (networkNodeToRemove.nodeType) {
                        case GeneralNetworkNode.NODE_TYPE_ENDPOINT:
                            topology.removeNetworkNode((EndpointNetworkNode) networkNodeToRemove);
                            logger.info("Remove EndpointNetworkNode " + networkNodeToRemove.getNodeName() + " message processed.");
                            break;
                        case GeneralNetworkNode.NODE_TYPE_PHYSICAL:
                            topology.removeNetworkNode((PhysicalNetworkNode) networkNodeToRemove);
                            logger.info("Remove PhysicalNetworkNode " + networkNodeToRemove.getNodeName() + " message processed.");
                            break;
                        case GeneralNetworkNode.NODE_TYPE_UNKNOWN_NETWORK:
                            topology.removeNetworkNode((UnknownNetworkNode) networkNodeToRemove);
                            logger.info("Remove UnknownNetworkNode " + networkNodeToRemove.getNodeName() + " message processed.");
                            break;
                        default:
                            logger.warn("Unknown type of network node, could not be removed!");
                            return false;
                    }
                    return true;
                } catch (Exception e) {
                    infoOnException(e, "Failed to remove the node from the network topology.");
                }
            } // </editor-fold>
            
            if (type.equals(MessageType.LINK_LATENCY_CHANGED_MESSAGE)) { // <editor-fold>
                logger.info("Link latency change message received!");

                LogicalNetworkLink newLink;
                try {
                    newLink = (LogicalNetworkLink) objects[0];
                    // TODO: Optimize using from/to nodes of the link
                    for (LogicalNetworkLink llink : topology.getLogicalLinks()) {
                        if (llink.equals(newLink)) {
                            llink.setLatency(newLink.getLatency());
                            logger.info("Latency of <" + llink.toString() + "> set to " + newLink.getLatency() + "ms");
                            return true;
                        }
                    }
                } catch (Exception e) {
                    infoOnException(e, "Failed to decode incomming MessageElement.");
                }
                
            } // </editor-fold>
            
            return false;
        }
        
        
        @Override
        public void run() {
            while (true) {
                
                try {
                    if (paused) {
                        synchronized(this) {
                            wait();
                        }
                    }
                } catch (InterruptedException ex) {
                    continue;
                }
                if (paused) continue;
                
                Task task = null;
                try {
                    task = taskQueue.take();
                } catch (InterruptedException ex) {
                    continue;
                }
                if (task == null) continue;

                try {
                    if (task instanceof SnapshotRequest) {
                        SnapshotRequest request = (SnapshotRequest) task;
                        request.topology = PartiallyKnownNetworkTopology.getCurrentSnapshot(topology);
                        task.completed = true;
                        
                        continue;
                    }

                    if (task instanceof SetTopologyTask) {
                        topology = PartiallyKnownNetworkTopology.getCurrentSnapshot(((SetTopologyTask) task).topology);
                        task.completed = true;
                        continue;
                    }

                    if (task instanceof UpdateTask) {
                        if (updateTopology(((UpdateTask) task).type, ((UpdateTask) task).contents.toArray())) {
                            task.completed = true;
                            continue;
                        }
                    }
                } catch (Exception e) {
                    logger.error("Exception in processing task " + task, e);
                }
                task.completed = true;

                logger.warn("Task "+task+" not recognized!");
            }
        };
        
    }
    
    /**
     * Create the topology holder. The updater thread is not active upon exit of constructor.
     * To start the updater thread, call start().
     */
    public TopologyHolder() {
        // TODO: Demo-specific, do something smarter later
//        topology = new PartiallyKnownNetworkTopology();
        SampleTopologyBuilder builder = new SampleTopologyBuilder();
        topology = builder.buildTopology(new Glif2014DemoTopology());
        workerThread = new TopologyUpdaterThread();
    }
    
    private void waitForCompletion(Task task) {
        while (! task.completed) {
            synchronized (task.lock) {
                try {
                    task.lock.wait(1000);
                } catch (InterruptedException ex) {
                }
            }
        }
        
    }
    
    public void start() {
        if (workerThread.getState().equals(State.NEW)) {
            logger.info("Starting topology updater thread");
            workerThread.start();
        }
        workerThread.paused = false;
        
        synchronized(workerThread) {
            workerThread.notify();
        }
    }
    
    /**
     * Pause the topology updater thread. Non-blocking calls to update topology will be added
     * to the queue, blocking calls are added and blocked until the thread is resumed and
     * the requests processsed.
     * To resume updater thread, call resume().
     */
    public void pause() {
        // The updater thread may be blocked on the empty taskQueue - that's after the pause check.
        // Interrupting the thread forces to abort waiting for a next queue entry and check pause status
        synchronized (workerThread) {
            workerThread.paused = true;
            workerThread.interrupt(); 
        }
    }
    
    /**
     * Resume execution of updater thread.
     */
    public void resume() {
        
        synchronized(workerThread) {
            workerThread.paused = false;
            workerThread.notify();
        }
    }
    
    /**
     * Set the whole topology knowledge.
     * All unprocessed requests will be discarded
     * @param topo 
     */
    public void setTopology(PartiallyKnownNetworkTopology topo) {
        // TODO: empty the task queue first? (tasks still require to be notified of processing/discarding them)
        taskQueue.add(new SetTopologyTask(topology));
    }
    
    /**
     * Get a consistent snapshot of the topology.
     * Blocking call. Returns after the snapshot had been created
     * @return Snapshot of the topology
     */
    public PartiallyKnownNetworkTopology getCurrentSnapshot() {
        SnapshotRequest request = new SnapshotRequest();
        
        taskQueue.add(request);
        
        waitForCompletion(request);
        
        return request.topology;
    }
    
    public void passMessage(MessageType type, Object[] contents) {
        UpdateTask task = new UpdateTask(type, contents);
        taskQueue.add(task);
    }
    
    public void passMessageBlocking(MessageType type, Object contents[]) {
        UpdateTask task = new UpdateTask(type, contents);
        taskQueue.add(task);
        
        waitForCompletion(task);
    }
    
    public NetworkTopologyStatus getStatus() {
        return topology.getStatus();
    }
    
    public void removeNetworkNode(GeneralNetworkNode node) {
        // TODO: Hot fix. Check whether works properly!
        taskQueue.add(new UpdateTask(MessageType.REMOVE_NODE_MESSAGE, new Object[]{node}));
    }
}
