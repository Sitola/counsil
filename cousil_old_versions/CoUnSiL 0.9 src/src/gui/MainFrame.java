/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gui;

import core.Main;
import core.P2pConfigWorker;
import core.UiStub;
import gui.mapVisualization.MapVisualizerPanel;
import gui.mapVisualization.UniverseWaypoint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import javax.swing.AbstractListModel;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import mediaAppFactory.MediaApplication;
import networkRepresentation.EndpointNetworkNode;
import networkRepresentation.EndpointNodeInterface;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import utils.GeoLocation;

/**
 *
 * @author luksoft
 */
public class MainFrame extends javax.swing.JFrame {
    static Logger logger = Logger.getLogger(MainFrame.class);
    
    
    private class ArrayStringModel extends AbstractListModel<String> {
        private JSONArray options;
        
        public ArrayStringModel() {
            options = new JSONArray();
        }
        public ArrayStringModel(JSONArray opts) {
            options = opts;
        }

        public JSONArray getOptions() {
            return options;
        }
        
        @Override
        public void fireContentsChanged(Object source, int index0, int index1) {
            super.fireContentsChanged(source, index0, index1); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void fireIntervalAdded(Object source, int index0, int index1) {
            super.fireIntervalAdded(source, index0, index1); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void fireIntervalRemoved(Object source, int index0, int index1) {
            super.fireIntervalRemoved(source, index0, index1); //To change body of generated methods, choose Tools | Templates.
        }
        
        public void setOptions(JSONArray options) {
            this.options = options;
            fireContentsChanged(this, 0, this.options.length());
        }

        @Override
        public int getSize() {
            return options.length();
        }

        @Override
        public String getElementAt(int i) {
            try {
                return options.getString(i);
            } catch (JSONException ex) {
                Logger.getLogger(MainFrame.class.getName()).log(Level.ERROR, "BUG: JSON array element is not string, yet it's supposed to be!", ex);
                return null;
            }
        }
    }
    
    private class ArrayInterfaceModel extends AbstractListModel<String> {
        private JSONArray interfaces;
        
        public ArrayInterfaceModel() {
            interfaces = new JSONArray();
        }
        public ArrayInterfaceModel(JSONArray ifaces) {
            setInterfaces(ifaces);
        }

        public JSONArray getInterfaces() {
            return interfaces;
        }
        
        public void fireReplaced() {
            super.fireContentsChanged(this, 0, getSize());
        }

        public void setInterfaces(JSONArray interfaces) {
            this.interfaces = interfaces;
            fireReplaced();
        }

        @Override
        public int getSize() {
            return interfaces.length();
        }

        @Override
        public String getElementAt(int i) {
            JSONObject iface = null;
            try {
                iface = interfaces.getJSONObject(i);
            } catch (JSONException ex) {
                Logger.getLogger(this.getClass()).error("BUG: interface entry " + i + " is not an JSONObject: " + interfaces.toString() + "; original error: "+ex.toString(), ex);
            }
            
            try {
                return iface.getString(EndpointNodeInterface.ConfigKeyIfName);
            } catch (JSONException ex) {
                Logger.getLogger(this.getClass()).error("Mallformed interface (missing string key "+EndpointNodeInterface.ConfigKeyIfName+" (" + iface.toString() + "), original error: " + ex.toString(), ex);
            }
            return "Mallformed interface entry";
        }
    }
    
    private class loggedInput extends InputStream {
        private final JTextArea log;
        private final InputStream input;

        public loggedInput(InputStream in, JTextArea log) {
            super();
            this.log = log;
            this.input = in;
        }
        private static final int LogChars = 1000;

        @Override
        public int read() throws IOException {
            
            if (input.available() > 0) {
                int c = input.read();
                if (c >= 0) {
                    log.append(Character.toString((char)c));
                    Document doc = log.getDocument();
                    if (doc.getLength() > LogChars){
                        int i = doc.getLength() - LogChars;
                        try {
                            while (!doc.getText(i, 1).equals("\n")) ++i;
                            doc.remove(0, i);
                        } catch (BadLocationException ex) {
                            log.setText("Log reset:\n\n");
                        }
                    }
                }
                return c;
            } else {
                return -1;
            }
        }

        @Override
        public int available() throws IOException {
            super.available();
            int retval = input.available();
            return retval;
        }

        @Override
        public void close() throws IOException {
            super.close();
            input.close();
        }

        @Override
        public boolean markSupported() {
            markSupported();
            return input.markSupported();
        }
        
        @Override
        public synchronized void mark(int i) {
            mark(i);
            input.mark(i);
        }

        @Override
        public synchronized void reset() throws IOException {
            reset();
            input.reset();
        }

        @Override
        public long skip(long l) throws IOException {
            skip(l);
            return input.skip(l);
        }
    }
    
    private class loggedOutput extends OutputStream {
        private final JTextArea log;
        private final OutputStream output;

        public loggedOutput(OutputStream out, JTextArea log) {
            super();
            this.log = log;
            this.output = out;
        }

        @Override
        public void write(int i) throws IOException {
            log.append(Character.toString((char)i));
            output.write(i);
        }
        
        @Override
        public void flush() throws IOException {
            super.flush();
            output.flush();
        }

        @Override
        public void close() throws IOException {
            super.close();
            output.close();
        }
    }
    
    private interface GuiTask {
        void execute(UiStub.UiChannelEndpoint uiEndpoint) throws IOException, JSONException;
    }
    private class GuiTasker extends Thread {
        private BlockingQueue<GuiTask> taskQueue = new LinkedBlockingDeque<GuiTask>();
        private UiStub.UiChannelEndpoint uiEndpoint;
        
        private class Poision implements GuiTask {
            @Override
            public void execute(UiStub.UiChannelEndpoint uiEndpoint) throws IOException, JSONException {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        }
        
        public GuiTasker(UiStub.UiChannelEndpoint uiEndpoint) {
            this.uiEndpoint = uiEndpoint;
        }
        
        public void addTask(GuiTask task) {
            taskQueue.add(task);
        }
        public void addUniqueTask(GuiTask task) {
            if (!taskQueue.contains(task)) {
                taskQueue.add(task);
            }
        }
        
        public void poisonWorker(){
            taskQueue.add(new Poision());
        }
        
        @Override
        public void run() {
            while (true) {
                GuiTask nextTask = null;
                try {
                    nextTask = taskQueue.take();
                } catch (InterruptedException ex) {
                    continue;
                }
                
                if (nextTask instanceof Poision) {
                    return;
                }
                
                try {
                    nextTask.execute(uiEndpoint);
                } catch (IOException | JSONException ex) {
                    Logger.getLogger(this.getClass()).error("Failed running task of " + nextTask.getClass().getCanonicalName() + " reason: "+ex.toString());
                } catch (NullPointerException ex) {
                    Logger.getLogger(this.getClass()).fatal("\n\nBUG - gui encountered NullPointerExcepiton: " + ex.toString());
                }
            }
        }
    }
    private class ConfigP2PUpdater implements GuiTask {
        @Override
        public void execute(UiStub.UiChannelEndpoint uiEndpoint) throws IOException, JSONException {
            uiEndpoint.swallowRest();

            uiEndpoint.getOutput().println("show config .p2p");

            if (!uiEndpoint.awaitResponse()) {
                throw new IOException("Timeout for reply has exceeded!");
            }

            // parse input line - holding shape some tokens = jsonobject denoted with two consecutive new line markers
            // state
            JSONObject response = uiEndpoint.processInput();

            if (!confirmState(response, UiStub.StatusOk)) {
                if (response.has(UiStub.KeyMessage)) {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, response.getString(UiStub.KeyStatus));
                } else {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, "Server-side user interface generated an uncommented error.");
                }
                return;
            }

            // expected path "."
            if (!response.getString(UiStub.KeyMessage).equals(".p2p")) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, "Wrong config path in response!");
            }

            JSONObject configuration = null;

            try {
                configuration = response.getJSONObject(UiStub.KeyResult);
            } catch (JSONException ex) {
                Logger.getLogger(MainFrame.class.getName()).log(Level.ERROR, "Received response does not contain JSON-represented result!", ex);
            }

            setConfigurationForP2p(configuration);        
        }
        public void setConfigurationForP2p(JSONObject configuration) {
            try {
                enableAgc.setSelected(configuration.getBoolean(P2pConfigWorker.ConfigKeyStartAGC));
                enableRendezvous.setSelected(configuration.getBoolean(P2pConfigWorker.ConfigKeyStartRendezvous));
                rendezvousSeedUrisModel.setOptions(configuration.getJSONArray(P2pConfigWorker.ConfigKeyRendezvousSeedingURIs));
                rendezvousUrisModel.setOptions(configuration.getJSONArray(P2pConfigWorker.ConfigKeyRendezvousURIs));
    
                statusbarInfo("P2P configuration loaded");
            } catch (JSONException ex) {
                String detail = "";
                try {
                    detail = " (" + configuration.toString(2)+ ")";
                } catch (JSONException exs) {
                    ; // silently ignore
                }

                Logger.getLogger(this.getClass().getName()).log(Level.FATAL, "Failed to read (probably missing) required attribute of configuration object" + detail + "!", ex);
            }
        }        
    }
    private class ConfigNodeUpdater implements GuiTask {
        @Override
        public void execute(UiStub.UiChannelEndpoint uiEndpoint) throws IOException, JSONException {
            uiEndpoint.swallowRest();
            uiEndpoint.getOutput().println("show config .localNode");

            if (!uiEndpoint.awaitResponse()) {
                throw new IOException("Timeout for reply has exceeded!");
            }

            // parse input line - holding shape some tokens = jsonobject denoted with two consecutive new line markers
            // state
            JSONObject response = uiEndpoint.processInput();

            if (!confirmState(response, UiStub.StatusOk)) {
                if (response.has(UiStub.KeyMessage)) {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, response.getString(UiStub.KeyStatus));
                } else {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, "Server-side user interface generated an uncommented error.");
                }
                return;
            }

            // expected path "."
            if (!response.getString(UiStub.KeyMessage).equals(".localNode")) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, "Wrong config path in response!");
            }

            JSONObject configuration = null;

            try {
                configuration = response.getJSONObject(UiStub.KeyResult);
            } catch (JSONException ex) {
                Logger.getLogger(MainFrame.class.getName()).log(Level.ERROR, "Received response does not contain JSON-represented result!", ex);
            }
            
            if (mapPanel == null) {
                setupVisualization(configuration);
            }

            setConfigurationForNode(configuration);
            setConfigurationForNetwork(configuration);
        }
        public void setConfigurationForNode(JSONObject configuration) {

            try {
                nodeName.setText(configuration.getString(EndpointNetworkNode.ConfigKeyNodeName));
                siteName.setText(configuration.getString(EndpointNetworkNode.ConfigKeySiteName));

                nodeUUID.setText(configuration.getString(EndpointNetworkNode.ConfigKeyUUID));
                thisNodeUuid = nodeUUID.getText();

                JSONObject loc = configuration.getJSONObject(EndpointNetworkNode.ConfigKeyLocation);
                geoLocation.setText(Double.toString(loc.getDouble(GeoLocation.KeyLatitude)) + ", " + Double.toString(loc.getDouble(GeoLocation.KeyLongitude)));

                statusbarInfo("Node configuration loaded");
            } catch (JSONException ex) {
                String detail = "";
                try {
                    detail = " (" + configuration.toString(2)+ ")";
                } catch (JSONException exs) {
                    ; // silently ignore
                }

                Logger.getLogger(this.getClass().getName()).log(Level.FATAL, "Failed to read (probably missing) required attribute of configuration object" + detail + "!", ex);
            }
        }

    }
    private class ShutdownTask implements GuiTask {
        @Override
        public void execute(UiStub.UiChannelEndpoint uiEndpoint)throws JSONException, IOException {
            uiEndpoint.swallowRest();
            uiEndpoint.getOutput().println("shutdown");
            if (uiEndpoint.awaitResponse()) {
                status(uiEndpoint.processInput());
            } else {
                Logger.getLogger("Failed to receive reply from connected CoUniverse");
            }
            MainFrame.this.uiTasks.poisonWorker();
            MainFrame.this.dispose();
        }
    }
    private class MapUpdaterTask implements GuiTask {
        public static final int MapUpdateInterval = 1000; // miliseconds
        
        @Override
        public void execute(UiStub.UiChannelEndpoint uiEndpoint) throws IOException, JSONException {
            loadPlan(uiEndpoint);
            loadNodes(uiEndpoint);
            if (MainFrame.this.mapPanel != null) {
                MainFrame.this.mapPanel.getMapVisualizer().update(currentNetworkNodes, currentPlan);
            }
        }
        
        private void loadPlan(UiStub.UiChannelEndpoint uiEndpoint) throws IOException, JSONException {
            uiEndpoint.swallowRest();
            uiEndpoint.getOutput().println("show plan");

            if (!uiEndpoint.awaitResponse()) {
                throw new IOException("Timeout for reply has exceeded!");
            }

            // parse input line - holding shape some tokens = jsonobject denoted with two consecutive new line markers
            // state
            JSONObject response = uiEndpoint.processInput();

            if (!confirmState(response, UiStub.StatusOk)) {
                if (response.has(UiStub.KeyMessage)) {
                    logger.error("response.getString(UiStub.KeyStatus) = " + response.getString(UiStub.KeyStatus));
                } else {
                    logger.error("Server-side user interface generated an uncommented error.");
                }
                return;
            }

            JSONObject plan = null;

            try {
                plan = response.getJSONObject(UiStub.KeyResult);
            } catch (JSONException ex) {
                logger.error("Received response does not contain JSON-represented result!", ex);
            }

            currentPlan = plan;
        }
        private void loadNodes(UiStub.UiChannelEndpoint uiEndpoint) throws IOException, JSONException {
            uiEndpoint.swallowRest();
            uiEndpoint.getOutput().println("show universe");

            if (!uiEndpoint.awaitResponse()) {
                throw new IOException("Timeout for reply has exceeded!");
            }

            // parse input line - holding shape some tokens = jsonobject denoted with two consecutive new line markers
            // state
            JSONObject response = uiEndpoint.processInput();

            if (!confirmState(response, UiStub.StatusOk)) {
                if (response.has(UiStub.KeyMessage)) {
                    logger.error("response.getString(UiStub.KeyStatus) = " + response.getString(UiStub.KeyStatus));
                } else {
                    logger.error("Server-side user interface generated an uncommented error.");
                }
                return;
            }

            try {
                JSONObject universe = response.getJSONObject(UiStub.KeyResult);
                currentNetworkNodes = universe.getJSONObject("nodes");
                System.out.println("Nodes set to: " + currentNetworkNodes);
            } catch (JSONException ex) {
                logger.error("Received response does not contain JSON-represented result!", ex);
            }
        }
    }
    
    public void setConfigurationForNetwork(JSONObject configuration) {
        try {
            interfacesModel.setInterfaces(configuration.getJSONArray(EndpointNetworkNode.ConfigKeyNodeInterfaces));
            statusbarInfo("Network configuration loaded");
        } catch (JSONException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.FATAL, "Failed to read (probably missing) required attribute of configuration object (" + configuration.toString()+ ")!", ex);
        }
    }
    
    private void initModels(){
        rendezvousSeedUrisModel = new ArrayStringModel();
        rendezvousUrisModel = new ArrayStringModel();
        interfacesModel = new ArrayInterfaceModel();
    }
        
    private void setupVisualization(JSONObject initialData) {
        mapPanel = new MapVisualizerPanel(initialData);
        tabs.add(mapPanel, 0);
        
        // todo local visualization
        //Main.getUniversePeer().setMapVisualizer(mapPanel.getMapVisualizer());
                
        tabs.insertTab("Universe", null, mapPanel, null, 0);
        tabs.setSelectedIndex(0);
        tabs.validate();
                
        mapPanel.getBottomPanel().addApplicationButton.addActionListener(new ActionListener() {
            MainFrame base;            
            
            public ActionListener setBase(MainFrame base) {
                this.base = base;
                return this;
            }
            @Override
            @Deprecated
            public void actionPerformed(ActionEvent event) {
                
                ApplicationDialog applicationDialog = new ApplicationDialog(base);
                
                applicationDialog.setVisible(true);
                
                if (applicationDialog.getHasBeenApplicationSet() && applicationDialog.getMediaApplication() != null) {
                    MediaApplication app = applicationDialog.getMediaApplication();
                    /** @todo REWRITE TO BE COMPATIBLE WITH NEW UI **/
/*
                    Main.getUniversePeer().getLocalNode().getApplicationManager().addApplication(app);
                    Main.getUniversePeer().runApplication(app);
*/
                }
                applicationDialog.dispose();
                
            }
            
        }.setBase(this));

        mapPanel.getBottomPanel().removeApplicationButton.addActionListener(new ActionListener() {
            MainFrame base;            
            
            public ActionListener setBase(MainFrame base) {
                this.base = base;
                return this;
            }
            @Override
            @Deprecated
            public void actionPerformed(ActionEvent event) {
                RemoveApplicationDialog removeDialog = new RemoveApplicationDialog(
                                      base, Main.getUniversePeer().getLocalNode().getNodeApplications()
                                );
                removeDialog.setVisible(true);
                
                if (removeDialog.getHasBeenInterfaceSet() && removeDialog.getMediaApplication() != null) {
                    MediaApplication app = removeDialog.getMediaApplication();
                    Main.getUniversePeer().getLocalNode().getApplicationManager().removeApplication(app);
                    Main.getUniversePeer().applicationControl().stopMediaApplication(app);
                    //new BaseFrame.SaveConfigTask().execute();
                }
            
                removeDialog.dispose();

            }
        }.setBase(this));

        mapPanel.getBottomPanel().selectSourceButton.addActionListener(new ActionListener() {
            MapVisualizerPanel mapPanel;

            public ActionListener setMapPanel(MapVisualizerPanel mapPanel) {
                this.mapPanel = mapPanel;
                return this;
            }
            @Override
            @Deprecated
            public void actionPerformed(ActionEvent event) {
                UniverseWaypoint waypoint = mapPanel.getMapVisualizer().getLastClickedWaypoint();
                if (waypoint == null) return;
                
                String lastNodeUuid = waypoint.getNodeUuid();
                if (!lastNodeUuid.equals(thisNodeUuid)) return;
/*                
                JSONObject manipulated = currentNetworkNodes.getJSONObject(lastNodeUuid);
/*                
                J
                

                EndpointNetworkNode node = (EndpointNetworkNode) waypoint.getNetworkNode();
                Collection<MediaApplication> apps = Main.getUniversePeer().getLocalNode().getNodeApplications();
                Iterator<MediaApplication> it = apps.iterator();
                while (it.hasNext()) {
                    MediaApplication app = it.next();

                    if (app instanceof MediaApplicationConsumer) {
                        MediaApplicationConsumer appConsumer = (MediaApplicationConsumer) app;
                        Main.getUniversePeer().applicationControl().stopMediaApplication(appConsumer);
                        Main.getUniversePeer().getLocalNode().getApplicationManager().removeApplication(appConsumer);
                        appConsumer.setSourceSite(node.getNodeSite());

                        Main.getUniversePeer().getLocalNode().getApplicationManager().addApplication(appConsumer);
                        Main.getUniversePeer().applicationControl().runMediaApplication(appConsumer);
                    }
                }
*/
            }
        }.setMapPanel(mapPanel));
                
        }
    
    private boolean confirmState(JSONObject response, String state) throws JSONException {
        return (response.getString(UiStub.KeyStatus).equals(state));
    }

    
    /**
     * Creates new form MainFrame
     */
    public MainFrame(InputStream fromStub, OutputStream toStub) {
        initModels();
        initComponents();
//        this.uiEndpoint = new UiStub.UiChannelEndpoint(new BufferedReader(new loggedReader(fromStub, log)), new PrintStream(new loggedOutput(toStub, log)));
        this.uiTasks = new GuiTasker(
            new UiStub.UiChannelEndpoint(
                new BufferedReader(new InputStreamReader(new loggedInput(fromStub, log))), 
                new PrintStream(new loggedOutput(toStub, log))));
//        setupVisualization();
    }
    
    void status(JSONObject reply) {
        try {
            if (confirmState(reply, UiStub.StatusOk)) {
                statusbarInfo(reply.optString(UiStub.KeyMessage, "Done"));
            } else {
                statusbarInfo(reply.optString(UiStub.KeyMessage, "Error executing command"));
            }
        } catch (JSONException error) {
            statusbarError("Error parsing server response");
            Logger.getLogger(this.getClass()).error("BUG: Received reply does not contain requred filed " + UiStub.KeyStatus + ": " + reply.toString());
        }
    }
    void statusbarError(String message) {
        statusbar.setIcon(UIManager.getIcon("OptionPane.errorIcon"));
        statusbar.setText(message);
    }
    void statusbarInfo(String message) {
        statusbar.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
        statusbar.setText(message);
    }
    
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            uiTasks.start();
            uiTasks.addTask(new ConfigP2PUpdater());
            uiTasks.addTask(new ConfigNodeUpdater());
            
            // start periodic updater
            new Thread() {
                @Override
                public void run() {
                    boolean previousState = false;
                    
                    while ((previousState && MainFrame.this.isVisible()) || !previousState) {
                        previousState = MainFrame.this.isVisible();
                        uiTasks.addUniqueTask(new MapUpdaterTask());
                        try {
                            Thread.sleep(MapUpdaterTask.MapUpdateInterval);
                        } catch (InterruptedException ex) {
                            continue;
                        }
                    }
                }
            }.start();
        } else {
            uiTasks.poisonWorker();
        }
//            statusbarInfo("Connected");
        super.setVisible(visible);
    }

        /**
         * This method is called from within the constructor to initialize the
         * form. WARNING: Do NOT modify this code. The content of this method is
         * always regenerated by the Form Editor.
         */
        @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        tabs = new javax.swing.JTabbedPane();
        javax.swing.JScrollPane tabP2p = new javax.swing.JScrollPane();
        javax.swing.JPanel panelP2p = new javax.swing.JPanel();
        enableAgc = new javax.swing.JCheckBox();
        enableRendezvous = new javax.swing.JCheckBox();
        javax.swing.JPanel panSeeds = new javax.swing.JPanel();
        javax.swing.JPanel pan2 = new javax.swing.JPanel();
        javax.swing.JLabel lblSeeding = new javax.swing.JLabel();
        rendezvousSeedURIEditor = new javax.swing.JTextField();
        addSeed = new javax.swing.JButton();
        delSeed = new javax.swing.JButton();
        okSeed = new javax.swing.JButton();
        javax.swing.JScrollPane sp1 = new javax.swing.JScrollPane();
        rendezvousSeedURIs = new javax.swing.JList();
        javax.swing.JPanel panRendezvous = new javax.swing.JPanel();
        javax.swing.JPanel pan10 = new javax.swing.JPanel();
        javax.swing.JLabel lblRendezvous = new javax.swing.JLabel();
        rendezvousURIEditor = new javax.swing.JTextField();
        addRendezvous = new javax.swing.JButton();
        delRendezvous = new javax.swing.JButton();
        okRendezvous = new javax.swing.JButton();
        javax.swing.JScrollPane sp2 = new javax.swing.JScrollPane();
        rendezvousURIs = new javax.swing.JList();
        tabNode = new javax.swing.JScrollPane();
        javax.swing.JPanel panNode = new javax.swing.JPanel();
        javax.swing.JLabel lblNodeName = new javax.swing.JLabel();
        javax.swing.JLabel lblSiteName = new javax.swing.JLabel();
        javax.swing.JLabel lblLocation = new javax.swing.JLabel();
        javax.swing.JLabel lblUUID = new javax.swing.JLabel();
        nodeName = new javax.swing.JTextField();
        siteName = new javax.swing.JTextField();
        geoLocation = new javax.swing.JTextField();
        nodeUUID = new javax.swing.JTextField();
        tabNetworking = new javax.swing.JScrollPane();
        javax.swing.JPanel panelNet = new javax.swing.JPanel();
        javax.swing.JPanel panNetMenu = new javax.swing.JPanel();
        javax.swing.JScrollPane spNet2 = new javax.swing.JScrollPane();
        lstInterfaces = new javax.swing.JList();
        javax.swing.JPanel panNetControl = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        javax.swing.JScrollPane spNet1 = new javax.swing.JScrollPane();
        panInterfaceProperties = new javax.swing.JPanel();
        javax.swing.JPanel panBasic = new javax.swing.JPanel();
        javax.swing.JLabel lblIface = new javax.swing.JLabel();
        ifaceName = new javax.swing.JTextField();
        ipAddress = new javax.swing.JTextField();
        netMask = new javax.swing.JTextField();
        javax.swing.JLabel lblSlash = new javax.swing.JLabel();
        javax.swing.JLabel lblIpAddr = new javax.swing.JLabel();
        lanName = new javax.swing.JTextField();
        javax.swing.JLabel lblLanName = new javax.swing.JLabel();
        bandwidth = new javax.swing.JTextField();
        fullDuplex = new javax.swing.JCheckBox();
        javax.swing.JLabel lblBandwidth = new javax.swing.JLabel();
        ifaceUUID = new javax.swing.JTextField();
        javax.swing.JLabel lblIfaceUUID = new javax.swing.JLabel();
        javax.swing.JPanel panLambda = new javax.swing.JPanel();
        lambdaEndpoint = new javax.swing.JCheckBox();
        lambdaTagged = new javax.swing.JCheckBox();
        lambdaVlanId = new javax.swing.JTextField();
        lambdaNetworkId = new javax.swing.JTextField();
        javax.swing.JLabel lblNetworkId = new javax.swing.JLabel();
        lambdaLocalId = new javax.swing.JTextField();
        javax.swing.JLabel lblLocalId = new javax.swing.JLabel();
        btnNetOk = new javax.swing.JButton();
        tabLog = new javax.swing.JScrollPane();
        log = new javax.swing.JTextArea();
        btnCommit = new javax.swing.JButton();
        btnShutdown = new javax.swing.JButton();
        statusbar = new javax.swing.JLabel();

        setTitle("CoUniverse");
        setAutoRequestFocus(false);
        setBounds(new java.awt.Rectangle(0, 0, 0, 0));
        setName("mainFrame"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        enableAgc.setText("Enable AGC candidate");

        enableRendezvous.setText("Enable rendezvous node");

        panSeeds.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, pan10, org.jdesktop.beansbinding.ELProperty.create("${preferredSize}"), pan2, org.jdesktop.beansbinding.BeanProperty.create("preferredSize"));
        bindingGroup.addBinding(binding);

        lblSeeding.setText("Rendezvous seed URIs");

        addSeed.setText("+");
        addSeed.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                addSeedMouseClicked(evt);
            }
        });
        addSeed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addSeedActionPerformed(evt);
            }
        });

        delSeed.setText("-");
        delSeed.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                delSeedMouseClicked(evt);
            }
        });

        okSeed.setText("OK");
        okSeed.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                okSeedMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout pan2Layout = new javax.swing.GroupLayout(pan2);
        pan2.setLayout(pan2Layout);
        pan2Layout.setHorizontalGroup(
            pan2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pan2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pan2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblSeeding, javax.swing.GroupLayout.DEFAULT_SIZE, 182, Short.MAX_VALUE)
                    .addComponent(rendezvousSeedURIEditor)
                    .addGroup(pan2Layout.createSequentialGroup()
                        .addComponent(addSeed, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(0, 0, 0)
                        .addComponent(okSeed, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(0, 0, 0)
                        .addComponent(delSeed, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        pan2Layout.setVerticalGroup(
            pan2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pan2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblSeeding)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rendezvousSeedURIEditor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addGroup(pan2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addSeed)
                    .addComponent(delSeed)
                    .addComponent(okSeed))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        rendezvousSeedURIs.setModel(getRendezvousSeedUrisModel());
        rendezvousSeedURIs.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        rendezvousSeedURIs.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent lse) {
                String text = "";
                if ((rendezvousSeedURIs.getSelectedIndex() >= 0) && (rendezvousSeedURIs.getSelectedIndex() < rendezvousSeedUrisModel.getSize()))
                text = rendezvousSeedUrisModel.getElementAt(rendezvousSeedURIs.getSelectedIndex());
                rendezvousSeedURIEditor.setText(text);
            }
        });
        sp1.setViewportView(rendezvousSeedURIs);

        javax.swing.GroupLayout panSeedsLayout = new javax.swing.GroupLayout(panSeeds);
        panSeeds.setLayout(panSeedsLayout);
        panSeedsLayout.setHorizontalGroup(
            panSeedsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panSeedsLayout.createSequentialGroup()
                .addComponent(pan2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sp1, javax.swing.GroupLayout.DEFAULT_SIZE, 715, Short.MAX_VALUE)
                .addContainerGap())
        );
        panSeedsLayout.setVerticalGroup(
            panSeedsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pan2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(panSeedsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sp1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        panRendezvous.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        pan10.setPreferredSize(new java.awt.Dimension(206, 89));

        lblRendezvous.setText("Rendezvous URIs");

        addRendezvous.setText("+");
        addRendezvous.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                addRendezvousMouseClicked(evt);
            }
        });

        delRendezvous.setText("-");
        delRendezvous.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                delRendezvousMouseClicked(evt);
            }
        });

        okRendezvous.setText("OK");
        okRendezvous.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                okRendezvousMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout pan10Layout = new javax.swing.GroupLayout(pan10);
        pan10.setLayout(pan10Layout);
        pan10Layout.setHorizontalGroup(
            pan10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pan10Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pan10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(rendezvousURIEditor)
                    .addGroup(pan10Layout.createSequentialGroup()
                        .addComponent(lblRendezvous)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(pan10Layout.createSequentialGroup()
                        .addComponent(addRendezvous, javax.swing.GroupLayout.DEFAULT_SIZE, 54, Short.MAX_VALUE)
                        .addGap(0, 0, 0)
                        .addComponent(okRendezvous, javax.swing.GroupLayout.DEFAULT_SIZE, 73, Short.MAX_VALUE)
                        .addGap(0, 0, 0)
                        .addComponent(delRendezvous, javax.swing.GroupLayout.DEFAULT_SIZE, 55, Short.MAX_VALUE)))
                .addContainerGap())
        );
        pan10Layout.setVerticalGroup(
            pan10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pan10Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblRendezvous)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rendezvousURIEditor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pan10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addRendezvous)
                    .addComponent(delRendezvous)
                    .addComponent(okRendezvous))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        rendezvousURIs.setModel(getRendezvousUrisModel());
        rendezvousURIs.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        rendezvousURIs.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent lse) {
                String text = "";
                if ((rendezvousURIs.getSelectedIndex() >= 0) && (rendezvousSeedURIs.getSelectedIndex() < rendezvousSeedUrisModel.getSize()))
                text = rendezvousUrisModel.getElementAt(rendezvousURIs.getSelectedIndex());
                rendezvousURIEditor.setText(text);
            }
        });
        sp2.setViewportView(rendezvousURIs);

        javax.swing.GroupLayout panRendezvousLayout = new javax.swing.GroupLayout(panRendezvous);
        panRendezvous.setLayout(panRendezvousLayout);
        panRendezvousLayout.setHorizontalGroup(
            panRendezvousLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panRendezvousLayout.createSequentialGroup()
                .addComponent(pan10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sp2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );
        panRendezvousLayout.setVerticalGroup(
            panRendezvousLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pan10, javax.swing.GroupLayout.DEFAULT_SIZE, 95, Short.MAX_VALUE)
            .addGroup(panRendezvousLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sp2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout panelP2pLayout = new javax.swing.GroupLayout(panelP2p);
        panelP2p.setLayout(panelP2pLayout);
        panelP2pLayout.setHorizontalGroup(
            panelP2pLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelP2pLayout.createSequentialGroup()
                .addComponent(enableRendezvous)
                .addGap(0, 0, 0)
                .addComponent(enableAgc)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(panSeeds, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(panRendezvous, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        panelP2pLayout.setVerticalGroup(
            panelP2pLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelP2pLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelP2pLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(enableRendezvous)
                    .addComponent(enableAgc))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(panSeeds, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(panRendezvous, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tabP2p.setViewportView(panelP2p);

        tabs.addTab("P2P settings", tabP2p);

        lblNodeName.setText("Node name");

        lblSiteName.setText("Node site name");

        lblLocation.setText("Geo location");

        lblUUID.setText("Node UUID");

        nodeName.setText("moira.fi.muni.cz");

        siteName.setText("sitola");

        geoLocation.setText("49.209894, 16.598918");

        nodeUUID.setText("cdbbfdc4-bbbc-43ee-9f68-58035f153e13");

        javax.swing.GroupLayout panNodeLayout = new javax.swing.GroupLayout(panNode);
        panNode.setLayout(panNodeLayout);
        panNodeLayout.setHorizontalGroup(
            panNodeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panNodeLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panNodeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblNodeName)
                    .addComponent(lblSiteName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblLocation, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblUUID, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panNodeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(siteName)
                    .addComponent(nodeName)
                    .addComponent(geoLocation)
                    .addComponent(nodeUUID, javax.swing.GroupLayout.DEFAULT_SIZE, 796, Short.MAX_VALUE))
                .addContainerGap())
        );
        panNodeLayout.setVerticalGroup(
            panNodeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panNodeLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panNodeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblNodeName)
                    .addComponent(nodeName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panNodeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(siteName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblSiteName))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panNodeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(geoLocation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblLocation))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panNodeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nodeUUID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblUUID))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tabNode.setViewportView(panNode);

        tabs.addTab("Node properties", tabNode);

        lstInterfaces.setModel(getInterfacesModel());
        lstInterfaces.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lstInterfaces.setFocusCycleRoot(true);
        lstInterfaces.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent lse) {
                try {
                    setupInterface(lstInterfaces.getSelectedIndex());
                    statusbarInfo("Interface editor ready");
                } catch (JSONException ex) {
                    statusbarError("Error reseting the interface editor: "+ex.toString());
                }
            }
        });
        spNet2.setViewportView(lstInterfaces);

        jButton1.setText("+");
        jButton1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                addInterface(evt);
            }
        });

        jButton2.setText("-");
        jButton2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                delInterface(evt);
            }
        });

        javax.swing.GroupLayout panNetControlLayout = new javax.swing.GroupLayout(panNetControl);
        panNetControl.setLayout(panNetControlLayout);
        panNetControlLayout.setHorizontalGroup(
            panNetControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panNetControlLayout.createSequentialGroup()
                .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panNetControlLayout.setVerticalGroup(
            panNetControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jButton1)
            .addComponent(jButton2)
        );

        javax.swing.GroupLayout panNetMenuLayout = new javax.swing.GroupLayout(panNetMenu);
        panNetMenu.setLayout(panNetMenuLayout);
        panNetMenuLayout.setHorizontalGroup(
            panNetMenuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panNetMenuLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panNetMenuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(spNet2)
                    .addComponent(panNetControl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        panNetMenuLayout.setVerticalGroup(
            panNetMenuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panNetMenuLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spNet2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panNetControl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        panBasic.setBorder(javax.swing.BorderFactory.createTitledBorder("Basic interface settings"));

        lblIface.setText("Interface name");

        ifaceName.setText("eth0");

        ipAddress.setText("147.251.54.44");

        netMask.setText("24");

        lblSlash.setText("/");

        lblIpAddr.setText("IP address / netmask");

        lanName.setText("world");

        lblLanName.setText("Local network name");

        bandwidth.setText("1000M");

        fullDuplex.setText("Full duplex");

        lblBandwidth.setText("Bandwidth");

        ifaceUUID.setText("21f5c0cc-6135-4de0-96fa-292e93ce8876");

        lblIfaceUUID.setText("Interface UUID");

        javax.swing.GroupLayout panBasicLayout = new javax.swing.GroupLayout(panBasic);
        panBasic.setLayout(panBasicLayout);
        panBasicLayout.setHorizontalGroup(
            panBasicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panBasicLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panBasicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblIface)
                    .addComponent(lblLanName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblIpAddr, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblBandwidth, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblIfaceUUID, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panBasicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panBasicLayout.createSequentialGroup()
                        .addComponent(bandwidth)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fullDuplex))
                    .addGroup(panBasicLayout.createSequentialGroup()
                        .addComponent(ipAddress)
                        .addGap(0, 0, 0)
                        .addComponent(lblSlash)
                        .addGap(1, 1, 1)
                        .addComponent(netMask))
                    .addComponent(ifaceName)
                    .addComponent(lanName)
                    .addComponent(ifaceUUID, javax.swing.GroupLayout.DEFAULT_SIZE, 439, Short.MAX_VALUE))
                .addContainerGap())
        );
        panBasicLayout.setVerticalGroup(
            panBasicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panBasicLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panBasicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblIface)
                    .addComponent(ifaceName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panBasicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ipAddress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(netMask, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblSlash)
                    .addComponent(lblIpAddr))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panBasicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lanName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblLanName))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panBasicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(bandwidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fullDuplex)
                    .addComponent(lblBandwidth))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panBasicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ifaceUUID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblIfaceUUID))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panLambda.setBorder(javax.swing.BorderFactory.createTitledBorder("Lambda endpoint settings"));

        lambdaEndpoint.setText("Lambda endpoint");

        lambdaTagged.setText("Tagged vlan");

        lambdaVlanId.setText("vlanID");

        lambdaNetworkId.setText("lambdaNetworkId");

        lblNetworkId.setText("Network ID");

        lambdaLocalId.setText("lambdaLocalId");

        lblLocalId.setText("Local ID");

        javax.swing.GroupLayout panLambdaLayout = new javax.swing.GroupLayout(panLambda);
        panLambda.setLayout(panLambdaLayout);
        panLambdaLayout.setHorizontalGroup(
            panLambdaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panLambdaLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panLambdaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, panLambdaLayout.createSequentialGroup()
                        .addGroup(panLambdaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(lblNetworkId, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(lblLocalId, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panLambdaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lambdaLocalId)
                            .addComponent(lambdaNetworkId)))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, panLambdaLayout.createSequentialGroup()
                        .addComponent(lambdaTagged)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lambdaVlanId))
                    .addComponent(lambdaEndpoint, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        panLambdaLayout.setVerticalGroup(
            panLambdaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panLambdaLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lambdaEndpoint)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panLambdaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lambdaNetworkId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblNetworkId))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panLambdaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lambdaLocalId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblLocalId))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panLambdaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lambdaVlanId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lambdaTagged))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        btnNetOk.setText("Ok");
        btnNetOk.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnNetOkMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout panInterfacePropertiesLayout = new javax.swing.GroupLayout(panInterfaceProperties);
        panInterfaceProperties.setLayout(panInterfacePropertiesLayout);
        panInterfacePropertiesLayout.setHorizontalGroup(
            panInterfacePropertiesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panInterfacePropertiesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panInterfacePropertiesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panBasic, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panLambda, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panInterfacePropertiesLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnNetOk)))
                .addContainerGap())
        );
        panInterfacePropertiesLayout.setVerticalGroup(
            panInterfacePropertiesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panInterfacePropertiesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panBasic, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panLambda, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnNetOk)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        spNet1.setViewportView(panInterfaceProperties);
        panInterfaceProperties.setVisible(false);

        javax.swing.GroupLayout panelNetLayout = new javax.swing.GroupLayout(panelNet);
        panelNet.setLayout(panelNetLayout);
        panelNetLayout.setHorizontalGroup(
            panelNetLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelNetLayout.createSequentialGroup()
                .addComponent(panNetMenu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(spNet1))
        );
        panelNetLayout.setVerticalGroup(
            panelNetLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panNetMenu, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(spNet1)
        );

        tabNetworking.setViewportView(panelNet);

        tabs.addTab("Network interfaces", tabNetworking);

        log.setEditable(false);
        log.setColumns(20);
        log.setRows(5);
        tabLog.setViewportView(log);

        tabs.addTab("Comm log", tabLog);

        btnCommit.setText("Commit & reload");

        btnShutdown.setText("Shutdown node");
        btnShutdown.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnShutdownMouseClicked(evt);
            }
        });

        statusbar.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
        statusbar.setText("Starting & loading configuration...");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tabs)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusbar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnShutdown)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnCommit)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(tabs, javax.swing.GroupLayout.DEFAULT_SIZE, 389, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCommit)
                    .addComponent(btnShutdown)
                    .addComponent(statusbar))
                .addContainerGap())
        );

        bindingGroup.bind();

        pack();
    }// </editor-fold>//GEN-END:initComponents

        private void addSeedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addSeedActionPerformed
              // TODO add your handling code here:
        }//GEN-LAST:event_addSeedActionPerformed

    private void enableInterfaceEditor(boolean enable) {
        //panInterfaceProperties.setEnabled(enable);
        panInterfaceProperties.setVisible(enable);
    }
    
    private void enableLambdaEditor(boolean enable) {
        lambdaLocalId.setEnabled(enable);
        lambdaNetworkId.setEnabled(enable);
        lambdaTagged.setEnabled(enable);
        lambdaVlanId.setEnabled(enable);
    }
    
    private void setupInterface(int interfaceIndex) throws JSONException {
        if (interfaceIndex >= 0 && interfaceIndex < interfacesModel.getSize()) {
            setupInterface(interfacesModel.interfaces.getJSONObject(interfaceIndex));
        } else {
            enableInterfaceEditor(false);
        }
    }
    private void setupInterface(JSONObject iface) throws JSONException {
        enableInterfaceEditor(true);
        
        ifaceName.setText(iface.optString(EndpointNodeInterface.ConfigKeyIfName, "Undefined"));
        ipAddress.setText(iface.optString(EndpointNodeInterface.ConfigKeyAddress, "127.0.0.1"));
        netMask.setText(iface.optString(EndpointNodeInterface.ConfigKeyNetmask, "255.255.255.0"));
        fullDuplex.setSelected(iface.optBoolean(EndpointNodeInterface.ConfigKeyDuplex, true));
        bandwidth.setText(Double.toString(iface.optDouble(EndpointNodeInterface.ConfigKeyBandwidth, 1E10)));
        lanName.setText(iface.optString(EndpointNodeInterface.ConfigKeySubnetName, "world"));
        ifaceUUID.setText(iface.optString(EndpointNodeInterface.ConfigKeyUuid, UUID.randomUUID().toString()));

        enableLambdaEditor(false);
        /*
        LambdaLinkEndPoint lambda = iface.getLambdaLinkEndpoint();
        lambdaEndpoint.setSelected(lambda != null);
        enableLambdaEditor(lambdaEndpoint.isSelected());
        if (lambdaEndpoint.isSelected()) {
            //lambdaLocalId.setText(lambda.)
            lambdaTagged.setSelected(lambda.isLambdaLinkEndpointTagged());
            lambdaVlanId.setText(lambda.getLambdaLinkEndpointVlan());
        }
        */
    }
    private void commitInterface(int interfaceIndex) throws JSONException {
        JSONObject iface = interfacesModel.interfaces.getJSONObject(interfaceIndex);
        assert iface != null;
        
        iface.put(EndpointNodeInterface.ConfigKeyIfName, ifaceName.getText());
        iface.put(EndpointNodeInterface.ConfigKeyAddress, ipAddress.getText());
        iface.put(EndpointNodeInterface.ConfigKeyNetmask, netMask.getText());
        iface.put(EndpointNodeInterface.ConfigKeyDuplex, fullDuplex.isSelected());
        iface.put(EndpointNodeInterface.ConfigKeyBandwidth, Double.parseDouble(bandwidth.getText()));
        iface.put(EndpointNodeInterface.ConfigKeySubnetName, lanName.getText());
        iface.put(EndpointNodeInterface.ConfigKeyUuid, ifaceUUID.getText());
        
//        if (lambdaEndpoint.isSelected() && iface.getLambdaLinkEndpoint() == null) {
//            iface.setLambdaLinkEndpoint(new LambdaLinkEndPointNSI1);
//        }
        interfacesModel.fireReplaced();
    }
    
    private void addInterface(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_addInterface
        JSONObject iface = new JSONObject();
        try {
            iface = new EndpointNodeInterface().defaultConfig();
            iface.put(EndpointNodeInterface.ConfigKeyIfName, "newInterface");
        } catch (Exception ex) {
            statusbarError("Error loading defaults for new interface: " + ex.toString());
        }
        interfacesModel.interfaces.put(iface);
        enableInterfaceEditor(true);
        
        int next = interfacesModel.getSize()-1;
        lstInterfaces.setSelectedIndex(next);
        try {
            setupInterface(iface);
        } catch (JSONException ex) {
            statusbarError("Error during interface editor reset");
        }
        interfacesModel.fireReplaced();
    }//GEN-LAST:event_addInterface

    private void delInterface(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_delInterface
        if (lstInterfaces.getSelectedIndex() < 0) return;
        if (lstInterfaces.getSelectedIndex() >= interfacesModel.getSize()) return;
        
        int next = lstInterfaces.getSelectedIndex()-1;
        if (next < 0) next = 0;
        
        interfacesModel.interfaces.remove(lstInterfaces.getSelectedIndex());
        lstInterfaces.setSelectedIndex(next);
        interfacesModel.fireReplaced();
        try {
            setupInterface(next);
        } catch (JSONException ex) {
            statusbarError("Interface editor reset failed: "+ex.toString());
        }
    }//GEN-LAST:event_delInterface

    private void btnNetOkMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnNetOkMouseClicked
        try {
            commitInterface(lstInterfaces.getSelectedIndex());
            statusbarInfo("Interface ready for configuration commit");
        } catch (JSONException ex) {
            statusbarError("Interface not saved due to error: "+ex.toString());
        }
    }//GEN-LAST:event_btnNetOkMouseClicked

    private void addSeedMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_addSeedMouseClicked
        if (rendezvousSeedURIEditor.getText().isEmpty()) return;
        rendezvousSeedUrisModel.options.put(rendezvousSeedURIEditor.getText());
        rendezvousSeedUrisModel.fireIntervalAdded(rendezvousSeedUrisModel, rendezvousSeedUrisModel.getSize()-1, rendezvousSeedUrisModel.getSize());
        rendezvousSeedURIs.setSelectedIndex(rendezvousSeedUrisModel.getSize()-1);
    }//GEN-LAST:event_addSeedMouseClicked

    private void addRendezvousMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_addRendezvousMouseClicked
        if (rendezvousURIEditor.getText().isEmpty()) return;
        rendezvousUrisModel.options.put(rendezvousURIEditor.getText());
        rendezvousUrisModel.fireIntervalAdded(rendezvousUrisModel, rendezvousUrisModel.getSize()-1, rendezvousUrisModel.getSize());
        rendezvousURIs.setSelectedIndex(rendezvousUrisModel.getSize()-1);
    }//GEN-LAST:event_addRendezvousMouseClicked

    private void okSeedMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_okSeedMouseClicked
        if (rendezvousSeedURIEditor.getText().isEmpty()) return;
        if (rendezvousSeedURIs.getSelectedIndex() < 0) return;
        if (rendezvousSeedURIs.getSelectedIndex() >= rendezvousSeedUrisModel.getSize()) return;
        try {
            rendezvousSeedUrisModel.options.put(rendezvousSeedURIs.getSelectedIndex(), rendezvousSeedURIEditor.getText());
        } catch (JSONException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        }
        rendezvousSeedUrisModel.fireContentsChanged(rendezvousSeedUrisModel, rendezvousSeedURIs.getSelectedIndex(), rendezvousSeedURIs.getSelectedIndex()+1);
    }//GEN-LAST:event_okSeedMouseClicked

    private void okRendezvousMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_okRendezvousMouseClicked
        if (rendezvousURIEditor.getText().isEmpty()) return;
        if (rendezvousURIs.getSelectedIndex() < 0) return;
        if (rendezvousURIs.getSelectedIndex() >= rendezvousUrisModel.getSize()) return;
        try {
            rendezvousUrisModel.options.put(rendezvousURIs.getSelectedIndex(), rendezvousURIEditor.getText());
        } catch (JSONException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        }
        rendezvousUrisModel.fireContentsChanged(rendezvousUrisModel, rendezvousURIs.getSelectedIndex(), rendezvousURIs.getSelectedIndex()+1);
    }//GEN-LAST:event_okRendezvousMouseClicked

    private void delSeedMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_delSeedMouseClicked
        if (rendezvousSeedURIs.getSelectedIndex() < 0) return;
        if (rendezvousSeedURIs.getSelectedIndex() >= rendezvousSeedUrisModel.getSize()) return;
        try {
            rendezvousSeedUrisModel.options.remove(rendezvousSeedUrisModel.options.get(rendezvousSeedURIs.getSelectedIndex()));
        } catch (JSONException ex) {
            Logger.getLogger(MainFrame.class).log(Level.ERROR, "BUG: attempting to remove value from list, but there is no such value...", ex);
        }
        rendezvousSeedUrisModel.fireIntervalRemoved(rendezvousSeedUrisModel, rendezvousSeedURIs.getSelectedIndex(), rendezvousSeedURIs.getSelectedIndex()+1);
    }//GEN-LAST:event_delSeedMouseClicked

    private void delRendezvousMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_delRendezvousMouseClicked
        if (rendezvousURIs.getSelectedIndex() < 0) return;
        if (rendezvousURIs.getSelectedIndex() >= rendezvousUrisModel.getSize()) return;
        try {
            rendezvousUrisModel.options.remove(rendezvousUrisModel.options.get(rendezvousURIs.getSelectedIndex()));
        } catch (JSONException ex) {
            Logger.getLogger(MainFrame.class).log(Level.ERROR, "BUG: attempting to remove value from list, but there is no such value...", ex);
        }
        rendezvousUrisModel.fireIntervalRemoved(rendezvousUrisModel, rendezvousURIs.getSelectedIndex(), rendezvousURIs.getSelectedIndex()+1);
    }//GEN-LAST:event_delRendezvousMouseClicked

    
        private void btnShutdownMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnShutdownMouseClicked
            uiTasks.addTask(new ShutdownTask());
            this.setVisible(false);
        }//GEN-LAST:event_btnShutdownMouseClicked

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        uiTasks.addTask(new ShutdownTask());
    }//GEN-LAST:event_formWindowClosed

    public ArrayStringModel getRendezvousSeedUrisModel() {
        return rendezvousSeedUrisModel;
    }

    public ArrayStringModel getRendezvousUrisModel() {
        return rendezvousUrisModel;
    }

    public ArrayInterfaceModel getInterfacesModel() {
        return interfacesModel;
    }
    
    
        public void setupLookAndFeel() {
                /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
        * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
        */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
                Logger.getLogger(MainFrame.class.getName()).log(Level.DEBUG, null, ex);
        } catch (InstantiationException ex) {
                Logger.getLogger(MainFrame.class.getName()).log(Level.DEBUG, null, ex);
        } catch (IllegalAccessException ex) {
                Logger.getLogger(MainFrame.class.getName()).log(Level.DEBUG, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
                Logger.getLogger(MainFrame.class.getName()).log(Level.DEBUG, null, ex);
        }
        //</editor-fold>
        }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addRendezvous;
    private javax.swing.JButton addSeed;
    private javax.swing.JTextField bandwidth;
    private javax.swing.JButton btnCommit;
    private javax.swing.JButton btnNetOk;
    private javax.swing.JButton btnShutdown;
    private javax.swing.JButton delRendezvous;
    private javax.swing.JButton delSeed;
    private javax.swing.JCheckBox enableAgc;
    private javax.swing.JCheckBox enableRendezvous;
    private javax.swing.JCheckBox fullDuplex;
    private javax.swing.JTextField geoLocation;
    private javax.swing.JTextField ifaceName;
    private javax.swing.JTextField ifaceUUID;
    private javax.swing.JTextField ipAddress;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JCheckBox lambdaEndpoint;
    private javax.swing.JTextField lambdaLocalId;
    private javax.swing.JTextField lambdaNetworkId;
    private javax.swing.JCheckBox lambdaTagged;
    private javax.swing.JTextField lambdaVlanId;
    private javax.swing.JTextField lanName;
    private javax.swing.JTextArea log;
    private javax.swing.JList lstInterfaces;
    private javax.swing.JTextField netMask;
    private javax.swing.JTextField nodeName;
    private javax.swing.JTextField nodeUUID;
    private javax.swing.JButton okRendezvous;
    private javax.swing.JButton okSeed;
    private javax.swing.JPanel panInterfaceProperties;
    private javax.swing.JTextField rendezvousSeedURIEditor;
    private javax.swing.JList rendezvousSeedURIs;
    private javax.swing.JTextField rendezvousURIEditor;
    private javax.swing.JList rendezvousURIs;
    private javax.swing.JTextField siteName;
    private javax.swing.JLabel statusbar;
    private javax.swing.JScrollPane tabLog;
    private javax.swing.JScrollPane tabNetworking;
    private javax.swing.JScrollPane tabNode;
    private javax.swing.JTabbedPane tabs;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables

    private MapVisualizerPanel mapPanel = null;
    private GuiTasker uiTasks = null;
    private ArrayStringModel rendezvousSeedUrisModel = null;
    private ArrayStringModel rendezvousUrisModel = null;
    private ArrayInterfaceModel interfacesModel = null;
    private JSONObject currentPlan = null;
    private JSONObject currentNetworkNodes = null;
    private String thisNodeUuid = "";

}
