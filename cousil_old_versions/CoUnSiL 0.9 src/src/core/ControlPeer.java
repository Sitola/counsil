package core;

import agc.ApplicationGroupController;
import agc.PlanElementBean;
import appControllers.ApplicationEventListener;
import appControllers.ControllerImpl;
import counsil.UserController;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import mediaAppFactory.MediaApplication;
import monitoring.NetworkMonitor;
import monitoring.NetworkMonitorListener;
import myGUI.ControllerFrame;
import myJXTA.MyJXTAConnector;
import myJXTA.MyJXTAConnectorID;
import networkRepresentation.EndpointNetworkNode;
import networkRepresentation.LogicalNetworkLink;
import networkRepresentation.PhysicalNetworkLink;
import networkRepresentation.PhysicalNetworkNode;
import networkRepresentation.UnknownNetworkNode;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import p2p.CoUniverseMessage;
import p2p.MessageListener;
import p2p.MessageType;
import p2p.NetworkConnector;
import p2p.NodeGroupIdentifier;
import utils.MyLogger;
import utils.TimeUtils;

/**
 * Created by IntelliJ IDEA.
 * User: xtlach
 * Date: Apr 11, 2008
 * Time: 2:19:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class ControlPeer {


    private static NetworkConnector networkConnector;

    public static NetworkConnector getNetworkConnector() {
        return networkConnector;
    }
    
    private static final EndpointNetworkNode localNode = new EndpointNetworkNode();
    private static byte[] localNodeInputPipeAdvDoc = null; // local node JXTA input pipe advertisement to communicate with the node

    private static HashMap<MediaApplication, ControllerImpl> localApplicationControllers;

    private final AtomicBoolean terminateFlag, receivedFromAGC;
    private final NetworkMonitor.NetworkMonitorClass DEFAULT_MONITOR_CLASS = new NetworkMonitor.NetworkMonitorClass(10000);
    private final NetworkMonitor.NetworkMonitorClass PRIORITY_MONITOR_CLASS = new NetworkMonitor.NetworkMonitorClass(1000);
    private NetworkMonitor networkMonitor;

    private ApplicationGroupController agc = null;
    private final Object localNodeAdvertisingThreadLock = new Object();
    private Thread localNodeAdvertisingThread = null;
    private final Object networkUpdateListenerLock = new Object();
    private MessageListener networkMonitorListener = null;

    public class UniverseStatusHolder implements Reportable, p2p.MessageListener {
        public static final String KeyTargets = "targets";
        public static final String KeyTargetIp = "ip";
        public static final String KeyTargetMask = "mask";
        public static final String KeyTargetPort = "port";
        public static final String KeyTargetFormat = "format";
        public static final String KeyAppStatus = "status";

        @Override
        public JSONObject reportStatus() throws JSONException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void onMessageArrived(CoUniverseMessage message) {
            //mapVisualizer.update(currentNodes, currentPlan);
            
            switch (message.type) {
                case NETWORK_UPDATE_MESSAGE: {
                    updateNetwork(message.content[0], message.content[3]);
                    break;
                }
                case PLAN_UPDATE_MESSAGE: {
                    updatePlan(message.content[0]);
                    applicationControl().processPlan();
                    PlanElementBean.Plan plan = (PlanElementBean.Plan) message.content[0];
                    plan.printPlan(System.out);
                    break;
                }
            
                
                
                default: {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, "Received yet unsupported message " + message.type.toString());
                }
            }
        }
        
        private void updatePlan(Object plan) {
            assert plan instanceof PlanElementBean.Plan;
            currentPlan = (PlanElementBean.Plan) plan;
        }
        private void updateNetwork(Object nodes, Object links) {
            
            if (nodes == null || !(nodes instanceof ArrayList) ) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, "No network endpoint nodes received from NETWORK_UPDATE_MESSAGE!");
                return;
            }
            if (links == null || !(links instanceof ArrayList) ) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, "No network links received from NETWORK_UPDATE_MESSAGE!");
                return;
            }
            
            universeEndpoints = (ArrayList)nodes;
            universeNetwork = (ArrayList)links;
        }
        
        public synchronized PlanElementBean getPlanFor(MediaApplication app) {
            return currentPlan.getPlanElementBean(app);
            
        }
        public synchronized JSONObject getPlanReportFor(MediaApplication app) throws JSONException {
            JSONObject report = new JSONObject();
            
            if (currentPlan == null) return report;
            PlanElementBean bean = currentPlan.getPlanElementBean(app);
            
            report.put(KeyAppStatus, bean == null ? "suspended" : "running");
            if (bean == null) return report;
            
            if (bean.hasTargets()) {
                JSONArray targets = new JSONArray();
                report.put(KeyTargets, targets);

                for (PlanElementBean.Target t : bean.getTargets()) {
                    JSONObject target = new JSONObject();
                    target.put(KeyTargetIp, t.getTargetIp());
                    target.put(KeyTargetMask, t.getTargetMask());
                    target.put(KeyTargetPort, t.getTargetPort());
                    target.put(KeyTargetFormat, t.getFormat());

                    targets.put(target);
                }
            }
            
            return report;
        }
        
        public synchronized JSONObject reportPlan() throws JSONException {
            if (currentPlan == null) return null;

            JSONObject wholePlan = new JSONObject();

            for (PlanElementBean planElement : currentPlan.values()) {
                JSONObject component = new JSONObject();
                wholePlan.put(planElement.getUuid(), component);

                JSONArray targets = null;
                if (planElement.hasTargets()) {
                    targets = new JSONArray();

                    for (PlanElementBean.Target t : planElement.getTargets()) {
                        JSONObject target = new JSONObject();
                        target.put(KeyTargetIp, t.getTargetIp());
                        target.put(KeyTargetMask, t.getTargetMask());
                        target.put(KeyTargetPort, t.getTargetPort());
                        target.put(KeyTargetFormat, t.getFormat());

                        targets.put(target);
                    }
                }
                component.put(KeyTargets, targets);
            }

            return wholePlan;
        }

        public Collection<EndpointNetworkNode> getUniverseEndpoints() {
            return universeEndpoints;
        }

        public Collection<LogicalNetworkLink> getUniverseNetwork() {
            return universeNetwork;
        }

        public PlanElementBean.Plan getCurrentPlan() {
            return currentPlan;
        }
        
        private ArrayList<EndpointNetworkNode> universeEndpoints = new ArrayList<EndpointNetworkNode>();
        private ArrayList<LogicalNetworkLink> universeNetwork = new ArrayList<LogicalNetworkLink>();
        private PlanElementBean.Plan currentPlan = null;
    }
    
    private class RingBufferLog implements ControllerImpl.OutputReader {
        private final int LogSize = 50;

        AtomicBoolean terminated = new AtomicBoolean(false);
        ArrayList<String> buffer = new ArrayList<>(LogSize);

        public RingBufferLog() {
            buffer.ensureCapacity(LogSize);
        }

        private synchronized void putEntry(String line) {
            if (buffer.size() == LogSize) {
                buffer.remove(0);
            }
            buffer.add(line);
        }
        public synchronized String getLog(){
            StringBuilder log = new StringBuilder();
            for (String line : buffer) {
                log.append(line);
            }
            return log.toString();
        }

        private class LogWriter extends Thread {
            BufferedReader log = null;
            String prefix = null;

            public LogWriter(String prefix, InputStream logStream) {
                this.prefix = prefix;
                log = new BufferedReader(new InputStreamReader(logStream));
            }

            @Override
            public void run() {
                try {
                    while (!terminated.get()) {
                        String line = log.readLine();
                        putEntry(prefix+": "+line+"\n");
                    }
                } catch (IOException ex) {
                    Logger.getLogger(this.getClass()).error("IO exception in UG monitoring thread");
                }
            }
        }

        public void attachStderr(InputStream is) {
            new LogWriter("STDERR", is).start();
        }
        public void attachStdout(InputStream is){
            new LogWriter("STDOUT", is).start();
        }
        public void detachStderr() {
            terminated.set(true);
        }
        public void detachStdout() {
            terminated.set(true);
        }
    }

    public class AppControllerManager extends Thread implements Reportable  {
        
        public static final String KeyWholePlan = "wholePlan";
        public static final String KeyLocalDetail = "localApplications";
        public static final String KeyAppName = "name";
        public static final String KeyAppDesc = "desc";

        private void putApplication(MediaApplication application, ControllerImpl ctrl) {
            applicationControllers.put(application, ctrl);
            localNode.getApplicationManager().addApplication(application);
        }
        private void dropApplication(MediaApplication application) throws IllegalArgumentException {
            applicationRedirects.remove(application);
            localNode.getApplicationManager().removeApplication(application);
            applicationControllers.remove(application);
        }
            
        private final HashMap<MediaApplication, ControllerImpl> applicationControllers = new HashMap<MediaApplication, ControllerImpl>();
        private final HashMap<MediaApplication, RingBufferLog> applicationRedirects = new HashMap<>();

        /**
         * Run application on this node.
         * @param application - new application to be run
         * @throws IllegalArgumentException when application is already running
         * @throws IOException when application controller fails to start
         */
        public synchronized void runMediaApplication(MediaApplication application) throws IllegalArgumentException, IOException {
            if (applicationControllers.get(application) != null) {
                throw new IllegalArgumentException("Cannot run <"+application.getApplicationName()+">, it is already running!");            
            }

            ControllerImpl ctrl = mediaAppFactory.ApplicationFactory.newController(application);

            try {
                RingBufferLog log = new RingBufferLog();
                applicationRedirects.put(application, log);
                ctrl.setReaders(log);
                ctrl.runApplication();
                putApplication(application, ctrl);
            } catch (IOException ex) {
                dropApplication(application);
                ctrl.stopApplication();
                throw new IOException("Failed to run process for <"+application.getApplicationName()+">: " + ex.toString(), ex);
            }
            
            if (ctrl != null) {
                ApplicationEventListener listener = new ApplicationEventListener() {
                    @Override
                    public void onDebugMessage(final Object message) {
                        
                        CoUniverseMessage couniverseMessage = new CoUniverseMessage(
                                MessageType.DEBUG_LOG_MESSAGE, 
                                (Serializable[]) message,
                                networkConnector.getConnectorID(), 
                                null);
                        
                        networkConnector.sendMessageToGroup(couniverseMessage, NodeGroupIdentifier.AGC_CAPABLE_NODES);
                    }
                };
                ctrl.registerApplicationStatusListener(listener);
            }
            
            sendNodeInformations();
        }

        private synchronized void stopMediaApplicationInternal(MediaApplication app) throws IllegalArgumentException {
            ControllerImpl ctrl = applicationControllers.get(app);
            if (ctrl == null) {
                throw new IllegalArgumentException("Attempted to stop <"+app.getApplicationName()+"> which is not registered!");
            }

            dropApplication(app);
            ctrl.stopApplication();
        }
        public synchronized void stopMediaApplication(MediaApplication app) throws IllegalArgumentException {
            stopMediaApplicationInternal(app);
            sendNodeInformations();
        }

        public void stopMediaApplications() {
    //        this.waitForQuit();
            for (MediaApplication nodeApplication : localNode.getNodeApplications()) {
                assert nodeApplication != null;
                stopMediaApplicationInternal(nodeApplication);
            }
            sendNodeInformations();
        }

        public String statusMediaApplication(MediaApplication app) {
            RingBufferLog log = applicationRedirects.get(app);
            if (log == null) return null;
            return log.getLog();
        }

        public Collection<MediaApplication> getMediaApplications() {
            return localNode.getNodeApplications();
        }

        public MediaApplication getAppByUuidOrName(String identifier) {
            return mediaAppFactory.ApplicationFactory.filterByUuidOrName(identifier, getMediaApplications());
        }
        
        public void processPlan(){

            // @todo one day, agc might start remote applications... but not this day. That is why localNodeApplications are scanned
            for (MediaApplication localApp : localNode.getNodeApplications()) {
                PlanElementBean planPatchElement = universeStatus.getPlanFor(localApp);
                try {
                    ControllerImpl controller = applicationControllers.get(localApp);
                    if (controller == null) {
                        Logger.getLogger(this.getClass()).log(Level.FATAL, "Inconsistency between application lists held by NetworkNode and controller list detected!!! No controller for app " + localApp.getUuid() + " found amongst local controllers!");
                        continue;
                    }
                    controller.applyPatch(planPatchElement);
                } catch (IOException ex) {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, "Failed to apply patch for <"+localApp+">: <"+planPatchElement+">", ex);
                }
            }
        }
        
        @Override
        public JSONObject reportStatus() throws JSONException {
            JSONObject status = new JSONObject();
            
            for (Map.Entry<MediaApplication, ControllerImpl> entry : applicationControllers.entrySet()) {
                JSONObject appStatus = new JSONObject();
                status.put(entry.getKey().getUuid().toString(), appStatus);
                
                appStatus.put("requested", entry.getKey().activeConfig());
                appStatus.put("plan", universeStatus.getPlanReportFor(entry.getKey()));
            }
            
            return status;
        }

        public JSONObject reportDetail(MediaApplication app) throws JSONException {
            JSONObject status = new JSONObject();
            
            JSONObject appStatus = new JSONObject();
            status.put(app.getUuid().toString(), appStatus);

            appStatus.put("requested", app.activeConfig());
            appStatus.put("capable", app.reportStatus());
            appStatus.put("plan", universeStatus.getPlanReportFor(app));
            
            return status;
        }
        
        @Override
        public String toString() {
            try {
                return reportStatus().toString(2);
            } catch (JSONException ex) {
                return getClass().getCanonicalName() + ": error reading state, reason: " + ex.toString();
            }
        }
        
    }
    
    private final UniverseStatusHolder universeStatus = new UniverseStatusHolder();
    private final AppControllerManager localAppControl = new AppControllerManager();

    // @todo - tuto hruzu vysekat, zase gui
    //private ControllerFrame controllerFrame;

    // The name of the logger needs to be referenced as String as it is not part of any package and thus not referable from MyLogger
    static Logger logger = Logger.getLogger("GuiUniversePeer");

    static {
        MyLogger.setup();
    }

    public ControlPeer() {
        MyLogger.setup();
        assert localNode != null;

        terminateFlag = new AtomicBoolean(false);
        receivedFromAGC = new AtomicBoolean(false);

        networkMonitor = new NetworkMonitor();
        networkMonitor.addMonitoringClass(DEFAULT_MONITOR_CLASS);
        networkMonitor.addMonitoringClass(PRIORITY_MONITOR_CLASS);
        networkMonitor.addNetworkMonitorListenerForAll(new NetworkMonitorListener() {
            // this is to be called when network link is found non-functional
            @Override
            public void onNetworkLinkLost(LogicalNetworkLink networkLink) {
                ControlPeer.logger.info("" + networkLink.getToInterface().getIpAddress() + " is unreachable.");
                ControlPeer.logger.info("Sending NetworkNode Unreachable message to AGC.");
                CoUniverseMessage message = new CoUniverseMessage(
                    MessageType.NODE_UNREACHABLE_MESSAGE, 
                    new Serializable[]{networkLink}, 
                    networkConnector.getConnectorID(), 
                    null);
                try {
                    networkConnector.sendMessageToAgc(message);
                } catch (IOException ex) {
                    logger.warn("Failed to send NODE_UNREACHABLE_MESSAGE to agc", ex);
                }
            }

            // this is to be called when network link is found functional again
            @Override
            public void onNetworkLinkReestablished(LogicalNetworkLink networkLink) {
                ControlPeer.logger.info("" + networkLink.getToInterface().getIpAddress() + " is reachable.");
                ControlPeer.logger.info("Sending NetworkNode Reachable message to AGC.");
                CoUniverseMessage message = new CoUniverseMessage(
                    MessageType.NODE_REACHABLE_MESSAGE, 
                    new Serializable[]{networkLink}, 
                    networkConnector.getConnectorID(), 
                    null);
                try {
                    networkConnector.sendMessageToAgc(message);
                } catch (IOException ex) {
                    logger.warn("Failed to send NODE_UNREACHABLE_MESSAGE to agc", ex);
                }
            }

            @Override
            public void onNetworkLinkFlap(LogicalNetworkLink networkLink) {
                ControlPeer.logger.info("" + networkLink.getToInterface().getIpAddress() + " is flapping.");
            }

            // this is to be called when the link latency differs significantly
            @Override
            public void onNetworkLinkLatencyChange(LogicalNetworkLink networkLink) {
                ControlPeer.logger.info("" + networkLink.getToInterface().getIpAddress() + " changed latency significantly.");
                ControlPeer.logger.info("Sending LogicalNetworkLink latency change message to AGC.");
                CoUniverseMessage message = new CoUniverseMessage(
                    MessageType.LINK_LATENCY_CHANGED_MESSAGE, 
                    new Serializable[]{networkLink}, 
                    networkConnector.getConnectorID(), 
                    null);
                try {
                    networkConnector.sendMessageToAgc(message);
                } catch (IOException ex) {
                    logger.warn("Failed to send NODE_UNREACHABLE_MESSAGE to agc", ex);
                }
//                try {
//                    networkConnector.sendReliableMesssage(agcOutputPipe, MessageType.LINK_LATENCY_CHANGED_MESSAGE, networkLink);
//                } catch (IOException e) {
//                    //infoOnException(e, "Failed to send link latency change message to AGC.");
//                }
            }
        });

        // NOTE: The call will fail upon invocation, as the ConnectorID must not be null
        networkConnector = new MyJXTAConnector(null);
    }

    public AppControllerManager applicationControl() {
        return localAppControl;
    }

    public UniverseStatusHolder getLatestUniverseState() {
        return universeStatus;
    }

    public synchronized void leaveUniverse() {
        // gonzales - najit toho *#! co sem napsala joinThreads
//        this.joinThreads();
        ControlPeer.logger.info("Disconnecting universe");
        terminateFlag.set(true);

        // Send a network node removed information to AGC
        ControlPeer.logger.info("Sending localNetworkNode removed information to AGC.");
        try {
            networkConnector.sendMessageToGroup( new CoUniverseMessage(MessageType.REMOVE_NODE_MESSAGE, new Serializable[]{localNode}, networkConnector.getConnectorID(), null), NodeGroupIdentifier.ALL_NODES);
        }
        catch (Exception e) {
        }

        // this is to help the main thread do the cleanup

        // todo gonzales ugly timeout, cleanup timeout for main shall be in main
        TimeUtils.sleepFor(1000);
        networkConnector.leaveUniverse();
        stopMonitoring();
        ControlPeer.logger.info("Disconnected");
    }

    public void startLocalNodeAdvertisingThread() {

        synchronized (localNodeAdvertisingThreadLock) {
         
            if (localNodeAdvertisingThread != null) {
                logger.error("Overwriting a running local node advertising thread!");
            }
            
            // Current approach does not prevent losing handle of a  running 
            //   thread, hence the thread is set to be daemon to allow shutdown.
            // TODO: Prevent creation of multiple advertising threads
            localNodeAdvertisingThread = null;
            try {
                localNodeAdvertisingThread = new Thread() {

                    {
                        this.setDaemon(true);
                    }
                    
                    @Override
                    public String toString() {
                        return "Local node advertising thread";
                    }

                    @Override
                    public void run() {
                        while (!terminateFlag.get()) {
                            ControlPeer.logger.info("Local Node Advertising thread loop (" + Thread.currentThread().toString()+")");
                            sendNodeInformations();
                            if (receivedFromAGC.get()) {
                                TimeUtils.sleepFor(120000);
                            } else {
                                TimeUtils.sleepFor(5000);
                            }
                        }
                    }
                };
                localNodeAdvertisingThread.start();
            } catch (Exception e) {
                infoOnException(e, "Failed to run localNode Advertising thread");
            }
        }
    }

    /**
     * Connect to the JXTA universe. At least one uri must be specified
     * @param rendezvousSeedingUris List of uris, where the currently running rendezvous uris can be downloaded from
     * @param rendezvousUris List of uris where actual rendezvous nodes can be located
     */
    public void connectToJxtaUniverse(List<URI> rendezvousSeedingUris, List<URI> rendezvousUris, boolean enableRendezvous, boolean enableAgc) {
        assert localNode != null;
        assert localNode.getNodePeerID() instanceof MyJXTAConnectorID;

        MyJXTAConnector jxtaConnector = new MyJXTAConnector((MyJXTAConnectorID)localNode.getNodePeerID());

        rendezvousSeedingUris = new ArrayList<>();
        jxtaConnector.setRendezvousSeedingUris(rendezvousSeedingUris);
        jxtaConnector.setRendezvousUris(rendezvousUris);

        jxtaConnector.setRendezvousEnabled(enableRendezvous);

        synchronized (this) {
            networkConnector = jxtaConnector;

            networkConnector.joinUniverse(enableAgc);
            networkConnector.startReceiving();
            networkConnector.startReceivingFromGroup(NodeGroupIdentifier.ALL_NODES);
            networkConnector.addMessageListener(MessageType.PLAN_UPDATE_MESSAGE, universeStatus);
            networkConnector.addMessageListener(MessageType.NETWORK_UPDATE_MESSAGE, universeStatus);
            
            
            networkConnector.addMessageListener(MessageType.NETWORK_UPDATE_MESSAGE, UserController.getInstance() );
            networkConnector.addMessageListener(MessageType.COUNSIL_NETWORK_UPDATE, UserController.getInstance() );
            networkConnector.addMessageListener(MessageType.COUNSIL_CAN_NOT_TALK, UserController.getInstance() );
            networkConnector.addMessageListener(MessageType.COUNSIL_CAN_TALK, UserController.getInstance() );
            networkConnector.addMessageListener(MessageType.COUNSIL_DO_NOT_WANT_TO_TALK, UserController.getInstance() );
            networkConnector.addMessageListener(MessageType.COUNSIL_STOPPED_TALKING, UserController.getInstance() );
            networkConnector.addMessageListener(MessageType.COUNSIL_WANT_TO_TALK, UserController.getInstance() );
            networkConnector.addMessageListener(MessageType.COUNSIL_PRODUCER_STARTED, UserController.getInstance() );
        }
        
        startLocalNodeAdvertisingThread();
        startMonitoring();
    }

    @Deprecated
    public void runApplications(ControllerFrame localControllerFrame) {
        // Create a local application controller and add it to the localApplicationControllers hashMap
/**
        localApplicationControllers = new AppControllerManager();
        this.controllerFrame = localControllerFrame;

        if (!localNode.getNodeApplications().isEmpty()) {
            for (MediaApplication nodeApplication : localNode.getNodeApplications()) {
                runApplication(nodeApplication);
            }
        }

        for (ControllerImpl controller : localApplicationControllers.values()) {
            if (localController == null) continue;
            localControllerFrame.addController(localController, localController.getApplicationProxy());
            localController.setControllerFrame(localControllerFrame);
        }

        //localControllerFrame.setSize(localControllerFrame.getPreferredSize());
        //localControllerFrame.setVisible(true);

        if (!localNode.getNodeApplications().isEmpty()) {
            for (MediaApplication nodeApplication : localNode.getNodeApplications()) {
                assert nodeApplication != null;
                if (nodeApplication instanceof Rum) {
                    // Get controller for the mediaApp
                    assert localApplicationControllers != null;
                    ControllerImpl ctrl = localApplicationControllers.get(nodeApplication);
                    ControlPeer.logger.info("Starting local node application: " + nodeApplication + " " + nodeApplication.getApplicationCmdOptions());

                    try {
                        ctrl.runApplication();
                        ControlPeer.logger.info("" + nodeApplication + " started.");
                    } catch (ApplicationProxy.ApplicationProxyException e) {
                        infoOnException(e, "Failed to start local MediaApplication: " + nodeApplication + " " + nodeApplication.getApplicationCmdOptions());
                    }
                }
            }
        }
*/
    }

    public void startMonitoring() {
        
        synchronized (networkUpdateListenerLock) {
            this.networkMonitor.startMonitoring();
    
            if (networkMonitorListener != null) {
                networkConnector.removeMessageListener(networkMonitorListener, MessageType.NETWORK_UPDATE_MESSAGE);
            }
            
            networkMonitorListener = new MessageListener() {

                @SuppressWarnings({"unchecked"})
                @Override
                public void onMessageArrived(CoUniverseMessage message) {
                    // TODO: Paralell access to monitor not controlled!
                    if (! receivedFromAGC.get()) receivedFromAGC.set(true);

                    ControlPeer.logger.debug("Received NetworkTopology Update Message");
                    ArrayList<EndpointNetworkNode> endpointNetworkNodes = (ArrayList<EndpointNetworkNode>) message.content[0];
                    ArrayList<PhysicalNetworkNode> physicalNetworkNodes = (ArrayList<PhysicalNetworkNode>) message.content[1];
                    ArrayList<UnknownNetworkNode> unknownNetworkNodes = (ArrayList<UnknownNetworkNode>) message.content[2];
                    ArrayList<LogicalNetworkLink> logicalNetworkLinks = (ArrayList<LogicalNetworkLink>) message.content[3];
                    ArrayList<PhysicalNetworkLink> physicalNetworkLinks = (ArrayList<PhysicalNetworkLink>) message.content[4];
    //                ControlPeer.logger.info("Received NetworkTopology with " + endpointNetworkNodes.size() + " endpoint nodes, "
    //                                                                           + physicalNetworkNodes.size() + " physical nodes, "
    //                                                                           + unknownNetworkNodes.size() + " unknown network nodes, "
    //                                                                           + logicalNetworkLinks.size() + " logical edges and "
    //                                                                           + physicalNetworkLinks.size() + "physical edges.");
                    ControlPeer.logger.info("Topology details: " + endpointNetworkNodes.size() + " endpoints, " +
                        logicalNetworkLinks.size() + " logical links, " +
                        physicalNetworkNodes.size() + " physical nodes, " +
                        physicalNetworkLinks.size() + " physical links and " +
                        unknownNetworkNodes.size() + " unknown nodes.");
                    for (EndpointNetworkNode node : endpointNetworkNodes) {
                        ControlPeer.logger.info("  "+node.getNodeName());
                        for (MediaApplication app : node.getNodeApplications()) {
                            ControlPeer.logger.info("    "+app);
                        }
                    }
                    for (LogicalNetworkLink llink : logicalNetworkLinks) {
                        logger.info("  " + llink);
                    }

                    Set<LogicalNetworkLink> monitoredLinks = networkMonitor.getMonitoredLinks();
                    for (LogicalNetworkLink logicalLink : logicalNetworkLinks) {
                        if (logicalLink.getFromNode().equals(localNode) && !monitoredLinks.contains(logicalLink)) {

                            logger.debug("Adding network link for monitiring: " + logicalLink);
                            networkMonitor.addNetworkLink(logicalLink, DEFAULT_MONITOR_CLASS);
                            // register callbacks here if the global callback is not enough
                        }
                    }
                    for (LogicalNetworkLink monitoredLink : monitoredLinks) {
                        // remove the link only if it belongs to the DEFAULT_MONITOR_CLASS - it may belong to PRIORITY_MONITOR_CLASS, which
                        // is not being handled here by this message
                        if (!logicalNetworkLinks.contains(monitoredLink) && networkMonitor.getNetworkMonitorClass(monitoredLink).equals(DEFAULT_MONITOR_CLASS)) {
                            networkMonitor.removeNetworkLink(monitoredLink);
                        }
                    }
                }
            };
            networkConnector.addMessageListener(MessageType.NETWORK_UPDATE_MESSAGE, networkMonitorListener);
        }
    }
    
    public void stopMonitoring() {
        
        synchronized (networkUpdateListenerLock) {
            networkConnector.removeMessageListener(networkMonitorListener, MessageType.NETWORK_UPDATE_MESSAGE);
            networkMonitorListener = null;
            this.networkMonitor.stopMonitoring();
        }
    }

    public void joinThreads() {
        synchronized (localNodeAdvertisingThreadLock) {
            if (localNodeAdvertisingThread != null) {
                try {
                    ControlPeer.logger.info("Joining localNode Advertising thread.");
                    localNodeAdvertisingThread.join(1000);
                } catch (InterruptedException e) {
                    infoOnException(e, "Failed to join localNode Advertising thread.");
                } catch (NullPointerException e) {
                    ControlPeer.logger.info("Failed to join localNodeAdvertising thread");
                }
            }
            // TODO: Losing handle of a daemon thread that may be still alive
            localNodeAdvertisingThread = null;
        }
    }

    private static void infoOnException(Throwable e, String s) {
        e.printStackTrace();
        if (logger.isDebugEnabled()) {
        }
        logger.error(s);
    }
    
    void sendNodeInformations() {
        ControlPeer.logger.debug("Sending localNetworkNode information to AGC.");
        try {
            CoUniverseMessage message = new CoUniverseMessage(
                MessageType.NEW_ENDPOINT_NODE_MESSAGE, 
                new Serializable[]{localNode},
                networkConnector.getConnectorID(),
                null);
            networkConnector.sendMessageToAgc(message);
        }
        catch (Exception e) {
            //infoOnException(e, "Failed to send localNetworkNode information to AGC.");
        }
    }

    public EndpointNetworkNode getLocalNode() {
        return localNode;
    }
    

}
