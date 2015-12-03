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
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
     * Instance of LayoutManager to notify Layout about changes
     */
    LayoutManager layoutManager;
    TopologyAggregator topologyAggregator;
    //RemoteNodesAggregator nodesAggregator;

    /**
     * Node identificator
     */
    String ident = "";

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
        this.layoutManager.addLayoutManagerListener(new LayoutManagerListener() {

            @Override
            public void alertActionPerformed() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void windowChosenActionPerformed(String title) {               
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void muteActionPerformed() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
        getMac();
    }

    /**
     * This method find mac adress and fill ident If it fails, ident is empty
     * string - TODO anything random
     */
    private void getMac() {
        try {
            NetworkInterface network = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            byte[] mac = network.getHardwareAddress();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X", mac[i]));
            }
            ident = sb.toString();
        } catch (UnknownHostException | SocketException ex) {
            Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            macToMD5();
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(SessionManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * this convert mac adress to md5 hash of it
     * @throws NoSuchAlgorithmException if MD5 algorithm is not find
     */
    private void macToMD5() throws NoSuchAlgorithmException {
        MessageDigest m = MessageDigest.getInstance("MD5");
        m.reset();
        m.update(ident.getBytes());
        byte[] digest = m.digest();
        BigInteger bigInt = new BigInteger(1, digest);
        ident = bigInt.toString(16);
        // Now we need to zero pad it if you actually want the full 32 chars.
        while (ident.length() < 32) {
            ident = ident + "0";
        }
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
        NetworkNode.addPropertyParser("agc", NodePropertyParser.STRING_PARSER);
        NetworkNode.addPropertyParser("role", NodePropertyParser.STRING_PARSER);
        NetworkNode.addPropertyParser("windowName", NodePropertyParser.STRING_PARSER);
        core = Main.startCoUniverse();
        
       

        //nodesAggregator = RemoteNodesAggregator.newInstance(core.getConnector());
        topologyAggregator = TopologyAggregator.newInstance(core);
        local = core.getLocalNode();
        //local.getUuid();
        // create produrer for local content
        createProducent((String) local.getProperty("role"));
        final Object myLock = new Object();
        synchronized (myLock) {
            //Set<NetworkNode> nodes = nodesAggregator.addNodePresenceListener(new RemoteNodesAggregator.NodePresenceListener() {
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
                        if (node == local) {
                            return;
                        }
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
        // I want to start distributor
        createDistrubutor();
        // TODO name constartApplicationtent properly
        // think how to name content properly
        // todo start different producer with different role
        // no sound, better quality, framerate etc
        ObjectNode prodConfig = core.newApplicationTemplate("producer");
        String identification = "Producer #" + ident;
        prodConfig.put("content", identification);
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
        // get content destriptor from producer
        // TODO I want to split content and extract number to name window by Consumer
        // + given number
        String content = app.getProvidedContentDescriptor();
        // podla mna treba to premysliet - content = windowName
        //String windowName = (String) node.getProperty("windowName");
        producer2consumer.put(app.getName(), content);
        node2producer.put(node.getName(), app.getName());
        ObjectNode cons = core.newApplicationTemplate("consumer");
        // content from producer is consumer's source
        cons.put("source", content);
        cons.put("name", content);
        cons.put("arguments", "--window-title " + content);
        core.startApplication(cons, "consumer");
        return content;
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
            if (app instanceof UltraGridProducerApplication) {
                UltraGridProducerApplication producer = (UltraGridProducerApplication) app;
                try {
                    if (!producer.getProvidedContentDescriptor().equals(producer2consumer.get(producer.getName()))) {
                        createConsumer(producer, node);
                        //layoutManager.addNode(createConsumer(producer, node), (String) node.getProperty("role"));
                    }
                    //
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
        UltraGridConsumerApplication con = null;
        Set<MediaApplication> applications = node.getApplications();
        String requredProducer = node2producer.get(node.getName());
        if (requredProducer == null) {
            return;
        }
        // if all apps are running it's ok
        for (MediaApplication app : applications) {
            if (app.getName().equals(requredProducer)) {
                return;
            }
        }
        String consumer = producer2consumer.get(requredProducer);
        for (MediaApplication app : core.getLocalNode().getApplications()) {
            if (app instanceof UltraGridConsumerApplication) {
                con = (UltraGridConsumerApplication) app;
                if (consumer.equals(con.getRequestedContentDescriptor())) {
                    node2producer.remove(node.getName());
                    producer2consumer.remove(requredProducer);
                    // notify layoutManager
                    //layoutManager.removeNode(requredProducer);
                    core.stopApplication(app);
                }
            }
        }
    }
}

