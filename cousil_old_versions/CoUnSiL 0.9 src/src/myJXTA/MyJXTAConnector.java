package myJXTA;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import static java.text.MessageFormat.format;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.jxta.credential.AuthenticationCredential;
import net.jxta.credential.Credential;
import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredTextDocument;
import net.jxta.endpoint.Message;
import net.jxta.exception.PeerGroupException;
import net.jxta.exception.ProtocolNotSupportedException;
import net.jxta.membership.Authenticator;
import net.jxta.membership.MembershipService;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.DiscoveryResponseMsg;
import net.jxta.protocol.ModuleSpecAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.rendezvous.RendezvousEvent;
import net.jxta.rendezvous.RendezvousListener;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import p2p.CoUniverseMessage;
import p2p.ConnectorID;
import p2p.MessageListener;
import p2p.MessageType;
import p2p.NetworkConnector;
import p2p.NodeGroupIdentifier;
import utils.TimeUtils;

/**
 *
 */
public class MyJXTAConnector extends NetworkConnector {

    static Logger logger = Logger.getLogger(MyJXTAConnector.class);
    public static final int DEFAULT_COUNIVERSE_TCP_PORT = 9701;
    public static final int MAX_DATAGRAM_SIZE_BYTES = 60000;
    public static final int CLIENT_PIPE_OPEN_TIMEOUT = 10000;

    private static final String CoUniversePrincipal = "username";
    private static final String CoUniversePassword = "password";

    private final AtomicBoolean terminate;
    private final AtomicBoolean joinedUniverse;

    private CopyOnWriteArrayList<MessageType> messageTypes;


    private ConcurrentHashMap<InputPipe, Thread> messageReceivers = null;
    private ConcurrentHashMap<InputPipe, Thread> messageHandlers = null;

    private NetworkManager manager;
    private PeerGroup netPeerGroup = null;
    private PeerGroup universePeerGroup = null;

    private boolean allowRdvNode = false;
    private List<URI> rendezvousSeedingUris = new ArrayList<>(), rendezvousUris = new ArrayList<>();

    private MyJXTAMulticastServices multicastServices;
    private MyJXTAPipeServices pipeServices; // TODO: make private

    /**
     * Initializes MyJXTAConnector including initializing some basic network functionality (which should be however
     * very limited in order to keep constructor runtime as low as possible).
     * @param localNodeID ID of the local node
     */
    public MyJXTAConnector(MyJXTAConnectorID localNodeID) {
        super(localNodeID);

        terminate = new AtomicBoolean(true);
        joinedUniverse = new AtomicBoolean(false);
        messageReceivers = new ConcurrentHashMap<>();
        messageHandlers = new ConcurrentHashMap<>();

        if (logger.isDebugEnabled()) {
            addGlobalMessageListener(new MessageListener() {

                @Override
                public void onMessageArrived(CoUniverseMessage message) {
                    logger.debug("MyJXTAConnector debug listener: Received message from " + message.sender + ": " + message);
                }

                @Override
                public String toString() {
                    return "JXTA echo print listener";
                }
            });
        }
    }

    @Override
    public void leaveUniverseInternal() {
        logger.info("Leaving JXTA universe.");

        terminate.set(true);

        // todo MAARA - mas to rozbity, pokud je leaveUniverse zavolana
        // bez predchozi joinUniverse, ocekaval bych noop-like vysledek, ale ono se to kousne
        logger.info("Unregistering DiscoveryListener.");

        logger.info("Stopping universePeerGroup");
        universePeerGroup.stopApp();
        universePeerGroup.unref();
        logger.info("Stoping the network.");
        netPeerGroup.stopApp();
        netPeerGroup.unref();

        // messageReceivers may be wating on receiving messages from pipe
        for (Thread thread : messageReceivers.values()) {
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
        }
        for (Thread thread : messageReceivers.values()) {
            //noinspection EmptyCatchBlock
            try {
                thread.join();
            } catch (InterruptedException e) {
            }
        }
        for (Thread thread : messageHandlers.values()) {
            //noinspection EmptyCatchBlock
            try {
                thread.join();
            } catch (InterruptedException e) {
            }
        }

        logger.info("Leaved JXTA universe.");
    }

    public PeerGroup getNetPeerGroup() {
        return netPeerGroup;
    }

    public PeerGroup getUniversePeerGroup() {
        return universePeerGroup;
    }

    /**
     * Helper method for reporting exceptions in user-friendly way.
     * <p/>
     *
     * @param e exception to report
     * @param s message to log using logger (useful namely when debugging is disabled)
     */
    private static void infoOnException(Throwable e, String s) {
        if (logger.isDebugEnabled()) {
            e.printStackTrace();
        }
        logger.error(s);
    }

    public void setRendezvousEnabled(boolean allowRdvNode) {
        this.allowRdvNode = allowRdvNode;
    }

    // TODO: Currently must be set prior to starting JXTA. Check whether
    //   it would be possible to add them on-the-fly
    public void setRendezvousSeedingUris(List<URI> rendezvousSeedingUris) {
        this.rendezvousSeedingUris = new ArrayList<>(rendezvousSeedingUris);
    }
    public void setRendezvousUris(List<URI> rendezvousUris) {
        this.rendezvousUris = new ArrayList<>(rendezvousUris);
    }

    @Override
    protected void joinUniverseInternal() {
        joinUniverseInternal(DEFAULT_COUNIVERSE_TCP_PORT);
    }

    private void joinUniverseInternal(int tcpPort) {

        terminate.set(false);

        if (rendezvousSeedingUris.size() + rendezvousUris.size() == 0 && (! allowRdvNode) ) {
            throw new IllegalStateException("No rendezvous specified!");
        }

        try {
            logger.info("Configuring platform");
//            logger.debug("RDV_HOME = " + jxtaHome);

            try {
                MyJXTAUtils.deleteFileOrDirectory(new File(".jxta"));
            } catch (IOException e) {
                throw new IOException("Failed to delete JXTA config", e);
            }
            String peerName = null;

            try {
                // todo uuid sem
                peerName = "Collab Universe Peer - " + InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException e) {
                infoOnException(e, "Unable to get local hostname.");
            }

            manager = new NetworkManager(
                    (allowRdvNode ? NetworkManager.ConfigMode.RENDEZVOUS_RELAY : NetworkManager.ConfigMode.EDGE),
                    peerName,
                    (new File("." + System.getProperty("file.separator") + ".jxta" + System.getProperty("file.separator") + "CoUniverse-" + InetAddress.getLocalHost().getHostName())).toURI());

            NetworkConfigurator config = manager.getConfigurator();


            config.setPrincipal(CoUniversePrincipal);
            config.setPassword(CoUniversePassword);
            config.setDescription("Collaborative Universe, " + (allowRdvNode ? "rendezvous peer" : "edge peer"));

            config.setTcpPort(tcpPort);
            config.setTcpEnabled(true);
            config.setTcpIncoming(true);
            config.setTcpOutgoing(true);
            config.setUseMulticast(false);

            logger.info("Rendezvous seeding URIs: "+rendezvousSeedingUris);

            for (URI uri : rendezvousSeedingUris) {
                config.addRdvSeedingURI(uri);
                config.addRelaySeedingURI(uri);
            }

            if (! rendezvousUris.isEmpty()) {
                logger.info("Rendezvous URIs: " + rendezvousUris);
                for (URI uri : rendezvousUris) {
                    config.addSeedRendezvous(uri);
                    config.addSeedRelay(uri);
                }
            }

            try {
                config.save();
            } catch (IOException e) {
                infoOnException(e, "Failed to save config.properties.");
                System.exit(1);
            }

            logger.info("Platform configured and saved");

            logger.info("Starting JXTA platform");


            try {
                netPeerGroup = manager.startNetwork();
            } catch (PeerGroupException e) {
                infoOnException(e, "Failed to start network!");
                System.exit(1);
            }

            logger.info("NetworkManager PeerID = " + netPeerGroup.getPeerID());
            logger.info("netPeerGroup = " + netPeerGroup.getPeerGroupID().toString());

            getNetPeerGroup().getRendezVousService().setAutoStart(allowRdvNode);

            //create and join private group
//            if (allowRdvNode) {
//                netPeerGroup.getRendezVousService().startRendezVous();
//            } else {
//                netPeerGroup.getRendezVousService().setAutoStart(false);
//                connectToRendezVous(netPeerGroup);
//            }

            try {
                universePeerGroup = MyJXTAUtils.createCoUniversePeerGroup(netPeerGroup);
            } catch (Exception e) {
                infoOnException(e, "Failed to create CoUniversePeerGroup!");
            }

            universePeerGroup.startApp(new String[0]);

            getUniversePeerGroup().getRendezVousService().setAutoStart(allowRdvNode);

//            logger.info("Private Application PeerGroup " + universePeerGroup.getPeerGroupName() + " created and published.");
//            try {
//                joinGroup(universePeerGroup);
//            } catch (ProtocolNotSupportedException e) {
//                infoOnException(e, "Failed to join universePeerGroup");
//            } catch (PeerGroupException e) {
//                infoOnException(e, "Failed to join universePeerGroup");
//            }
//

            //rendezvous service
            // Maara: Find out why is rendezvous started on PeerGroup separately
//            if (allowRdvNode) {
//                universePeerGroup.getRendezVousService().startRendezVous();
//            } else {
//                connectToRendezVous(universePeerGroup);
//            }

//            discoveryService = netPeerGroup.getDiscoveryService();

            // Application peer group pipe service
//            if (universePeerGroup != null) {
//                pipeService = universePeerGroup.getPipeService();
//            }

            logger.info("Platform started.");

            this.multicastServices = new MyJXTAMulticastServices(this);

            this.pipeServices = new MyJXTAPipeServices(this);
            this.pipeServices.startServerPipeListener();
            
            System.out.println("Waiting for rendezvous:");
            while (! manager.waitForRendezvousConnection(100)) {
                System.out.println("Waiting for connection to rendezvous!");
            }
            System.out.println("Rendezvous found!");
            
            synchronized (joinedUniverse) {
                joinedUniverse.set(true);
                joinedUniverse.notifyAll();
            }

        } catch (IOException ex) {
            Logger.getLogger(MyJXTAConnector.class.getName()).log(Level.FATAL, null,ex);
        }

    }

    @Override
    public void sendMessageToGroup(CoUniverseMessage message, NodeGroupIdentifier groupIdentifier) {
        multicastServices.sendMessageToGroup(message, groupIdentifier);
    }

    @Override
    public void sendMessageToNode(CoUniverseMessage message) throws IOException {
        // TODO: If the pipe is not opened, open it now or throw exception?
        pipeServices.sendMessage(message);
    }

    @Override
    public void startReceivingFromGroup(NodeGroupIdentifier groupId) {
        multicastServices.startListeningToGroup(groupId);
    }

    @Override
    public void startLocalPipeServer() throws IOException {
        pipeServices.startServerPipeListener();
    }

    @Override
    public void stopLocalPipeServer() {
        MyJXTAConnectorID jxtaID = (MyJXTAConnectorID) getConnectorID();
        pipeServices.stopServerPipeListener(jxtaID.getId());
    }

    @Override
    public void createClientPipe(ConnectorID id) throws IOException {
        MyJXTAConnectorID jxtaID = (MyJXTAConnectorID) id;
        pipeServices.connectClientPipe(jxtaID, CLIENT_PIPE_OPEN_TIMEOUT);
    }

    @Override
    public void sendMessageToClients(CoUniverseMessage message) {
        pipeServices.sendServerMessage(message);
    }
}
