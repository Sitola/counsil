/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import core.ControlPeer;
import core.Main;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import mediaAppFactory.MediaApplication;
import mediaAppFactory.MediaApplicationConsumer;
import networkRepresentation.EndpointNetworkNode;
import networkRepresentation.EndpointUserRole;
import networkRepresentation.NetworkSite;
import org.codehaus.jettison.json.JSONException;
import p2p.CoUniverseMessage;
import p2p.MessageType;
import p2p.NetworkConnector;
import p2p.NodeGroupIdentifier;
import wddman.UnsupportedOperatingSystemException;
import wddman.WDDMan;
import wddman.WDDManException;
import wddman.Window;

/**
 *
 * @author Peter
 */
public class UserController implements p2p.MessageListener {

    public static final boolean USING_DUMMY_UG = Main.getUniversePeer().getLocalNode().isDebugUsingDummyUG();

    private static ArrayList<DisplayableInfo> typesList = new ArrayList<>();
    static Layout layout;
    static boolean hasProducer = false;
    static MediaApplication producer;
    private ArrayList<CounsilNetworkNodeLight> nodes = new ArrayList<>();
    private boolean initialized;
    private static final NetworkConnector networkConnector = ControlPeer.getNetworkConnector();
    
    private final EndpointUserRole myEndpointUserRole = Main.getUniversePeer().getLocalNode().getMyEndpointUserRole();

    private static UserController userController;

    public static UserController getInstance() {
        if (userController == null) {
            userController = new UserController();
        }

        return userController;
    }

    private static void initializeProducer() {
        if (hasProducer == false && Main.getUniversePeer().getLocalNode().getIsDistributor() == false) {
            try {
                System.out.println("CoUnSil: Starting producer");
                
                if (USING_DUMMY_UG) {
                    producer = Main.getUniversePeer().getLocalNode().getApplicationTemplates().instantiateLocalTemplate("producent_dummy");
                } else if ((layout.getNodeRole().equals("teacher")) || (layout.getNodeRole().equals("interpreter"))) {
                    producer = Main.getUniversePeer().getLocalNode().getApplicationTemplates().instantiateLocalTemplate("producent_with_sound");
                } else {
                    producer = Main.getUniversePeer().getLocalNode().getApplicationTemplates().instantiateLocalTemplate("producent");
                }
                
                Main.getUniversePeer().applicationControl().runMediaApplication(producer);
                System.out.println("CoUnSil: Producer started");
                hasProducer = true;
                networkConnector.sendMessageToGroup(new CoUniverseMessage(MessageType.COUNSIL_PRODUCER_STARTED, new Serializable[]{}, networkConnector.getConnectorID(), null), NodeGroupIdentifier.ALL_NODES);

            } catch (IllegalArgumentException ex) {
                Logger.getLogger(UserController.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(UserController.class.getName()).log(Level.SEVERE, null, ex);
            } catch (JSONException ex) {
                Logger.getLogger(UserController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void initialize(String pathToConfig, EndpointUserRole nodeRole) {
        if (!Main.getUniversePeer().getLocalNode().getIsDistributor()) {
            CoUnSilConnectedControl controlPanel = new CoUnSilConnectedControl();
            controlPanel.setVisible(true);

            typesList.add(new DisplayableInfo(controlPanel.getTitle(), "controlPanel", controlPanel, null, null));
            System.out.println("Type list size is: " + typesList.size());

            //create layout
            layout = LayoutCreator.createLayoutFromConfigFile(pathToConfig, nodeRole.getMyRole());

            //arrange layout
            layout.countCoordinatesForLayout(Split.NO);

        }

        initializeProducer();

    }

    public static void userConnected(String identifier) {

        layout.countCoordinatesForLayout(Split.NO);
    }

    public static void userDisconnected(String identifier) {

        for (int i = 0; i < typesList.size(); i++) {
            if (typesList.get(i).getHandler().equals(identifier)) {

                typesList.remove(i);
            }

            //typesList.remove(nfo);
        }
        layout.countCoordinatesForLayout(Split.NO);
    }

    public static void refreshLayout() {
        layout.countCoordinatesForLayout(Split.NO);
    }

    public static ArrayList<DisplayableInfo> getDisplayableInfo() {
        return typesList;

    }

    @Override
    public void onMessageArrived(CoUniverseMessage message) {
        if (!initialized) {
            initialize("JSON.txt", myEndpointUserRole);
            initialized = true;
        }

        System.out.println("CoUnSil: message arrived: " + message.content.toString());

        final boolean isDistributor = Main.getUniversePeer().getLocalNode().getIsDistributor();
        if (isDistributor) {
            if (message.type == MessageType.COUNSIL_NETWORK_UPDATE) {
                ArrayList<CounsilNetworkNodeLight> incomingNodes = (ArrayList<CounsilNetworkNodeLight>) message.content[0];
                boolean changed = false;
                for (CounsilNetworkNodeLight incomingNode : incomingNodes) {
                    if ((!nodes.contains(incomingNode)) && (!incomingNode.getNodeSite().equals(Main.getUniversePeer().getLocalNode().getNodeSite())) && (!incomingNode.isIsDistributor())) {
                        startDistributor();
                        changed = true;
                    }
                }

                if (changed) {
                    nodes = incomingNodes;
                }
            }

        } else {

            if (message.type == MessageType.COUNSIL_NETWORK_UPDATE) {

                updateNodes(message);

            } else if (message.type == MessageType.COUNSIL_CAN_NOT_TALK) {
                for (DisplayableInfo di : typesList) {
                    if (((EndpointNetworkNode) message.content[0]).equals(di.getEndpointNetworkNode())) {
                        WindowController wc = (WindowController) di.getStreamWindow();
                        wc.stopTalking();
                    }
                }

            } else if (message.type == MessageType.COUNSIL_CAN_TALK) {
                for (DisplayableInfo di : typesList) {
                    if (((EndpointNetworkNode) message.content[0]).equals(di.getEndpointNetworkNode())) {
                        WindowController wc = (WindowController) di.getStreamWindow();
                        wc.talk();
                    }
                }

            } else if (message.type == MessageType.COUNSIL_DO_NOT_WANT_TO_TALK) {
                for (DisplayableInfo di : typesList) {
                    if (((EndpointNetworkNode) message.content[0]).equals(di.getEndpointNetworkNode())) {
                        WindowController wc = (WindowController) di.getStreamWindow();
                        wc.stopRaisingHand();
                    }
                }

            } else if (message.type == MessageType.COUNSIL_STOPPED_TALKING) {

            } else if (message.type == MessageType.COUNSIL_WANT_TO_TALK) {
                for (DisplayableInfo di : typesList) {
                    if (((EndpointNetworkNode) message.content[0]).equals(di.getEndpointNetworkNode())) {
                        WindowController wc = (WindowController) di.getStreamWindow();
                        wc.raiseHand();
                    }
                }
            } else if (message.type == MessageType.COUNSIL_PRODUCER_STARTED) {
                waitAndRefreshLayout();
            }

        }

    }

    private void waitAndRefreshLayout() {
        try {
            Thread.sleep(8000);
        } catch (InterruptedException ex) {
            Logger.getLogger(UserController.class.getName()).log(Level.SEVERE, null, ex);
        }

        UserController.getInstance().refreshLayout();
    }

    private void startDistributor() {
        System.out.println("CoUnSil: Starting distributor");
        try {
            if (USING_DUMMY_UG) {
                MediaApplication distributor = null;
                distributor = Main.getUniversePeer().getLocalNode().getApplicationTemplates().instantiateLocalTemplate("distributor_dummy");
                Main.getUniversePeer().applicationControl().runMediaApplication(distributor);
            } else {
                MediaApplication distributor = null;
                distributor = Main.getUniversePeer().getLocalNode().getApplicationTemplates().instantiateLocalTemplate("distributor");
                Main.getUniversePeer().applicationControl().runMediaApplication(distributor);
            }
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(UserController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(UserController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JSONException ex) {
            Logger.getLogger(UserController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void updateNodes(CoUniverseMessage message) throws IllegalArgumentException {
        ArrayList<CounsilNetworkNodeLight> incomingNodes = (ArrayList<CounsilNetworkNodeLight>) message.content[0];
        boolean refreshLayout = false;

        //create consumers for new nodes
        for (CounsilNetworkNodeLight incomingNode : incomingNodes) {
            if ((!nodes.contains(incomingNode)) && (!incomingNode.getNodeSite().equals(Main.getUniversePeer().getLocalNode().getNodeSite())) && (!incomingNode.isIsDistributor())) {

                NetworkSite nodeSite = incomingNode.getNodeSite();
                String windowTitle = nodeSite.getSiteName();

                MediaApplication consumer = startConsumer(nodeSite, windowTitle);

                String type = incomingNode.getMyEndpointUserRole().getMyRole();

                WDDMan manipulator = null;
                try {
                    manipulator = new WDDMan();
                } catch (UnsupportedOperatingSystemException ex) {
                    Logger.getLogger(UserController.class.getName()).log(Level.SEVERE, null, ex);
                }

                Window consumerWindow = waitUntilWindowIsCreated(manipulator, windowTitle);

                Displayable encapsulatedWindow = new WindowController(consumerWindow, incomingNode);

                DisplayableInfo consumerInfo = new DisplayableInfo(windowTitle, type, encapsulatedWindow, consumer, incomingNode);

                typesList.add(consumerInfo);

                refreshLayout = true;
            }
        }

        //kill consumers for missing nodes
        for (CounsilNetworkNodeLight node : nodes) {
            if (!incomingNodes.contains(node)) {
                // zabit konzumenta
                DisplayableInfo consumerInfo = null;

                for (DisplayableInfo info : typesList) {
                    if (node.getNodeSite().getSiteName().equals(info.getHandler())) {
                        consumerInfo = info;
                    }
                }

                //stop app, remove from list
                Main.getUniversePeer().applicationControl().stopMediaApplication(consumerInfo.getApp());
                for (int i = 0; i < typesList.size(); i++) {
                    if (consumerInfo.equals(typesList.get(i))) {
                        typesList.remove(i);
                    }
                }

                // odebrat z layoutu
                refreshLayout = true;
            }
        }

        if (refreshLayout) {
            //refresh Layout
            layout.countCoordinatesForLayout(Split.NO);
            //update node list
            nodes = incomingNodes;

        }

    }

    private MediaApplication startConsumer(NetworkSite nodeSite, String windowTitle) {
        MediaApplication consumer = null;
        try {
            if (USING_DUMMY_UG) {
                consumer = Main.getUniversePeer().getLocalNode().getApplicationTemplates().instantiateLocalTemplate("konzument_dummy");
                ((MediaApplicationConsumer) consumer).setSourceSite(nodeSite);
                consumer.setApplicationCmdOptions(windowTitle);

                Main.getUniversePeer().applicationControl().runMediaApplication(consumer);
            } else if ((layout.getNodeRole().equals("teacher")) || (layout.getNodeRole().equals("interpreter"))) {
                consumer = Main.getUniversePeer().getLocalNode().getApplicationTemplates().instantiateLocalTemplate("konzument_sound");

                ((MediaApplicationConsumer) consumer).setSourceSite(nodeSite);

                String oldCommandOptions = consumer.getApplicationCmdOptions();
                consumer.setApplicationCmdOptions(oldCommandOptions + " --window-title " + windowTitle);

                System.out.println("CoUnSil: Starting consumer with sound: " + oldCommandOptions + " " + windowTitle);
                Main.getUniversePeer().applicationControl().runMediaApplication(consumer);
            } else {
                consumer = Main.getUniversePeer().getLocalNode().getApplicationTemplates().instantiateLocalTemplate("konzument");

                ((MediaApplicationConsumer) consumer).setSourceSite(nodeSite);

                //set title
                String oldCommandOptions = consumer.getApplicationCmdOptions();

                //print
                System.out.println("CoUnSil: Starting consumer: " + oldCommandOptions + " " + windowTitle);

                //set title
                consumer.setApplicationCmdOptions(oldCommandOptions + " --window-title " + windowTitle);

                Main.getUniversePeer().applicationControl().runMediaApplication(consumer);
            }

        } catch (IllegalArgumentException ex) {
            Logger.getLogger(UserController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(UserController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JSONException ex) {
            Logger.getLogger(UserController.class.getName()).log(Level.SEVERE, null, ex);
        }

        return consumer;
    }

    private Window waitUntilWindowIsCreated(WDDMan manipulator, String windowTitle) {
        Window consumerWindow = null;
        while (consumerWindow == null) {
            try {
                consumerWindow = manipulator.getWindowByTitle(windowTitle);
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Logger.getLogger(UserController.class.getName()).log(Level.SEVERE, null, ex);
            } catch (WDDManException ex) {
                Logger.getLogger(UserController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return consumerWindow;
    }

}
