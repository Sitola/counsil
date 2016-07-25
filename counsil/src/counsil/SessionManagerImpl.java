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
import couniverse.core.p2p.CoUniverseMessage;
import couniverse.core.p2p.GroupConnectorID;
import couniverse.core.p2p.MessageListener;
import couniverse.core.p2p.MessageType;
import couniverse.monitoring.NodePresenceListener;
import couniverse.monitoring.TopologyAggregator;
import couniverse.ultragrid.UltraGridConsumerApplication;
import couniverse.ultragrid.UltraGridControllerHandle;
import couniverse.ultragrid.UltraGridProducerApplication;
import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author palci
 */
public class SessionManagerImpl implements SessionManager {

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
     * Listens for ultragrid windows changes
     */
    couniverse.core.controllers.AppEventListener consumerListener;

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
     * Listens alert and permission to talk messages
     */
    MessageListener counsilListener;

    /**
     * Currently talking node
     */
    private volatile NetworkNode talkingNode;

    /**
     * Timers for alerting certain windows
     */
    private HashMap<String, CounsilTimer> timers = new HashMap<>();

    /**
     * Alert message is used for alerting other nodes
     */
    public static MessageType ALERT = MessageType.createCustomMessageType("AlertMessage", "NetworkNode");

    /**
     * Talk message is used for granting talk permission
     */
    public static MessageType TALK = MessageType.createCustomMessageType("TalkPermissionGrantedMessage", "NetworkNode");

    /**
     * 30 seconds timer, to forbid user spamming alert messages
     */
    private Boolean canAlert;
    Timer alertTimer;

    /**
     * actual resources bundlo for lenguages
     */
    ResourceBundle lenguageBundle;

    String alertColor;

    String talkColor;

    /**
     * Constructor to initialize LayoutManager
     *
     * @param layoutManager
     * @param talkingColor
     * @param riseHandColor
     * @param languageBundle
     */
    public SessionManagerImpl(LayoutManager layoutManager, Color talkingColor, Color riseHandColor, ResourceBundle languageBundle) {

        alertColor = "#";
        alertColor += Integer.toHexString(riseHandColor.getRed());
        alertColor += Integer.toHexString(riseHandColor.getGreen());
        alertColor += Integer.toHexString(riseHandColor.getBlue());

        talkColor = "#";
        talkColor += Integer.toHexString(talkingColor.getRed());
        talkColor += Integer.toHexString(talkingColor.getGreen());
        talkColor += Integer.toHexString(talkingColor.getBlue());

        talkingNode = null;
        canAlert = true;
        alertTimer = new Timer();

        this.lenguageBundle = languageBundle;

        if (layoutManager == null) {
            throw new IllegalArgumentException("layoutManager is null");
        }
        this.layoutManager = layoutManager;
        this.layoutManager.addLayoutManagerListener(new LayoutManagerListener() {

            @Override
            public void alertActionPerformed() {

                if ((talkingNode != null && local.getName().equals(talkingNode.getName())) || !canAlert) {
                    return;
                }

                CoUniverseMessage alert = CoUniverseMessage.newInstance(ALERT, core.getLocalNode());
                System.out.println("Sending alert...");
                core.getConnector().sendMessageToGroup(alert, GroupConnectorID.ALL_NODES);
                canAlert = false;

                alertTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        canAlert = true;
                    }
                }, 30000);

            }

            @Override
            public void windowChoosenActionPerformed(String windowName) {
                NetworkNode choosenNode = getNetworkNodeByProducer(getProducerByConsumer(getConsumerByTitle(windowName)));
                CoUniverseMessage talk = CoUniverseMessage.newInstance(TALK, choosenNode);
                Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, "Sending talk permission granted for node {0}...", windowName);
                core.getConnector().sendMessageToGroup(talk, GroupConnectorID.ALL_NODES);
            }
        });
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
                        onNodeChanged(node);
                    }
                }

                @Override
                public void onNewNodeAppeared(NetworkNode node) {
                    onNodeChanged(node);
                }

                @Override
                public void onNodeChanged(NetworkNode node) {
                    // Check if there is new media application
                    checkProducent(node);
                }

                @Override
                public void onNodeLeft(NetworkNode node) {
                    String nodeName = consumer2name.get(producer2consumer.get(node2producer.get(node)));
                    if (nodeName != null) {
                        if ((talkingNode != null) && (node.getName().equals(talkingNode.getName()))) {
                            talkingNode = null;
                        }
                    }
                    //stopConsumer(node);
                }
            });
        }

        counsilListener = new MessageListener() {

            @Override
            public void onMessageArrived(CoUniverseMessage message) {
                Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, "Received new message {0}", message);

                NetworkNode talker = (NetworkNode) message.content[0];
                UltraGridConsumerApplication consumer = producer2consumer.get(node2producer.get(talker)[0]);
                String title = consumer.getName();
                UltraGridControllerHandle handle = ((UltraGridControllerHandle) core.getApplicationControllerHandle(consumer));

                if (message.type.equals(ALERT)) {
                    if (handle != null) {
                        alertConsumer(handle, timers.get(consumer.name));
                    }

                } else if (message.type.equals(TALK)) {
                    if (title != null) {

                        String currentTalkingName = null;
                        // STOP TALKING old node
                        if (talkingNode != null) {

                            UltraGridConsumerApplication oldConsumer = producer2consumer.get(node2producer.get(talkingNode)[0]);
                            currentTalkingName = oldConsumer.getName();
                            if (currentTalkingName != null) {
                                layoutManager.downScale(currentTalkingName);
                            }
                            if (oldConsumer != null) {
                                UltraGridControllerHandle oldHandle = ((UltraGridControllerHandle) core.getApplicationControllerHandle(oldConsumer));
                                if (oldHandle != null) {
                                    try {
                                        oldHandle.sendCommand("postprocess flush");
                                    } catch (InterruptedException | TimeoutException ex) {
                                        Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }
                            }
                            talkingNode = null;
                        }

                        if (!title.equals(currentTalkingName)) {

                            CounsilTimer currentTimer = timers.get(consumer.name);
                            currentTimer.task.cancel();
                            currentTimer.timer.purge();
                            // new node TALK!
                            talkingNode = talker;
                            layoutManager.upScale(title);
                            if (handle != null) {
                                try {
                                    handle.sendCommand("postprocess border:width=10:color=" + talkColor);
                                } catch (InterruptedException | TimeoutException ex) {
                                    Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }

                    }
                }
            }

            private void alertConsumer(UltraGridControllerHandle handle, CounsilTimer timer) {

                // alertConsumerByFlashing(handle, timer, 2000);
                alertConsumerContinuously(handle, timer, 25000);

            }

            private void alertConsumerContinuously(UltraGridControllerHandle handle, CounsilTimer counsilTimer, int duration) {

                try {
                    handle.sendCommand("postprocess border:width=10:color=" + alertColor);
                } catch (InterruptedException | TimeoutException ex) {
                    Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                }

                counsilTimer.task = new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            handle.sendCommand("postprocess flush");
                            counsilTimer.timer.purge();
                        } catch (InterruptedException | TimeoutException ex) {
                            Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                };

                counsilTimer.timer.schedule(counsilTimer.task, duration);
            }

            class Flasher implements Runnable {

                UltraGridControllerHandle handle;
                CounsilTimer timer;

                String[] COMMAND = {
                    "postprocess border:width=10:color=#ff0000",
                    "postprocess flush"
                };

                public Flasher(UltraGridControllerHandle handle, CounsilTimer timer) {

                    this.handle = handle;
                    this.timer = timer;
                }

                @Override
                public void run() {

                    if (timer.timesFlashed != 10) {
                        try {
                            handle.sendCommand(COMMAND[timer.timesFlashed % 2]);
                        } catch (InterruptedException | TimeoutException ex) {
                            Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        timer.timesFlashed++;
                    } else {
                        future.cancel(false);
                        alertConsumerContinuously(handle, timer, 25000);
                    }
                }
            }

            ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
            ScheduledFuture<?> future;

            private void alertConsumerByFlashing(UltraGridControllerHandle handle, CounsilTimer timer, int duration) {

                timer.timesFlashed = 0;
                future = executor.scheduleAtFixedRate(new Flasher(handle, timer), 0, duration, TimeUnit.MILLISECONDS);
            }

        };

        // define message types
        core.getConnector().attachMessageListener(counsilListener, ALERT, TALK);
    }

    /**
     * Starts producer from local node
     *
     * @throws IOException if there is problem during starting Producer
     */
    private void createProducent(String role) throws IOException {
        String video = (String) local.getProperty("videoProducer");
        if (video == null) {
            throw new IllegalArgumentException("Specify video producer in config");
        }
        String audio = (String) local.getProperty("audioProducer");
        //if (audio == null) {
        //    throw new IllegalArgumentException("Specify audio in config");
        //}

        createProducer(TypeOfContent.VIDEO, video, audio, role);
        
        if (role.equals("teacher")) {
            String pres = (String) local.getProperty("presentationProducer");
            if ((pres != null) && (!pres.equals(""))) {
                createProducer(TypeOfContent.PRESENTATION, pres, null, role);
            }
        }

    }

    private void createProducer(
            TypeOfContent type, 
            String videoSettings,
            String audioSettings,
            String role
    ) throws IOException {
        ObjectNode prodConfig = core.newApplicationTemplate("producer");
        String identification = local.getName() + "-" + type.toString() + "-" + role;
        prodConfig.put("content", identification);
        switch (type) {
            case VIDEO:
                prodConfig.put("video", videoSettings);
                prodConfig.put("audio", audioSettings);
                break;
            case PRESENTATION:
                prodConfig.put("video", videoSettings);
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
        // validation
        if (app == null) {
            throw new IllegalArgumentException("app is null");
        }
        if (node == null) {
            throw new IllegalArgumentException("node is null");
        }

        UltraGridProducerApplication[] apps = node2producer.get(node);
        if (apps == null) {
            apps = new UltraGridProducerApplication[2];
        }

        // get content descriptor from producer
        String content = app.getProvidedContentDescriptor();

        UltraGridConsumerApplication con = null;
        String name = local.getName() + "-" + content;
        if (content.contains("VIDEO")) {
            con = createConsumer(
                    content, 
                    (String) local.getProperty("audioConsumer"), 
                    (String) local.getProperty("videoConsumer")
            );
            apps[0] = app;
        }

        if (content.contains("PRESENTATION")) {
            con = createConsumer(content, null, (String) local.getProperty("videoConsumer"));
            apps[1] = app;
        }

        timers.put(name, new CounsilTimer());
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
                    // If this is incoming applications which was not here before
                    if (producer2consumer.containsKey(producer) == false) {
                        String consumerName = createConsumer(producer, node);
                        if (consumerName.contains("PRESENTATION")) {
                            layoutManager.addNode(consumerName, "presentation");
                        } else {
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
        if (node2producer.get(node) == null) {
            return;
        }
        for (UltraGridProducerApplication app : node2producer.get(node)) {
            UltraGridConsumerApplication ugCon = producer2consumer.remove(app);
            if (ugCon != null) {
                timers.remove(ugCon.name);

                String removed = consumer2name.remove(ugCon);

                if (removed != null) {
                    layoutManager.removeNode(removed);
                    core.stopApplication(ugCon);
                } else {
                    System.out.println("You are trying to stop non-registered application");
                }
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

    @Override
    public void stopCounsil() {
        core.stop();
    }

    @Override
    public String getStatus() {

        String status = new String();

        int studentCount = 0;
        Boolean teacher = false,
                interpreter = false,
                presentation = false,
                interpreterAudio = false,
                teacherAudio = false;

        for (String name : consumer2name.values()) {

            String nameToUpper = name.toUpperCase();

            if (nameToUpper.contains("STUDENT")) {
                studentCount++;
            } else if (nameToUpper.contains("INTERPRETER")) {
                if (nameToUpper.contains("AUDIO")) {
                    interpreterAudio = true;
                } else {
                    interpreter = true;
                }
            } else if (nameToUpper.contains("TEACHER")) {
                if (nameToUpper.contains("PRESENTATION")) {
                    presentation = true;
                } else if (nameToUpper.contains("AUDIO")) {
                    teacherAudio = true;
                } else if (nameToUpper.contains("AUDIO")) {
                    teacherAudio = true;
                } else {
                    teacher = true;
                }
            }
        }

        status += (lenguageBundle.getString("INTERPRETER") + " video: ");
        if (interpreter) {
            status += (lenguageBundle.getString("ONLINE") + "\n");
        } else {
            status += (lenguageBundle.getString("OFFLINE") + "\n");
        }

        status += (lenguageBundle.getString("INTERPRETER") + " audio: ");
        if (interpreterAudio) {
            status += (lenguageBundle.getString("ONLINE") + "\n");
        } else {
            status += (lenguageBundle.getString("OFFLINE") + "\n");
        }

        status += (lenguageBundle.getString("TEACHER") + " video: ");
        if (teacher) {
            status += (lenguageBundle.getString("ONLINE") + "\n");
        } else {
            status += (lenguageBundle.getString("OFFLINE") + "\n");
        }

        status += (lenguageBundle.getString("TEACHER") + " audio: ");
        if (teacher) {
            status += (lenguageBundle.getString("ONLINE") + "\n");
        } else {
            status += (lenguageBundle.getString("OFFLINE") + "\n");
        }

        status += (lenguageBundle.getString("PRESENTATION") + ": ");
        if (presentation) {
            status += (lenguageBundle.getString("ONLINE") + "\n");
        } else {
            status += (lenguageBundle.getString("OFFLINE") + "\n");
        }

        status += (lenguageBundle.getString("STUDENTS") + ": ");
        status += studentCount;

        return status;
    }
}
