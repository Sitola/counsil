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
import couniverse.monitoring.RemoteNodesAggregator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
    Map<String, String> producer2consumer = new TreeMap<>();
    /**
     * Maps producers to nodes where are running Nodes are keys
     */
    Map<String, String> node2producer = new TreeMap<>();
    /**
     * Stored instance of node representing current computer
     */
    NetworkNode local;
    /**
     * Instance of Couniverse Core
     */
    Core core;
    /**
     * Instane of LayoutManager to notify Layout about changes
     */
    LayoutManager layoutManager;
    RemoteNodesAggregator nodesAggregator;

    /**
     * Constructor to initialize LayoutManager
     *
     * @param layoutManager
     */
    public SessionManagerImpl(LayoutManager layoutManager) {
        /*if (layoutManager == null) {
            throw new IllegalArgumentException("layoutManager is null");
        }*/
        this.layoutManager = layoutManager;
    }

    /**
     *
     * Starts producer on local node and listens to other nodes changes
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Override
    public void initCounsil() throws IOException, InterruptedException {
        // This parse additional attributes from configration file
        NetworkNode.addPropertyParser("role", NodePropertyParser.STRING_PARSER);
        NetworkNode.addPropertyParser("windowName", NodePropertyParser.STRING_PARSER);
        core = Main.startCoUniverse();
        
        nodesAggregator = RemoteNodesAggregator.newInstance(core.getConnector());
        local = core.getLocalNode();
        // create produrer for local content
        createProducent();
        final Object myLock = new Object();
        synchronized (myLock) {
            Set<NetworkNode> nodes = nodesAggregator.addNodePresenceListener(new RemoteNodesAggregator.NodePresenceListener() {

                @Override
                public void onNewNodeAppeared(NetworkNode node) {
                    onNodeChanged(node);
                }

                @Override
                public void onNodeChanged(NetworkNode node) {
                    System.out.println(node);
                    // check name
                    synchronized (myLock) {
                        // Check if there is new media application
                        seekForProducent(node);
                        // Check if there are still running registered applications
                        checkLeft(node);
                    }
                }

                @Override
                public void onNodeLeft(NetworkNode node) {
                    onNodeChanged(node);
                }
            });
            // zpracuju nodes
        }
    }

    /**
     * Starts producer from local node
     *
     * @throws IOException if there is problem during starting MediaApplication
     */
    private void createProducent() throws IOException {
        // TODO name content properly
        ObjectNode prod = core.newApplicationTemplate("producer");
        prod.put("content", "producer0");
        prod.put("name", "Producer 0");
        core.startApplication(prod, "producer");
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
    private String createConsumer(MediaApplication app, NetworkNode node) throws IOException, IllegalArgumentException {
        if (app == null) {
            throw new IllegalArgumentException("app is null");
        }
        if (node == null) {
            throw new IllegalArgumentException("node is null");
        }
        String content = (String) app.getProperty("content");
        String windowName = (String) node.getProperty("windowName");
        producer2consumer.put(app.getName(), windowName);
        node2producer.put(node.getName(), app.getName());
        ObjectNode cons = core.newApplicationTemplate("consumer");
        // content from producer is consumer's source
        cons.put("source", content);
        cons.put("name", windowName);
        cons.put("arguments", "--window-title " + windowName);
        core.startApplication(cons, "consumer");
        return windowName;
    }

    /**
     * Check if there is any new MediaApplication running on given node
     *
     * @param node where I check applications
     * @throws IllegalArgumentException if node is null
     */
    private void seekForProducent(NetworkNode node) throws IllegalArgumentException {
        if (node == null) {
            throw new IllegalArgumentException("node is null");
        }
        Set<MediaApplication> applications = node.getApplications();
        for (MediaApplication app : applications) {
            if (app instanceof MediaApplication) {
                try {
                    layoutManager.add(createConsumer(app, node), (String) node.getProperty("role"));
                } catch (IOException ex) {
                    Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Check whether is MediaApplication assigned to given node still running
     *
     * @param node where I check applications
     * @throws IllegalArgumentException if node is null
     */
    private void checkLeft(NetworkNode node) throws IllegalArgumentException {
        if (node == null) {
            throw new IllegalArgumentException("node is null");
        }
        Set<MediaApplication> applications = node.getApplications();
        String requredProducer = node2producer.get(node.getName());
        for (MediaApplication app : applications) {
            if (app.getName().equals(requredProducer)) {
                return;
            }
        }
        // notify layoutManager
        layoutManager.delete(requredProducer);
    }
}