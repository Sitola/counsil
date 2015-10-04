package agc;

import counsil.CounsilNetworkNodeLight;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import monitoring.LambdaMonitor;
import networkRepresentation.EndpointNetworkNode;
import networkRepresentation.GeneralNetworkNode;
import networkRepresentation.LambdaLink;
import networkRepresentation.LambdaLinkFactory;
import networkRepresentation.LogicalNetworkLink;
import networkRepresentation.NetworkTopologyStatus;
import networkRepresentation.PartiallyKnownNetworkTopology;
import org.apache.log4j.Logger;
import p2p.CoUniverseMessage;
import p2p.MessageListener;
import p2p.MessageType;
import p2p.NetworkConnector;
import p2p.NodeGroupIdentifier;
import utils.DebugConsoleNamed;
import utils.TimeUtils;

/**
 * Application Group Controller running in a thread
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 31.7.2007
 * Time: 17:55:24
 */
public class ApplicationGroupController extends Thread {

    public static final long TOPOLOGY_PUBLISH_TIMEOUT = 10000;
    
    private final NetworkConnector connector;
    private Thread agcPlanThread;
    private Thread agcPublishThread;
    private final AtomicLong lastTopologyPublishTime = new AtomicLong(System.currentTimeMillis());
    
    private final TopologyHolder topologyHolder;
    @Deprecated
    private final ConcurrentHashMap<EndpointNetworkNode, byte[]> nodePipeInputAdvMap = new ConcurrentHashMap<>();
    private volatile AtomicBoolean terminateFlag;
    private final LambdaMonitor lambdaMonitor;
    static Logger logger = Logger.getLogger(ApplicationGroupController.class);
    static Logger lambdaLogger = Logger.getLogger("lambda");

    /**
     * AGC constructor
     * <p/>
     *
     * @param connector connector to operate upon
     */
    public ApplicationGroupController(final NetworkConnector connector) {
        topologyHolder = new TopologyHolder();
        topologyHolder.start();
        this.connector = connector;
        lambdaMonitor = new LambdaMonitor();
        
        connector.startReceivingFromGroup(NodeGroupIdentifier.DEBUG_LISTENER_NODES);
        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                DebugConsoleNamed debug = new DebugConsoleNamed(connector);
                debug.setVisible(true);
            }
        });
    }

    private void registerJXTAConnectorCallbacks() {
        
        // register processing callbacks
        this.connector.addMessageListener(MessageType.NODE_UNREACHABLE_MESSAGE, new MessageListener() {

            @Override
            public void onMessageArrived(CoUniverseMessage message) {
                topologyHolder.passMessage(message.type, message.content);
            }
        });

        this.connector.addMessageListener(MessageType.NODE_REACHABLE_MESSAGE, new MessageListener() {

            @Override
            public void onMessageArrived(CoUniverseMessage message) {
                topologyHolder.passMessage(message.type, message.content);
            }
        });

        this.connector.addMessageListener(MessageType.NEW_ENDPOINT_NODE_MESSAGE, new MessageListener() {

            @Override
            public void onMessageArrived(CoUniverseMessage message) {

                Object[] objects = message.content;
//                nodePipeInputAdvMap.put((EndpointNetworkNode) objects[0], (byte[]) objects[1]);

                logger.debug("AGC: Received new endpoint node mesage " + message);
                // TODO: Consider creating a separate thread for sending updates to other nodes
                topologyHolder.passMessageBlocking(message.type, objects);

                logger.info("AGC: Processed new endpoint node message ");
                if (topologyHolder.getStatus().isChanged()) {
                    logger.info("  Topology has changed! Sending to other peers");
                    sendNetworkTopology();
                }
            }
        });

        this.connector.addMessageListener(MessageType.REMOVE_NODE_MESSAGE, new MessageListener() {

            @Override
            public void onMessageArrived(CoUniverseMessage message) {
                Object[] objects = message.content;
                MessageType type = message.type;
                
                topologyHolder.passMessageBlocking(type, objects);
                
                ApplicationGroupController.logger.info("NetworkNode Removed message received.");
                GeneralNetworkNode networkNodeToRemove;
                try {
                    networkNodeToRemove = (GeneralNetworkNode) objects[0];
                    // Remove the node from the networkTopology
                    ApplicationGroupController.logger.debug("Removing network node " + networkNodeToRemove.getNodeName() + " from the network topology.");

                    // Check if all monitored lambdas are still in the network topology
                    // TODO: not needed anymore, lambdas are associated with physical links now
                    ArrayList<LambdaLink> lambdasToRemove = new ArrayList<>();
                    for (LambdaLink lambdaLink : lambdaMonitor.getMonitoredLinks()) {
                        boolean lambdaAssociated = false;
                        for (LogicalNetworkLink networkLink : lambdaLink.getAssociatedNetworkLinks()) {

                            // TODO: rewrite, getCurrentSnapshot is inefficient
                            if (topologyHolder.getCurrentSnapshot().getNetworkTopologyGraph().containsEdge(networkLink)) {
                                lambdaAssociated = true;
                            }
                        }
                        if (!lambdaAssociated) {
                            lambdasToRemove.add(lambdaLink);
                        }
                    }

                    for (LambdaLink lambdaLink : lambdasToRemove) {
                        lambdaMonitor.removeLambdaLink(lambdaLink);
                        if (lambdaLink.isAllocated()) {
                            ApplicationGroupController.lambdaLogger.info("About to deallocate lambda " + lambdaLink);
                            LambdaLinkFactory lambdaLinkFactory = new LambdaLinkFactory(lambdaLink);
                            lambdaLinkFactory.deallocate(lambdaLink);
                        }
                    }
                } catch (Exception e) {
                    infoOnException(e, "Failed to decode incomming MessageElement.");
                }
                sendNetworkTopology();
            }
        });

        this.connector.addMessageListener(MessageType.LINK_LATENCY_CHANGED_MESSAGE, new MessageListener() {

            @Override
            public void onMessageArrived(CoUniverseMessage message) {
                topologyHolder.passMessage(message.type, message.content);
                
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void sendNetworkTopology() {
        
        boolean publish = false;
        
        if (topologyHolder.getStatus().isChanged()) {
            publish = true;
        }
        
        long now = System.currentTimeMillis();
        if (now - lastTopologyPublishTime.get() > TOPOLOGY_PUBLISH_TIMEOUT) {
            publish = true;
        }
        
        if (publish) {

            lastTopologyPublishTime.set(System.currentTimeMillis());

            PartiallyKnownNetworkTopology topology = topologyHolder.getCurrentSnapshot();

            
            ArrayList<EndpointNetworkNode> endpointNetworkNodes = new ArrayList<>(topology.getEndpointNodes());
            ArrayList<LogicalNetworkLink> logicalNetworkLinks = new ArrayList<>(topology.getLogicalLinks());

            logger.debug("Sending topology: Topology details ... " + endpointNetworkNodes.size() + " endpoints, " +
                 logicalNetworkLinks.size() + " logical links.");
            CoUniverseMessage message;
            
            //sending topology
            /*
            message = new CoUniverseMessage(
                MessageType.NETWORK_UPDATE_MESSAGE,
                new Serializable[] {
                    endpointNetworkNodes, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
                },
                connector.getConnectorID(),
                null
            );
            */
            
            ArrayList<CounsilNetworkNodeLight> counsilNetworkNodes = new ArrayList<>();
            
            for(EndpointNetworkNode endpointNetworkNode : endpointNetworkNodes){
                CounsilNetworkNodeLight counsilNetworkNodeLight = new CounsilNetworkNodeLight(endpointNetworkNode.getNodeSite(), endpointNetworkNode.getMyEndpointUserRole(), endpointNetworkNode.hasDistributor());
                counsilNetworkNodes.add(counsilNetworkNodeLight);
            }
            
            //sending just necessary info
            message = new CoUniverseMessage(
                MessageType.COUNSIL_NETWORK_UPDATE,
                new Serializable[] {
                    counsilNetworkNodes
                },
                connector.getConnectorID(),
                null
            ); 
            
            connector.sendMessageToGroup(message, NodeGroupIdentifier.ALL_NODES);        
            
        }
    }

    /**
     * Application Group Controller Plan Loop
     */
    @SuppressWarnings("unchecked")
    private void agcPlanLoop() {
        PlanElementBean.Plan agcPlan = null;
        MatchMakerConfig agcMatchMakerConfig = new MatchMakerConfig();

        agcMatchMakerConfig.timeout = 30000;

        agcMatchMakerConfig.setProperty("enableElimIntrasiteLinksLogical", true);
        agcMatchMakerConfig.setProperty("enableElimLinkCapacityLogical", true);
        agcMatchMakerConfig.setProperty("enableElimNoappLogical", true);
        agcMatchMakerConfig.setProperty("enableElimLinkCapacityPhysical", true);
        agcMatchMakerConfig.setProperty("enableElimNoappPhysical", true);

        int threadPoolSize = 10;  // TODO how many lambda link allocations do we want to perform simultaneously?
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(threadPoolSize, 2 * threadPoolSize, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        logger.info("Plan Thread started.");

        while (!terminateFlag.get()) {
            PartiallyKnownNetworkTopology topology = topologyHolder.getCurrentSnapshot();
            
            if (logger.isDebugEnabled()) topology.print();

            boolean proceedWithPlanning = false;

            NetworkTopologyStatus nts = topologyHolder.getStatus();
            long now = System.currentTimeMillis();
            
            logger.info("AGC plan loop: Changed=" + nts.isChanged()+", changed stamp=" + nts.getChangedStamp() + ", now = " + now + ", getChangesCount=" + nts.getChangesCount());
            boolean b1 = nts.isChanged();
            boolean b2 = (nts.getChangedStamp() < System.currentTimeMillis() - 1000);
            boolean b3 = nts.getChangesCount() > 10;
            boolean res = b1 && (b2 || b3);
            logger.info("Eval: " + b1 + " && (" + b2 + " || " + b3 + ") = " + res);
            
            if (nts.isChanged() && (nts.getChangedStamp() < System.currentTimeMillis() - 1000 || nts.getChangesCount() > 10)) {
                proceedWithPlanning = true;
                logger.debug("Proceeding with planning");
            }

            if (proceedWithPlanning) {

                ApplicationGroupController.logger.debug("Starting MatchMaker.");

                MatchMaker agcMatchMaker = new MatchMakerACO(topology, agcMatchMakerConfig);
                
                
                try {
                    logger.debug("Starting planning");
                    boolean boo = agcMatchMaker.doMatch();
                    System.out.println("Planning result = " + boo);
                    if (boo) {

                        logger.debug("Planning succeeded");
                        // We have to ensure that getPlan is called while conditions are unchanged from doMatch()!!!
                        // Get results from the MatchMaker
                        ApplicationGroupController.logger.info("Getting the plan.");
                        agcPlan = agcMatchMaker.getPlan();

                        logger.debug("The plan is using logical links: " + agcMatchMaker.getUsedLinks());

                    } else {
                        logger.debug("Failed to create plan");
                        agcPlan = new PlanElementBean.Plan();
                    }
                } catch (Exception ex) {
                    System.err.println("Planning failed!");
                    ex.printStackTrace(System.err);
                }

                if (agcPlan != null) {

                    CoUniverseMessage message = new CoUniverseMessage(
                            MessageType.PLAN_UPDATE_MESSAGE, 
                            new Serializable[]{agcPlan},
                            connector.getConnectorID(), 
                            null);
                    this.connector.sendMessageToGroup(message, NodeGroupIdentifier.ALL_NODES);
//                    topologyHolder.resetChangedStatus(nts);
                    processLambdas(agcMatchMaker, threadPoolExecutor);

                } else {
                    ApplicationGroupController.logger.error("Got an empty plan.");
                }
            } else {
                // There was no change in the network topology, lets sleep for a while
                ApplicationGroupController.logger.debug("The topology has not been modified; No planning needed");
                TimeUtils.sleepFor(500);
            }
        }

        threadPoolExecutor.shutdown();
    }

    private void processLambdas(MatchMaker agcMatchMaker, ThreadPoolExecutor threadPoolExecutor) {
        // <editor-fold desc="lambda stuff" defaultstate="collapsed">
        
        // Check if all monitored lambdas are still in the network topology
        // TODO: not needed anymore, lambdas are associated with physical links now
        ArrayList<LambdaLink> lambdasToRemove = lambdaMonitor.getMonitoredLinks();
        
        // Check for any links with associated lambdas and allocate them
        ApplicationGroupController.lambdaLogger.info("Checking the plan for lambdas to be allocated.");
        ArrayList<LambdaLink> lambdasToAllocate = new ArrayList<>();
        if (agcMatchMaker.getUsedLinks() != null) { // TODO why does the match maker return null when there are no lambdas?
            for (LogicalNetworkLink nl : agcMatchMaker.getUsedLinks()) {
                // TODO There might be more lambdas associated with one link
                if (!nl.getAssociatedLambdas().isEmpty()) {
                    for (LambdaLink ll : nl.getAssociatedLambdas()) {
                        
                        Iterator<LambdaLink> it = lambdasToRemove.iterator();
                        while (it.hasNext()) {
                            LambdaLink currentLink = it.next();
                            if (ll.equals(currentLink))
                                it.remove();
                        }
                        
                        if (!lambdasToAllocate.contains(ll)) {
                            lambdasToAllocate.add(ll);
                            ApplicationGroupController.lambdaLogger.info("New Lambda to allocate: " + ll);
                        }
                    }
                }
            }
        }
        
        for (LambdaLink lambdaLink : lambdasToRemove) {
            lambdaMonitor.removeLambdaLink(lambdaLink);
            if (lambdaLink.isAllocated()) {
                ApplicationGroupController.lambdaLogger.info("About to deallocate lambda " + lambdaLink);
                LambdaLinkFactory lambdaLinkFactory = new LambdaLinkFactory(lambdaLink);
                lambdaLinkFactory.deallocate(lambdaLink);
            }
        }
        
        if (!lambdasToAllocate.isEmpty()) {
            ArrayList<FutureTask> lambdaLinkThreads = new ArrayList<>();
            ApplicationGroupController.lambdaLogger.info("Initiating allocation of planned lambda links.");
            for (final LambdaLink ll : lambdasToAllocate) {
                if (ll != null) {
                    if (!ll.isAllocated() && ll.isToAllocate()) {
                        @SuppressWarnings({"unchecked"})
                            FutureTask ft = new FutureTask(new Runnable() {
                                @Override
                                public void run() {
                                    ApplicationGroupController.lambdaLogger.debug("Allocating lambda " + ll);
                                    try {
                                        LambdaLinkFactory lambdaLinkFactory = new LambdaLinkFactory(ll);
                                        lambdaLinkFactory.allocate(ll);
                                    } catch (Exception e) {
                                        ApplicationGroupController.lambdaLogger.error("Lambda link allocation failed for lambda " + ll);
                                        infoOnException(e, null);
                                    }
                                }
                            }, null);
                        lambdaLinkThreads.add(ft);
                    }
                }
            }
            ApplicationGroupController.lambdaLogger.info("Executing allocation for planned lambda links.");
            for (FutureTask futureTask : lambdaLinkThreads) {
                threadPoolExecutor.execute(futureTask);
            }
            
            ApplicationGroupController.lambdaLogger.trace("Collecting allocation results for planned lambda links.");
            if (!lambdaLinkThreads.isEmpty()) {
                for (FutureTask futureTask : lambdaLinkThreads) {
                    //noinspection EmptyCatchBlock
                    if (futureTask != null) {
                        try {
                            futureTask.get();
                        } catch (InterruptedException e) {
                            ApplicationGroupController.logger.debug("Encountered InterruptedException.");
                            infoOnException(e, null);
                        } catch (ExecutionException e) {
                            ApplicationGroupController.logger.debug("Encountered ExecutionException.");
                            infoOnException(e, null);
                        }
                    }
                }
            }
            ApplicationGroupController.lambdaLogger.debug("Allocation results collected for planned lambda links.");
        }
        // </editor-fold>
    }

    /**
     * Starts Application Group Controller
     */
    public void startAGC() {
        if (terminateFlag != null) {
            throw new UnsupportedOperationException("Cannot start AGC: It was already started!");
            // In order to re-launch AGC, create a new ApplicationGroupController!
        }
        
        terminateFlag = new AtomicBoolean(false);
        ApplicationGroupController.logger.info("Starting Application Group Conroller.");
        try {
            // Start local pipe server. There is only one pipe serverrunning at
            //   any time, clients connect to it using this node's ConnectorID
            connector.startLocalPipeServer();
            connector.startReceiving();
        } catch (IOException e) {
            infoOnException(e, "Failed to create AGC input pipe.");
        }

        this.registerJXTAConnectorCallbacks();

        this.lambdaMonitor.startMonitoring();

        try {
            agcPlanThread = new Thread() {

                @Override
                public void run() {
                    agcPlanLoop();
                }
            };
            agcPlanThread.start();
            
            agcPublishThread = new Thread() {
                {
                    setDaemon(true);
                }
                
                @Override
                public void run() {
                    while (! terminateFlag.get()) {
                        sendNetworkTopology();

                        try {
                            sleep(1000);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            };
            agcPublishThread.start();
        } catch (Exception e) {
            infoOnException(e, "Failed to create and run AGC Plan thread.");
        }

    }

    /**
     * Terminates Application Group Controller including some cleanup
     */
    public void stopAGC() {
        ApplicationGroupController.logger.info("Stopping Application Group Controller.");
        terminateFlag.set(true);

        this.lambdaMonitor.stopMonitoring();

        try {
            if (agcPlanThread != null) {
                agcPlanThread.join();
            } else {
                ApplicationGroupController.logger.warn("AGC Plan thread was not started.");
            }
        } catch (InterruptedException e) {
            infoOnException(e, "Failed to interrupt and join AGC Plan thread.");
        }

        try {
            if (agcPublishThread != null) {
                agcPublishThread.join(500);
            } else {
                ApplicationGroupController.logger.warn("AGC Publish thread was not started.");
            }
        } catch (InterruptedException e) {
            infoOnException(e, "Failed to interrupt and join AGC Plan thread.");
        }
    }

    private static void infoOnException(Throwable e, String s) {
        logger.debug(e.getStackTrace());
        logger.error(s);
    }
}
