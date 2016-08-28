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
 * @author pkajaba, xdaxner
 */
public class SessionManagerImpl implements SessionManager {

    /**
     * Represents teacher string
     */
    private static final String TEACHER = "TEACHER";

    /**
     * Maps consumers on producers Producers are keys
     */
    private Map<UltraGridProducerApplication, UltraGridConsumerApplication> producer2consumer = new HashMap<>();

    /**
     * Maps producers to nodes where are running Nodes are keys
     */
    private Map<NetworkNode, UltraGridProducerApplication[]> node2producer = new HashMap<>();

    /**
     *
     */
    private Map<UltraGridConsumerApplication, String> consumer2name = new HashMap<>();

    /**
     * Stored instance of node representing current computer
     */
    private NetworkNode local;

    /**
     * Instance of couniverse Core
     */
    private Core core;

    /**
     * Instance of LayoutManager to notify Layout about changes
     */
    private LayoutManager layoutManager;
    private TopologyAggregator topologyAggregator;

    /**
     * Listens alert and permission to talk messages
     */
    private MessageListener counsilListener;

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
     * 30 seconds timer, to forbid user from spamming alert messages
     */
    private Boolean canAlert;
    private Timer alertTimer;

    /**
     * Language resource bundle
     */
    private ResourceBundle languageBundle;

    /**
     * Color of window highlight while alerting
     */
    private String alertColor;

    /**
     * Color of window highlight while talking
     */
    private String talkColor;

    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(15);
    Map<UltraGridControllerHandle, ScheduledFuture<?>> futures = new HashMap<>();
     /**
             * Constructor to initialize LayoutManager
             *
             * @param layoutManager
             * @param talkingColor
             * @param riseHandColor
             * @param languageBundle
             */

    public SessionManagerImpl(LayoutManager layoutManager, Color talkingColor, Color riseHandColor, ResourceBundle languageBundle) {

        this.alertColor = getColorCode(riseHandColor);
        this.talkColor = getColorCode(talkingColor);
        this.talkingNode = null;
        this.canAlert = true;
        this.alertTimer = new Timer();
        this.languageBundle = languageBundle;
        this.layoutManager = layoutManager;

        this.layoutManager.addLayoutManagerListener(new LayoutManagerListener() {

            @Override
            public void alertActionPerformed() {

                if (!canAlert || (talkingNode != null && talkingNode.getName().equals(local.getName()))) {
                    return;
                }

                CoUniverseMessage alert = CoUniverseMessage.newInstance(ALERT, core.getLocalNode());
                Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, null, "Sending alert...");
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
     * Gets string implementation of Color
     *
     * @param color
     * @return
     */
    private String getColorCode(Color color) {
        String colorStringRepresentation = "#" + Integer.toHexString(color.getRed());
        colorStringRepresentation += Integer.toHexString(color.getGreen());
        colorStringRepresentation += Integer.toHexString(color.getBlue());
        return colorStringRepresentation;
    }

    /**
     * Gets producer from consumer (in producer2consumer) map
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
        NetworkNode.addPropertyParser("room", NodePropertyParser.STRING_PARSER);
        NetworkNode.addPropertyParser("audio", NodePropertyParser.STRING_PARSER);
        
        core = Main.startCoUniverse();

        topologyAggregator = TopologyAggregator.getInstance(core);
        local = core.getLocalNode();
        // create produrer for local content
        createProducent((String) local.getProperty("role"));

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
                    if ((talkingNode != null) && (talkingNode.getName().equals(node.getName()))) {
                        talkingNode = null;
                    }
                }
                stopConsumer(node);
            }
        });

        counsilListener = new MessageListener() {

            @Override
            public void onMessageArrived(CoUniverseMessage message) {
                Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, "Received new message {0}", message);

                NetworkNode talker = (NetworkNode) message.content[0];
                UltraGridConsumerApplication messagingConsumer = producer2consumer.get(node2producer.get(talker)[0]);
                String messagingTitle = messagingConsumer.getName();
                UltraGridControllerHandle handle = ((UltraGridControllerHandle) core.getApplicationControllerHandle(messagingConsumer));

                if (ALERT.equals(message.type)) {
                    if (handle != null) {
                        alertConsumer(handle, timers.get(messagingConsumer.name), 1000);
                    }

                } else if (TALK.equals((message.type))) {
                    if (messagingTitle != null) {

                        String currentTalkingName = null;
                        // STOP TALKING old node
                        if (talkingNode != null) {

                            UltraGridConsumerApplication oldConsumer = producer2consumer.get(node2producer.get(talkingNode)[0]);
                            currentTalkingName = oldConsumer.getName();
                            if (currentTalkingName != null) {
                                layoutManager.downScale(currentTalkingName);
                            }

                            UltraGridControllerHandle oldHandle = ((UltraGridControllerHandle) core.getApplicationControllerHandle(oldConsumer));
                            if (oldHandle != null) {
                                try {
                                    oldHandle.sendCommand("postprocess flush");
                                } catch (InterruptedException | TimeoutException ex) {
                                    Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }

                            talkingNode = null;
                        }

                        if (currentTalkingName == null || !currentTalkingName.equals(messagingTitle)) {

                            CounsilTimer currentTimer = timers.get(messagingConsumer.name);
                            if (currentTimer.task != null) {
                                currentTimer.task.cancel();
                            }
                            currentTimer.timer.purge();
                            // new node TALK!
                            talkingNode = talker;
                            layoutManager.upScale(messagingTitle);
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

            private void alertContinuously(UltraGridControllerHandle handle, CounsilTimer counsilTimer, int duration) {
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
                    "postprocess border:width=10:color=" + alertColor,
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
                        alertContinuously(handle, timer, 25000);
                        futures.get(handle).cancel(false);
                    }
                }
            }

            private void alertConsumer(UltraGridControllerHandle handle, CounsilTimer timer, int duration) {

                timer.timesFlashed = 0;
                futures.put(handle, executor.scheduleAtFixedRate(new Flasher(handle, timer), 0, duration, TimeUnit.MILLISECONDS));
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
        
        // If there is an option which forbids audio set it to null
        if ((Boolean)local.getProperty("audio") == false) {
            audio = null;
        }
        
        createProducer(TypeOfContent.VIDEO, video, audio, role);

        if (TEACHER.equals(role.toUpperCase())) {
            String pres = (String) local.getProperty("presentationProducer");
            if ((pres != null) && (!"".equals(pres))) {
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
        if (content.toUpperCase().contains("VIDEO")) {
            String audio = (String) local.getProperty("audioConsumer");
            //if (local.uuid.equals(node.uuid)) {
            if (local.getName().equals(node.getName()) || 
                    (Boolean) local.getProperty("audio") == false) {
                audio = null;
            }
            con = createConsumer(
                    content,
                    audio,
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
     */
    private void checkProducent(NetworkNode node) {
        if (node == null) {
            Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, null, "node is null");
            return;
        }

        // in case they have separate rooms just skip searching for apps
        if (!local.getProperty("room").equals(node.getProperty("room"))) {
            return;
        }

        for (MediaApplication app : node.getApplications()) {
            if (app instanceof UltraGridProducerApplication) {
                UltraGridProducerApplication producer = (UltraGridProducerApplication) app;
                try {
                    // If this is incoming applications which was not here before
                    if (producer2consumer.containsKey(producer) == false) {
                        String consumerName = createConsumer(producer, node);
                        if (consumerName.toUpperCase().contains("PRESENTATION")) {
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
                    Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, null, "You are trying to stop non-registered application");
                }
            }
        }
        node2producer.remove(node);

    }

    /**
     * Stops the instance of couniverse core
     */
    @Override
    public void stop() {
        core.stop();
    }
}
