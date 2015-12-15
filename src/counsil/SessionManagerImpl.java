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
import couniverse.monitoring.NodePresenceListener;
import couniverse.monitoring.TopologyAggregator;
import couniverse.monitoring.TopologyUpdate;
import couniverse.ultragrid.UltraGridConsumerApplication;
import couniverse.ultragrid.UltraGridProducerApplication;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
    Map<NetworkNode, UltraGridProducerApplication> node2producer = new HashMap<>();

    /**
     *
     */
    Map<UltraGridConsumerApplication, String> consumer2name = new HashMap<>();

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
     * Constructor to initialize LayoutManager
     *
     * @param layoutManager
     */
    public SessionManagerImpl(LayoutManager layoutManager) {
        // TODO check this and finish listener
        /*if (layoutManager == null) {
         throw new IllegalArgumentException("layoutManager is null");
         }*/
        this.layoutManager = layoutManager;
        //this.layoutManager.addLayoutManagerListener(new LayoutManagerListener() {
        //  @Override
        // public void alertActionPerformed() {
        //   throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        //}

            //@Override
        //public void windowChosenActionPerformed(String title) {
        //  throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        //}
            //@Override
        //public void muteActionPerformed() {
        //  throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        //}
        //});
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

        topologyAggregator = TopologyAggregator.newInstance(core);
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
        String name = "\"Consumer #" + windowCounter + "\"";
        System.out.println("I have created : " + content + "\n\n\n\n\n");
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
        }
        else{
            System.out.println("You are trying to remove, what does not exist");
            System.out.println("Future holds why your insane mind want to do this");
        }
    }
}
