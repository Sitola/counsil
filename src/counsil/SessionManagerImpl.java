/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import couniverse.Main;
import couniverse.core.Core;
import couniverse.core.NetworkNode;
import couniverse.core.NodePropertyParser;
import couniverse.core.mediaApplications.MediaApplication;
import com.fasterxml.jackson.databind.node.ObjectNode;
import couniverse.core.controllers.ApplicationEvent;
import couniverse.core.controllers.ApplicationEventListener;
import couniverse.core.p2p.CoUniverseMessage;
import couniverse.core.p2p.GroupConnectorID;
import couniverse.core.p2p.MessageListener;
import couniverse.core.p2p.MessageType;
import couniverse.monitoring.NodePresenceListener;
import couniverse.monitoring.TopologyAggregator;
import couniverse.monitoring.TopologyUpdate;
import couniverse.ultragrid.UltraGridConsumerApplication;
import couniverse.ultragrid.UltraGridControllerHandle;
import couniverse.ultragrid.UltraGridProducerApplication;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author palci
 */
public class SessionManagerImpl implements SessionManager {

    // TODO 2 different consumers per one node
    // TODO 
    /**
     * Maps consumers on producers Producers are keys
     */
    Map<UltraGridProducerApplication, UltraGridConsumerApplication> producer2consumer = new HashMap<>();

    /**
     * Maps producers to nodes where are running Nodes are keys
     */
    Map<NetworkNode, UltraGridProducerApplication> node2producer = new HashMap<>();

    /**
     *
     */
    Map<UltraGridConsumerApplication, String> consumer2name = new HashMap<>();
    
    /**
     * Shows if current consumer is alerting
     */
    
    Map<UltraGridConsumerApplication, Boolean> consumer2alert = new HashMap<>();
    
    /**
     * Listens for ultragrid windows changes
     */
    couniverse.core.controllers.ApplicationEventListener consumerListener;
    
	/**
     * Stored instance of node representing current computer
     */
    NetworkNode local;
    /**
     * Instance of couniverse Core
     */
    Core core;

    /**
     * represents number of active windows
     */
    int windowCounter = 0;

    /**
     * Instance of LayoutManager to notify Layout about changes
     */
    LayoutManager layoutManager;
    TopologyAggregator topologyAggregator;

    /**
     * Listens alert and permission to talk messages
     */
    MessageListener counsilListener;

    /**
     * Alert message is used for alerting other nodes
     */
    public static MessageType ALERT = MessageType.createCustomMessageType("AlertMessage", "NetworkNode");

    /**
     * Alert message is used for alerting other nodes
     */
    public static MessageType TALK = MessageType.createCustomMessageType("TalkPermissionMessage", "NetworkNode");

    /**
     * Constructor to initialize LayoutManager
     *
     * @param layoutManager
     */
    public SessionManagerImpl(LayoutManager layoutManager) {
        if (layoutManager == null) {
            throw new IllegalArgumentException("layoutManager is null");
        }
        this.layoutManager = layoutManager;
        this.layoutManager.addLayoutManagerListener(new LayoutManagerListener() {

            @Override
            public void alertActionPerformed() {
                //! sem potrebujem pridat meno aktualneho uzlu, je to dobre?
                CoUniverseMessage alert = CoUniverseMessage.newInstance(ALERT, core.getLocalNode());
                System.out.println("Sending alert...");
                core.getConnector().sendMessageToGroup(alert, GroupConnectorID.ALL_NODES);
            }

            @Override
            public void windowChosenActionPerformed(String windowName) {
                //! sem potrebujem pridat meno aktualneho uzlu, je to dobre?
                CoUniverseMessage talk = CoUniverseMessage.newInstance(TALK, core.getLocalNode());
                System.out.println("Sending talk permission...");
                core.getConnector().sendMessageToGroup(talk, GroupConnectorID.ALL_NODES);
            }

            @Override
            public void muteActionPerformed(String windowName) {
                UltraGridConsumerApplication app = getKeyByValue(consumer2name, windowName);
                UltraGridControllerHandle handle = (UltraGridControllerHandle) core.getApplicationControllerHandle(app);
                handle.mute();
            }

            @Override
            public void volumeIncreasedActionPerformed(String windowName) {
                UltraGridConsumerApplication app = getKeyByValue(consumer2name, windowName);
                UltraGridControllerHandle handle = (UltraGridControllerHandle) core.getApplicationControllerHandle(app);
                handle.increaseVolume();
            }

            @Override
            public void volumeDecreasedActionPerformed(String windowName) {
                UltraGridConsumerApplication app = getKeyByValue(consumer2name, windowName);
                UltraGridControllerHandle handle = (UltraGridControllerHandle) core.getApplicationControllerHandle(app);
                handle.decreaseVolume();
            }

            @Override
            public void unmuteActionPerformed(String windowName) {
                UltraGridConsumerApplication app = getKeyByValue(consumer2name, windowName);
                UltraGridControllerHandle handle = (UltraGridControllerHandle) core.getApplicationControllerHandle(app);
                handle.unmute();
            }
        });

    }

    /**
     *
     * Starts producer on local node and listens to other nodes changes init
     * iterates all nodes from initial topology and checks
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Override
    public void initCounsil() throws IOException, InterruptedException {
        // This parse additional attributes from configration file
        NetworkNode.addPropertyParser("agc", NodePropertyParser.STRING_PARSER);
        NetworkNode.addPropertyParser("role", NodePropertyParser.STRING_PARSER);
        NetworkNode.addPropertyParser("windowName", NodePropertyParser.STRING_PARSER);
        core = Main.startCoUniverse();
        

        topologyAggregator = TopologyAggregator.getInstance(core);
        local = core.getLocalNode();
        // create produrer for local content
        createProducent((String) local.getProperty("role"));

        final Object myLock = new Object();
        synchronized (myLock) {
            topologyAggregator.addListener(new NodePresenceListener() {

                @Override
                public void init(Set<NetworkNode> nodes) {
                    for (NetworkNode node : nodes) {
                        onNodeChanged(node, TopologyUpdate.EMPTY_UPDATE);
                    }
                }

                @Override
                public void onNewNodeAppeared(NetworkNode node) {
                    onNodeChanged(node);
                }

                @Override
                public void onNodeChanged(NetworkNode node) {
                    // check name
                    synchronized (myLock) {
                        if (node.equals(local)) {
                            return;
                        }
                        // Check if there is new media application
                        checkProducent(node);
                    }
                }

                @Override
                public void onNodeLeft(NetworkNode node) {
                    stopConsumer(node);
                }
            });
        }

        counsilListener = new MessageListener() {

            @Override
            public void onMessageArrived(CoUniverseMessage message) {
                if (message.type.equals(ALERT)) {
                    System.out.println("Received new message " + message);
                    UltraGridConsumerApplication consumer = producer2consumer.get(node2producer.get((NetworkNode) message.content[0]));
                    String title = consumer2name.get(consumer);
                    System.out.println(title + " is alerting!");

                    try {
                        layoutManager.alert(title);
                    } catch (WDDManException ex) {
                        Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else if (message.type.equals(TALK)) {
                    UltraGridControllerHandle handle = (UltraGridControllerHandle) core.getApplicationControllerHandle(consumer);
                    try {  
                        if (consumer2alert.get(handle)){                        
                            handle.sendCommand("receiver.decoder flush");
                        }
                        else {                        
                            handle.sendCommand("receiver decoder border:width=2:color=#ff0000");                        
                        }
                    } catch (InterruptedException ex) { //!TODO  toto by sa malo poriesit, neviem vsak ako zatial na tieto vynimky reagovat
                        Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (TimeoutException ex) {
                        Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                    }
                   
                }
                else if (message.type.equals(TALK)){
                    System.out.println("Received new message " + message);
                    String title = consumer2name.get(producer2consumer.get(node2producer.get((NetworkNode) message.content[0])));
                    System.out.println(title + " is talking!");
                    //! todo
                }
            }
        };

        core.getConnector().attachMessageListener(counsilListener, ALERT, TALK);     
   
        consumerListener = new ApplicationEventListener() {
            @Override
            public void onApplicationEvent(MediaApplication app, ApplicationEvent event) {
                layoutManager.refresh();
            }

            @Override
            public void onApplicationStop(MediaApplication app, String message) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
    }

    /**
     * Starts distributor on local node
     *
     * @throws IOException if there is problem during starting Distributor
     */
    private void createDistrubutor() throws IOException {
        ObjectNode distConfig = core.newApplicationTemplate("distributor");
        core.startApplication(distConfig, "distributor");
    }

    /**
     * Starts producer from local node
     *
     * @throws IOException if there is problem during starting Producer
     */
    private void createProducent(String role) throws IOException {
        // start distributor
        createDistrubutor();
        // TODO create different producer accourding to role
        ObjectNode prodConfig = core.newApplicationTemplate("producer");
        String identification = local.getUuid().toString();
        prodConfig.put("content", identification);
        prodConfig.put("name", "Producer #" + identification);

        core.startApplication(prodConfig, "producer");
    }

    /**
     * Creates consumer from given producer
     *
     * @param app MediaApplication of producer on remote node
     * @param node remote node where is running producer
     * @return name/title for consumer MediaApplication
     * @throws IOException if there is problem during starting MediaApplication
     * @throws IllegalArgumentException if app or node is null
     */
    private String createConsumer(UltraGridProducerApplication app, NetworkNode node) throws IOException, IllegalArgumentException {
        if (app == null) {
            throw new IllegalArgumentException("app is null");
        }
        if (node == null) {
            throw new IllegalArgumentException("node is null");
        }
        // get content destriptor from producer
        String content = app.getProvidedContentDescriptor();
        ObjectNode cons = core.newApplicationTemplate("consumer");
        // Source is content
        // Naming : "local node":"source node" 
        String name = "\"" + local.getName() + ":" + node.getName() + "\"";
        cons.put("arguments", "--window-title " + name);
        name = name.replace("\"", "");
        cons.put("source", content);
        cons.put("name", name);
        UltraGridConsumerApplication con = (UltraGridConsumerApplication) core.startApplication(cons, "consumer");
        producer2consumer.put(app, con);
        node2producer.put(node, app);
        consumer2name.put(con, name);
        windowCounter++;
        return name;
    }

    /**
     * Check if there is any new MediaApplication running on given node Iterate
     * all node's applications and on these which are UG Producer Check if there
     * is assigned any Consumer app If not assign one == create consumer
     *
     * @param node where I check applications
     * @throws IllegalArgumentException if node is null
     */
    private void checkProducent(NetworkNode node) throws IllegalArgumentException {
        if (node == null) {
            throw new IllegalArgumentException("node is null");
        }
        Set<MediaApplication> applications = node.getApplications();
        for (MediaApplication app : applications) {
            if (app instanceof UltraGridProducerApplication) {
                UltraGridProducerApplication producer = (UltraGridProducerApplication) app;
                try {
                    if (producer2consumer.containsKey(producer) == false) {
                        layoutManager.addNode(createConsumer(producer, node), (String) node.getProperty("role"));
                    }
                } catch (IOException ex) {
                    Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * After receiving leftNode update that node left I will stop
     * UltraGridConsumer
     *
     * @param node
     */
    private void stopConsumer(NetworkNode node) {
        if (node == null) {
            throw new IllegalArgumentException("node is null");
        }
        MediaApplication ugCon = producer2consumer.remove(node2producer.remove(node));
        String removed = consumer2name.get(ugCon);
        if (removed != null) {
            layoutManager.removeNode(removed);
            core.stopApplication(ugCon);
        } else {
            System.out.println("You are trying to stop non-registered application");
        }
    }

    public static UltraGridConsumerApplication getKeyByValue(Map<UltraGridConsumerApplication, String> map, String value) {
        for (Entry<UltraGridConsumerApplication, String> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
}
