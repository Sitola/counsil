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
import couniverse.gui.display.NodeGraphics;
import couniverse.monitoring.NodePresenceListener;
import couniverse.monitoring.TopologyAggregator;
import couniverse.ultragrid.UltraGridConsumerApplication;
import couniverse.ultragrid.UltraGridControllerHandle;
import couniverse.ultragrid.UltraGridProducerApplication;
import java.awt.EventQueue;
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
    Map<NetworkNode, UltraGridProducerApplication[]> node2producer = new HashMap<>();

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
     * Instance of LayoutManager to notify Layout about changes
     */
    LayoutManager layoutManager;
    TopologyAggregator topologyAggregator;

    /**
     * Lock for gui and other stuff
     */
    final private Object eventLock = new Object();

    /**
     * Listens alert and permission to talk messages
     */
    MessageListener counsilListener;

    /**
     * Checks if someone is talking in this moment
     */
    private static Boolean isTalking;

    /**
     * Currently talking node
     */
    private static NetworkNode talkingNode;

    /**
     * Alert message is used for alerting other nodes
     */
    public static MessageType ALERT = MessageType.createCustomMessageType("AlertMessage", "NetworkNode");

    /**
     * StopAlert message is used for stopping alerting other nodes
     */
    public static MessageType STOPALERT = MessageType.createCustomMessageType("StopAlertMessage", "NetworkNode");

    /**
     * Talk message is used for granting talk permission
     */
    public static MessageType TALK = MessageType.createCustomMessageType("TalkPermissionGrantedMessage", "NetworkNode");

    /**
     * StopTalk message is used for removing talk right other nodes
     */
    public static MessageType STOPTALK = MessageType.createCustomMessageType("TalkPermissionRemovedMessage", "NetworkNode");

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
            public void alertActionPerformed(Boolean wasAlerted) {
                if (wasAlerted) {
                    CoUniverseMessage alert = CoUniverseMessage.newInstance(ALERT, core.getLocalNode());
                    System.out.println("Sending alert...");
                    core.getConnector().sendMessageToGroup(alert, GroupConnectorID.ALL_NODES);
                } else {
                    CoUniverseMessage stopalert = CoUniverseMessage.newInstance(STOPALERT, core.getLocalNode());
                    System.out.println("Sending stopalert...");
                    core.getConnector().sendMessageToGroup(stopalert, GroupConnectorID.ALL_NODES);
                }

            }

            @Override
            public void windowChoosenActionPerformed(String windowName) {
                NetworkNode choosenNode = getNetworkNodeByProducer(getProducerByConsumer(getConsumerByTitle(windowName)));
                if (!isTalking) {
                    CoUniverseMessage talk = CoUniverseMessage.newInstance(TALK, choosenNode);
                    Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, "Sending talk permission granted for node {0}...", windowName);
                    core.getConnector().sendMessageToGroup(talk, GroupConnectorID.ALL_NODES);
                    talkingNode = choosenNode;
                    isTalking = true;
                } else {
                    CoUniverseMessage stoptalk = CoUniverseMessage.newInstance(STOPTALK, choosenNode);
                    Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, "Sending talk permission removed for node {0}...", windowName);
                    core.getConnector().sendMessageToGroup(stoptalk, GroupConnectorID.ALL_NODES);
                    talkingNode = null;
                    isTalking = false;
                }

            }

            @Override
            public void muteActionPerformed(String windowName) {
                UltraGridConsumerApplication app = getKeyByValue(consumer2name, windowName);
                if (app != null) {
                    UltraGridControllerHandle handle = (UltraGridControllerHandle) core.getApplicationControllerHandle(app);
                    if (handle != null) {
                        handle.mute();
                    }
                }
            }

            @Override
            public void volumeIncreasedActionPerformed(String windowName) {
                UltraGridConsumerApplication app = getKeyByValue(consumer2name, windowName);
                if (app != null) {
                    UltraGridControllerHandle handle = (UltraGridControllerHandle) core.getApplicationControllerHandle(app);
                    if (handle != null) {
                        handle.increaseVolume();
                    }
                }
            }

            @Override
            public void volumeDecreasedActionPerformed(String windowName) {
                UltraGridConsumerApplication app = getKeyByValue(consumer2name, windowName);
                if (app != null) {
                    UltraGridControllerHandle handle = (UltraGridControllerHandle) core.getApplicationControllerHandle(app);
                    if (handle != null) {
                        handle.decreaseVolume();
                    }
                }

            }

            @Override
            public void unmuteActionPerformed(String windowName) {
                UltraGridConsumerApplication app = getKeyByValue(consumer2name, windowName);
                if (app != null) {
                    UltraGridControllerHandle handle = (UltraGridControllerHandle) core.getApplicationControllerHandle(app);
                    if (handle != null) {
                        handle.unmute();
                    }
                }
            }
        });

        isTalking = false;

    }

    /**
     * gets producer from consumer (in producer2consumer) map
     *
     * @param consumer
     * @return
     */
    private UltraGridProducerApplication getProducerByConsumer(UltraGridConsumerApplication consumer) {
        for (Entry<UltraGridProducerApplication, UltraGridConsumerApplication> entrySet : producer2consumer.entrySet()) {
            if (consumer.equals(entrySet.getValue())) {
                return entrySet.getKey();
            }
        }
        return null;
    }

    /**
     * gets network node from producer (in node2producer) map
     *
     * @param producer
     * @return
     */
    private NetworkNode getNetworkNodeByProducer(UltraGridProducerApplication producer) {
        for (Entry<NetworkNode, UltraGridProducerApplication[]> entrySet : node2producer.entrySet()) {
            for (UltraGridProducerApplication ug : entrySet.getValue()) {
                if (producer.equals(ug)) {
                    return entrySet.getKey();
                }
            }
        }
        return null;
    }

    /**
     * gets consumer from title (in consumer2name) map
     *
     * @param title
     * @return
     */
    private UltraGridConsumerApplication getConsumerByTitle(String title) {
        for (Entry<UltraGridConsumerApplication, String> entrySet : consumer2name.entrySet()) {
            if (title.equals(entrySet.getValue())) {
                return entrySet.getKey();
            }
        }
        return null;
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
        NetworkNode.addPropertyParser("videoProducer", NodePropertyParser.STRING_PARSER);
        NetworkNode.addPropertyParser("audioProducer", NodePropertyParser.STRING_PARSER);
        NetworkNode.addPropertyParser("audioConsumer", NodePropertyParser.STRING_PARSER);
        NetworkNode.addPropertyParser("presentationProducer", NodePropertyParser.STRING_PARSER);
        NetworkNode.addPropertyParser("videoConsumer", NodePropertyParser.STRING_PARSER);

        core = Main.startCoUniverse();

        topologyAggregator = TopologyAggregator.getInstance(core);
        local = core.getLocalNode();
        // create produrer for local content
        createProducent((String) local.getProperty("role"));

        Object listenerLock = new Object();
        synchronized (listenerLock) {
            topologyAggregator.addListener(new NodePresenceListener() {

                @Override
                public void init(Set<NetworkNode> nodes) {
                    for (NetworkNode node : nodes) {
                      //  synchronized (eventLock) {
                            onNodeChanged(node);
                      //  }
                    }
                }

                @Override
                public void onNewNodeAppeared(NetworkNode node) {
                    //synchronized (eventLock) {
                        onNodeChanged(node);
                    //}
                }

                @Override
                public void onNodeChanged(NetworkNode node) {          
                    // Check if there is new media application
                    checkProducent(node);
                }

                @Override
                public void onNodeLeft(NetworkNode node) {
                   // synchronized (eventLock) {
                        // tell clints to refresh if it was currently used node                       
                        System.err.println("Node " + node.getName() + " left!!!!!!!!!!!");   
                        if (isTalking){
                            String nodeName = consumer2name.get(producer2consumer.get(node2producer.get(node)));                          
                            if (nodeName != null && (((talkingNode != null) && (node.getName().equals(talkingNode.getName())))||(nodeName.toUpperCase().contains("TEACHER")))){ 
                                System.err.println("I am leaving!");
                                layoutManager.refreshToDefaultLayout();
                                isTalking = false;
                                talkingNode = null;
                            }   
                        }        
                        stopConsumer(node);
                    
                }
            });
        }

        //synchronized (listenerLock) {
            counsilListener = new MessageListener() {

                // catching alerting messages
                @Override
                public void onMessageArrived(CoUniverseMessage message) {
                    Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, "Received new message " + message);
                    if (message.type.equals(ALERT)) {
                        UltraGridConsumerApplication consumer = producer2consumer.get(node2producer.get((NetworkNode) message.content[0])[0]);

                        if (consumer != null) {
                            // get application handle and draw/remove border
                            UltraGridControllerHandle handle = ((UltraGridControllerHandle) core.getApplicationControllerHandle(consumer));
                            if (handle != null) {
                                try {
                                    handle.sendCommand("postprocess border:width=5:color=#ff0000");
                                    consumer2alert.replace(consumer, true);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (TimeoutException ex) {
                                    Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }

                    } else if (message.type.equals(STOPALERT)) {
                        UltraGridConsumerApplication consumer = producer2consumer.get(node2producer.get((NetworkNode) message.content[0])[0]);
                        if (consumer != null) {
                            // get application handle and draw/remove border
                            UltraGridControllerHandle handle = ((UltraGridControllerHandle) core.getApplicationControllerHandle(consumer));
                            if (handle != null) {
                                try {
                                    handle.sendCommand("postprocess flush");
                                    consumer2alert.replace(consumer, false);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (TimeoutException ex) {
                                    Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }

                    } else if (message.type.equals(TALK)) {
                        String title = consumer2name.get(producer2consumer.get(node2producer.get((NetworkNode) message.content[0])[0]));
                      //  synchronized (eventLock) {
                            layoutManager.swapPosition(title);
                       // }
                        isTalking = true;
                        talkingNode = (NetworkNode) message.content[0];
                    } else if (message.type.equals(STOPTALK)) {
                       // synchronized (eventLock) {
                            layoutManager.refreshToDefaultLayout();
                        //}
                        isTalking = false;
                        talkingNode = null;
                    }
                }
            };

            // define message types
            core.getConnector().attachMessageListener(counsilListener, ALERT, TALK, STOPALERT, STOPTALK);
        //}

      /*  synchronized (listenerLock) {
            // refreshes layout on consumer restart
            consumerListener = new ApplicationEventListener() {
                @Override
                public void onApplicationEvent(MediaApplication app, ApplicationEvent event) {
                    synchronized (eventLock) {
                        layoutManager.refresh();
                    }
                }

                @Override
                public void onApplicationStop(MediaApplication app, String message) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            };
        }
*/
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(30 * 1000);
                        EventQueue.invokeLater(() -> layoutManager.refresh());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
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
        String producer = (String) local.getProperty("videoProducer");
        if (producer == null) {
            throw new IllegalArgumentException("Specify video producer in config");
        }
        createProducer(TypeOfContent.VIDEO, producer, role);

        // create audio producer
        // this require 1 teacher and 1 intepreter because of distributor 
        if (isInterpreterOrTeacher(role)) {
            String audio = (String) local.getProperty("audioProducer");
            if (audio == null) {
                throw new IllegalArgumentException("Specify audio in config");
            }
            createProducer(TypeOfContent.SOUND, audio, role);
        }
        if (role.equals("teacher")) {
            String pres = (String) local.getProperty("presentationProducer");
            if (pres == null) {
                throw new IllegalArgumentException("Specify presentation in config");
            }
            createProducer(TypeOfContent.PRESENTATION, pres, role);
        }
    }

    private void createProducer(TypeOfContent type, String settings, String role) throws IOException {
        String PRODUCER = "producer";
        ObjectNode prodConfig = core.newApplicationTemplate(PRODUCER);
        String identification = local.getName().toString() + "-" + type.toString() + "-" + role;
        prodConfig.put("content", identification);
        switch (type) {
            case SOUND:
                prodConfig.put("audio", settings);
                break;
            case PRESENTATION:
                prodConfig.put("video", settings);
                break;
            case VIDEO:
                prodConfig.put("video", settings);
            default:
                break;
        }
        prodConfig.put("name", identification);
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

        UltraGridProducerApplication[] apps = node2producer.get(node);
        if (apps == null) {
            apps = new UltraGridProducerApplication[3];
        }

        // get content destriptor from producer
        String content = app.getProvidedContentDescriptor();
        if (content.contains("SOUND") && ((String) local.getProperty("role")).equals("student")) {
            return content;
        }
        UltraGridConsumerApplication con = null;
        String name = local.getName() + "-" + content;

        if (content.contains("SOUND") && isInterpreterOrTeacher((String) local.getProperty("role"))) {
            System.out.println(local.getName() + ":" + node.getName());
            if (node.getName().equals(local.getName())) {
                return content;
            }
            System.out.println("I should not be here");
            String audio = (String) local.getProperty("audioConsumer");
            if (audio == null) {
                throw new IllegalArgumentException("Specify audio in config");
            }
            con = createConsumer(content, audio, "dummy");
            apps[2] = app;
        }
        if (content.contains("PRESENTATION")) {
            con = createConsumer(content, null, (String) local.getProperty("videoConsumer"));
            apps[1] = app;
        }
        if (content.contains("VIDEO")) {
            con = createConsumer(content, null, (String) local.getProperty("videoConsumer"));
            apps[0] = app;
        }

        consumer2alert.put(con, false);
        producer2consumer.put(app, con);
        node2producer.put(node, apps);
        consumer2name.put(con, name);
        return name;
    }

    private UltraGridConsumerApplication createConsumer(String content, String audio, String video) throws IOException {
        ObjectNode cons = core.newApplicationTemplate("consumer");
        if (audio != null) {
            cons.put("audio", audio);
        }
        if (video != null) {
            cons.put("video", video);
        }
        cons.put("source", content);
        String name = local.getName() + "-" + content;
        cons.put("name", name);
        cons.put("arguments", "--window-title \"" + name + "\"");
        System.out.println(cons.get("source"));
        return (UltraGridConsumerApplication) core.startApplication(cons, "consumer");
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
                        String consumerName = createConsumer(producer, node);
                        if (!consumerName.contains("SOUND")) {
                            layoutManager.addNode(consumerName, (String) node.getProperty("role"));
                        }
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
        for (UltraGridProducerApplication app : node2producer.get(node)) {
            UltraGridConsumerApplication ugCon = producer2consumer.remove(app);
            consumer2alert.remove(ugCon);            
            String removed = consumer2name.remove(ugCon);

            if (removed != null) {               
                layoutManager.removeNode(removed);
                core.stopApplication(ugCon);                
            } else {
                System.out.println("You are trying to stop non-registered application");
            }
        }        
        node2producer.remove(node);

    }

    public static UltraGridConsumerApplication getKeyByValue(Map<UltraGridConsumerApplication, String> map, String value) {
        for (Entry<UltraGridConsumerApplication, String> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private boolean isInterpreterOrTeacher(String role) {
        return role.equals("teacher") || role.equals("interpreter");
    }
}
