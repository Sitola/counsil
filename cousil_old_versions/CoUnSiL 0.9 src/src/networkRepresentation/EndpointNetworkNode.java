package networkRepresentation;

import core.Configurable;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import mediaAppFactory.ApplicationFactory;
import mediaAppFactory.MediaApplication;
import mediaAppFactory.MediaApplicationConsumer;
import mediaAppFactory.MediaApplicationDistributor;
import mediaAppFactory.MediaApplicationProducer;
import myJXTA.MyJXTAConnectorID;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.opensaml.artifact.InvalidArgumentException;
import p2p.ConnectorID;
import utils.ConfigUtils;
import utils.ProxyNodeConnection;
import utils.ProxyNodeInterfacePolycomFX;
import utils.ProxyNodeInterfacePolycomFake;
import utils.ProxyNodeInterfacePolycomHDX;

/**
 * Network node representation. Each network node should maintain information on itself. The NetworkNode
 * class supports serializing the instance into a XML to be sent over JXTA pipe.
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 31.7.2007
 * Time: 16:33:43
 */
public class EndpointNetworkNode extends GeneralNetworkNode implements Serializable, Configurable {
    public static final String ConfigKeySiteName = "siteName";
    public static final String ConfigKeyNodeInterfaces = "networkInterfaces";
    public static final String ConfigKeyNodeCpus = "cpus";
    public static final String ConfigKeyNodeGpuGFlops = "gpuGFlops";
    
    public static final String ConfigKeyTemplateGenerator = "templateType";
    public static final String ConfigKeyTemplateName = "templateName";
    public static final String ConfigKeyAppTemplates = "applicationTemplates";
    public static final String ConfigKeyAppsRunning = "runningApplications";
    
    public static final String ConfigKeyMyEndpointUserRole = "myEndpointUserRole";
    public static final String ConfigKeyIsDistributor = "isDistributor";
    public static final String ConfigKeyDebugUsingDummyUG = "debugUsingDummyUG";
    public static final String ConfigKeyDebugUsingInvisibleOverlays = "debugUsingInvisibleOverlays";
    
    public static final String DefaultNetworkSiteName = "";
    
    HashMap<String, MediaApplication> applicationTemplates;
    
    public class ApplicationTemplatesManager implements Configurable, Serializable {
        public MediaApplication getTemplate(String templateName) {
            return applicationTemplates.get(templateName);
        }
        public MediaApplication createTemplate(String templateName, String templateType) throws IllegalArgumentException, JSONException {
            MediaApplication app = ApplicationFactory.newApplication(templateType);
            if (app == null) throw new IllegalArgumentException("Unable to create instance of " + templateType + "; due to wrong generator name");
            app.loadConfig(app.defaultConfig());
            applicationTemplates.put(templateName, app);
            return app;
        }

        public MediaApplication instantiateLocalTemplate(String templateName) throws IllegalArgumentException, IOException, JSONException {
            MediaApplication template = getTemplate(templateName);
            if (template == null) {
                throw new IllegalArgumentException("No such (local) template: " + templateName);
            }
            
            MediaApplication newApp = null;
            try {
                newApp = mediaAppFactory.ApplicationFactory.newApplication(mediaAppFactory.ApplicationFactory.getGeneratorNickname(template));
            } catch (NullPointerException ex) {
                throw new IllegalArgumentException("No generator for template: " + templateName);
            }
            newApp.loadConfig(template.activeConfig());
            newApp.setUuid(UUID.randomUUID().toString());
            
            return newApp;
        }

        @Override
        public boolean loadConfig(JSONObject configuration) throws IllegalArgumentException, JSONException {
            boolean loaded = false;
            HashMap<String, MediaApplication> loadedTemplates = new HashMap<>();
            
            JSONObject templates = configuration.getJSONObject(ConfigKeyAppTemplates);
            if (templates == null) return false;
            
            Iterator<Object> keyIterator = templates.keys();
            while (keyIterator.hasNext()) {
                Object keyValue = keyIterator.next();
                if (!(keyValue instanceof String)) {
                    Logger.getLogger(this.getClass()).log(Level.FATAL, "Object " + keyValue.toString() + " is not a string application template name");
                    continue;
                }
                
                String templateName = keyValue.toString();
                
                JSONObject appTemplateConfig = templates.getJSONObject(templateName);
                String generatorName = appTemplateConfig.getString(ConfigKeyTemplateGenerator);
                //String templateName = appTemplateConfig.getString(ConfigKeyTemplateName);

                MediaApplication app = ApplicationFactory.newApplication(generatorName);
                if (app == null) throw new IllegalArgumentException("Unable to create instance of " + generatorName + "; due to wrong generator name");
                applicationTemplates.put(templateName, app);

                if (app.loadConfig(appTemplateConfig)) {
                    loadedTemplates.put(templateName, app);
                    loaded |= true;
                } else {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, "Failed to load configuration for " + templateName + " from " + configuration.toString());
                }
            }
            
            // DO NOT DUMP RUNNING NODEAPPLICATIONS

            applicationTemplates = loadedTemplates;
            return loaded;
        }

        @Override
        public JSONObject activeConfig() throws JSONException {
            JSONObject config = new JSONObject();
            JSONObject templates = new JSONObject();
            config.put(ConfigKeyAppTemplates, templates);

            for (Map.Entry<String, MediaApplication> template : applicationTemplates.entrySet()) {
                JSONObject appTemplateConfig = new JSONObject();
                templates.put(template.getKey(), appTemplateConfig);
                appTemplateConfig.put(ConfigKeyTemplateGenerator, ApplicationFactory.getGeneratorNickname(template.getValue()));
//                appTemplateConfig.put(ConfigKeyTemplateName, template.getKey());
                ConfigUtils.mergeConfig(appTemplateConfig, template.getValue().activeConfig());
            }
            
            return config;
        }

        @Override
        public JSONObject defaultConfig() throws JSONException {
            return new JSONObject();
        }
    }
    public class ApplicationConfigurationManager implements Configurable, Serializable {

        /**
         * DO NOT USE THIS UNLESS YOU KNOW EXACTLY WHAT YOU'RE DOING
         */
        public void addApplication(MediaApplication ma) {
            nodeApplications.add(ma);
            ma.setParentNode(EndpointNetworkNode.this);
        }

        /**
         * DO NOT USE THIS UNLESS YOU KNOW EXACTLY WHAT YOU'RE DOING
         */
        public void removeApplication(MediaApplication ma) throws IllegalArgumentException {
            if (ma.getParentNode() != null && !EndpointNetworkNode.this.equals(ma.getParentNode())) throw new IllegalArgumentException("Application " + ma.getUuid() + " is not local as it's parent is not local node !");

            nodeApplications.remove(ma);
            ma.setParentNode(null);
        }
        
        private TreeSet<String> extractOrderedKeys(JSONObject o) {
            TreeSet<String> retval = new TreeSet<String>();
            
            Iterator<String> keys = o.keys();
            while (keys.hasNext()) {
                retval.add(keys.next());
            }
        
            return retval;
        }

        @Override
        public synchronized boolean loadConfig(JSONObject configuration) throws IllegalArgumentException, JSONException {
            boolean changed = false;
            
            JSONObject oldConfiguration = activeConfig();
            TreeSet<String> newKeysOrdered = extractOrderedKeys(configuration);
            
            for (String key : newKeysOrdered) {
                if (!oldConfiguration.has(key)) {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, "No application with uuid " + key + " available for configuration");
                    continue;
                }
                
                // find app by uuid
                MediaApplication app = mediaAppFactory.ApplicationFactory.filterByUuidOrName(key, nodeApplications);
                assert app != null;
                
                // call configure on app
                JSONObject request = configuration.getJSONObject(key);
                if (request == null) {
                    Logger.getLogger(this.getClass()).log(Level.WARN, "Loaded configuration does not contain " + key + ", skipping entry");
                    continue;
                }
                
                if (request.equals(app.activeConfig())) continue;
                
                // under no circumstances update uuid
                request.put(MediaApplication.ConfigKeyUuid, key);

                try {
                    changed |= app.loadConfig(request);
                } catch (IllegalArgumentException | JSONException ex) {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, "Failed to configure application " + app.getUuid() + ": " + ex.toString(), ex);
                }
            }
            
            return changed;
        }

        @Override
        public JSONObject activeConfig() throws JSONException {
            JSONObject running = new JSONObject();
            for (MediaApplication app : nodeApplications) {
                running.put(app.getUuid().toString(), app.activeConfig());
            }
            return running;
        }

        @Override
        public JSONObject defaultConfig() throws JSONException {
            return new JSONObject();
        }
    }

    private NetworkSite nodeSite;
    private final CopyOnWriteArraySet<MediaApplication> nodeApplications = new CopyOnWriteArraySet<>();  // applications available on the node
    private CopyOnWriteArraySet<EndpointNodeInterface> nodeInterfaces = new CopyOnWriteArraySet<>();  // node interfaces described using its IP addresses

    private final CopyOnWriteArraySet<ProxyNetworkNode> proxyNodes = new CopyOnWriteArraySet<>();

    // TODO: Transfer to ProxyNode class
    private boolean localNode;  // is this node me?
    private boolean proxyNode;  // is this node actually a proxy node that controls the real node which can't run CoUniverse
    private String proxyString;  // specification of the proxy, used to create connection to the proxied box
    private transient ProxyNodeConnection proxyNodeConnection;  // connection to proxy node

/* COUNSIL STUFF **/
    EndpointUserRole myEndpointUserRole;  //represents role of user   
    String ultraGridConsumerParameters; // UltraGrid parameters for creating consumer window
    ArrayList<EndpointUserRole> possibleRoles; // roles to choose from
    ArrayList<EndpointUserRole> desiredStreams; // desired roles for recieving their media
    ArrayList<Session> sessions; // possible sessions
    boolean isDistributor;
    boolean debugUsingDummyUG;
    boolean debugUsingInvisibleOverlays;
/* COUNSIL STUFF **/

    // \TODO
    private float gpuGFlops;
    private int cpus;
    // TODO: remove this
    @Deprecated
    transient public int index;
    
    /**
     * This is an empty JavaBean constructor in order to support XMLEncoder and XMLDecoder
     */
    public EndpointNetworkNode() {
        super(GeneralNetworkNode.NODE_TYPE_ENDPOINT);

        this.localNode = true;
        this.proxyNode = false;
        this.nodeSite = new NetworkSite(DefaultNetworkSiteName);
        this.applicationTemplates = new HashMap<>();

/* COUNSIL **/
        this.myEndpointUserRole=new EndpointUserRole("");
        this.sessions=new ArrayList<Session>();
        this.ultraGridConsumerParameters="";
        this.possibleRoles=new ArrayList<EndpointUserRole>();
        this.desiredStreams=new ArrayList<EndpointUserRole>();
        this.isDistributor=false;
        this.debugUsingDummyUG = false;
        this.debugUsingInvisibleOverlays = false;
/* COUNSIL **/
    }

    /**
     * Gets node peer ID in the P2P network.
     * <p/>
     *
     * @return ID of the node
     */
    public ConnectorID getNodePeerID() {
        return new MyJXTAConnectorID(getUuid());
    }

    /**
     * Does this object represent a local node?
     * </p>
     *
     * @return true iff the node is local
     */
    @Deprecated
    public boolean isLocalNode() {
        return localNode;
    }

    /**
     * Returns the site the node is belonging to.
     * <p/>
     *
     * @return site representation
     */
    public NetworkSite getNodeSite() {
        return nodeSite;
    }

    /**
     * JavaBean enforced setter
     * <p/>
     *
     * @param localNode is it a local node?
     */
    @Deprecated
    public void setLocalNode(boolean localNode) {
        this.localNode = localNode;
    }

    /**
     * JavaBean enforced setter
     * <p/>
     *
     * @param nodeSite home site of the node
     */
    public void setNodeSite(NetworkSite nodeSite) {
        this.nodeSite = nodeSite;
    }

    /**
     * JavaBean enforced setter
     * <p/>
     *
     * @param nodeApplications applications running on the node
     */
    @Deprecated
    public synchronized void setNodeApplications(Set<MediaApplication> nodeApplications) {
        for (MediaApplication ma : this.nodeApplications) {
            getApplicationManager().removeApplication(ma);
        }

        for (MediaApplication ma : nodeApplications) {
            getApplicationManager().addApplication(ma);
        }
    }

    /**
     * JavaBean enforced setter
     * <p/>
     *
     * @param nodeInterfaces node interfaces
     */
    @Deprecated
    public synchronized void setNodeInterfaces(Set<EndpointNodeInterface> nodeInterfaces) {
        for (EndpointNodeInterface ni : this.nodeInterfaces) {
            removeInterface(ni);
        }

        for (EndpointNodeInterface ni : nodeInterfaces) {
            addInterface(ni);
        }
    }

    /**
     * JavaBean enforced getter
     * <p/>
     *
     * @return list of MediaApplications of the node
     */
    public Set<MediaApplication> getNodeApplications() {
        return new HashSet<>(nodeApplications);
    }

    /**
     * JavaBean enforced getter
     * <p/>
     *
     * @return list of NodeInterfaces of the node
     */
    public Set<EndpointNodeInterface> getNodeInterfaces() {
        return new HashSet<>(nodeInterfaces);
    }

    public EndpointNodeInterface getNodeInterface(String nodeInterfaceName) {
        for (EndpointNodeInterface iface : nodeInterfaces) {
            if (iface.getNodeInterfaceName().equals(nodeInterfaceName)) {
                return iface;
            }
        }
        return null;
    }

//    public EndpointNodeInterface getNodeInterface(EndpointSubNetwork subnet) {
//        for (EndpointNodeInterface iface : this.nodeInterfaces) {
//            if (iface.getSubnet().equals(subnet.getSubNetName()))
//                return iface;
//        }
//        return null;
//    }

    private final ApplicationConfigurationManager applicationsManager = new ApplicationConfigurationManager();
    public ApplicationConfigurationManager getApplicationManager() {
        return applicationsManager;
    }
    private final ApplicationTemplatesManager applicationTemplatesManager = new ApplicationTemplatesManager();
    public ApplicationTemplatesManager getApplicationTemplates() {
        return applicationTemplatesManager;
    }
    
    
    
    /**
     * Adds an interface to the existing list
     * <p/>
     *
     * @param address interface object for the node
     */
    public void addInterface(EndpointNodeInterface address) {
        if (address.getParentNode() != this && address.getParentNode() != null) {
            throw new IllegalArgumentException("Attempting to add a non-local interface >>"+address+"<< of node >>"+address.getParentNode()+"<< to node >>"+this+"<<!!!");
        }
        nodeInterfaces.add(address);
    }

    /**
     * Removes an existing interface from the list
     * <p/>
     *
     * @param ni the NetworkInterface to remove
     */
    public void removeInterface(EndpointNodeInterface ni) {
        if (! this.equals(ni.getParentNode())) throw new IllegalArgumentException("Node ("+this+") is not parrent of interface ("+ni+")!!");

        nodeInterfaces.remove(ni);
        ni.setParentNode(null);
    }

    public boolean hasDistributor() {
        for (MediaApplication ma : this.nodeApplications) {
            if (ma instanceof MediaApplicationDistributor) {
                return true;
            }
        }

        return false;
    }

    public boolean hasProducer() {
        for (MediaApplication ma : this.nodeApplications) {
            if (ma instanceof MediaApplicationProducer) {
                return true;
            }
        }

        return false;
    }

    public boolean hasConsumer() {
        for (MediaApplication ma : this.nodeApplications) {
            if (ma instanceof MediaApplicationConsumer) {
                return true;
            }
        }

        return false;
    }

    @Deprecated
    public boolean isProxyNode() {
        return proxyNode;
    }

    @Deprecated
    public void setProxyNode(boolean proxyNode) {
        this.proxyNode = proxyNode;
    }

    @Deprecated
    public String getProxyString() {
        return proxyString;
    }

    @Deprecated
    public void setProxyString(String proxyString) {
        this.proxyString = proxyString;
    }

    @Deprecated
    public void initProxyNodeConnection() {
        String[] parts = proxyString.split(":");
        if (parts[0].equals(ProxyNodeConnection.PolycomHDXType)) {
            assert parts.length == 4;
            proxyNodeConnection = new ProxyNodeConnection(new ProxyNodeInterfacePolycomHDX(parts[1], Integer.decode(parts[2]), parts[3]));
            proxyNodeConnection.start();
        }
        else if (parts[0].equals(ProxyNodeConnection.PolycomFXType)) {
            assert parts.length == 4;
            proxyNodeConnection = new ProxyNodeConnection(new ProxyNodeInterfacePolycomFX(parts[1], Integer.decode(parts[2]), parts[3]));
            proxyNodeConnection.start();
        }
        else if (parts[0].equals(ProxyNodeConnection.PolycomFakeType)) {
            assert parts.length == 4;
            proxyNodeConnection = new ProxyNodeConnection(new ProxyNodeInterfacePolycomFake(parts[1], Integer.decode(parts[2]), parts[3]));
            proxyNodeConnection.start();
        }
    }

    @Deprecated
    public void stopProxyNodeConnection() {
        assert proxyNodeConnection != null;
        proxyNodeConnection.stop();
    }

    @Deprecated
    public ProxyNodeConnection getProxyNodeConnection() {
        return proxyNodeConnection;
    }

    @Deprecated
    public void setProxyNodeConnection(ProxyNodeConnection proxyNodeConnection) {
        this.proxyNodeConnection = proxyNodeConnection;
    }

    @Override
    public String toString() {
        return "" + this.getNodeName() + " (" + this.getNodePeerID() + ") of " + this.getNodeSite().getSiteName() + ", role is: " + getMyEndpointUserRole().getMyRole() + ", is distributor: " + getIsDistributor();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EndpointNetworkNode that = (EndpointNetworkNode) o;

        return super.equals(that);
    }

    @Override
    public boolean deepEquals(GeneralNetworkNode node) {
        return this.deepEquals(node, false);
    }
    @Override
    public boolean deepEquals(GeneralNetworkNode node, boolean unique) {
        if (this == node) return true;
        if (node == null || getClass() != node.getClass()) return false;

        if (! super.deepEquals(node, unique)) return false;

        /** @todo consider using counsil-speciffic attributes **/

        EndpointNetworkNode that = (EndpointNetworkNode) node;

        if (unique) {
            if (this.nodeSite == that.nodeSite) return false;
            if (this.nodeApplications == that.nodeApplications) return false;
        }

        if (! this.nodeSite.equals(that.nodeSite)) return false;

        if (this.nodeApplications.size() != that.nodeApplications.size()) return false;

        for (MediaApplication thisApp : this.nodeApplications) {
            boolean found = false;
            for (MediaApplication thatApp : that.nodeApplications) {
                if (thisApp.equals(thatApp)) {
                    if (found) throw new IllegalStateException("Application " + thisApp + " matches multiple applications!");
                    if (unique && thisApp == thatApp) return false;
                    found = true;
                }
            }
            if (! found) return false;
        }

        for (MediaApplication thatApp : that.nodeApplications) {
            boolean found = false;
            for (MediaApplication thisApp : this.nodeApplications) {
                if (thatApp.equals(thisApp)) {
                    if (found) throw new IllegalStateException("Application " + thisApp + " matches multiple applications!");
                    if (unique && thisApp == thatApp) return false;
                    found = true;
                }
            }
            if (! found) return false;
        }


        if (this.nodeInterfaces.size() != that.nodeInterfaces.size()) return false;

        for (EndpointNodeInterface thisIface : this.nodeInterfaces) {
            boolean found = false;
            for (EndpointNodeInterface thatIface : that.nodeInterfaces) {
                if (thisIface.equals(thatIface)) {
                    if (! thisIface.deepEquals(thatIface)) return false;
                    found = true;
                    break;
                }
            }
            if (! found) return false;
        }

        for (EndpointNodeInterface thatIface : that.nodeInterfaces) {
            boolean found = false;
            for (EndpointNodeInterface thisIface : this.nodeInterfaces) {
                if (thatIface.equals(thisIface)) {
                    if (! thatIface.deepEquals(thisIface)) return false;
                    found = true;
                    break;
                }
            }
            if (! found) return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return getUuid().hashCode();
    }

    @Override
    public boolean update(GeneralNetworkNode node) {
        // TODO: check! May the ifaces change as well? May any other properties change?
        if (! this.equals(node)) throw new IllegalArgumentException("Cannot update node ("+this+"); The argument ("+node+") does not correspond to this node!");

        if (!(node instanceof EndpointNetworkNode)) {
            return false;
        }
        EndpointNetworkNode newNode = (EndpointNetworkNode) node;
        boolean ret = false;
        if (!this.nodeApplications.equals( ((EndpointNetworkNode)node).getNodeApplications() )) {
            this.setNodeApplications( ((EndpointNetworkNode)node).getNodeApplications() );
            return true;
        }
        for (MediaApplication app : newNode.getNodeApplications()) {
            if (!(app instanceof MediaApplicationConsumer)) {
                continue;
            }
            MediaApplicationConsumer c = (MediaApplicationConsumer) app;
            MediaApplicationConsumer oldC = null;
            for (MediaApplication a : this.nodeApplications) {
                if (a instanceof MediaApplicationConsumer && ((MediaApplicationConsumer) a).equals(c)) {
                    oldC = (MediaApplicationConsumer) a;
                    break;
                }
            }
            assert oldC != null;
            if (c.getSourceSite() == null) {
                if (oldC.getSourceSite() != null) {
                    oldC.setSourceSite(null);
                    ret = true;
                }
                continue;
            }
            if (!c.getSourceSite().equals(oldC.getSourceSite())) {
                oldC.setSourceSite(c.getSourceSite());
                ret = true;
            }
        }
        return ret;
    }

    public void addProxyNode(ProxyNetworkNode proxy) {
        proxyNodes.add(proxy);
    }

    public Set<ProxyNetworkNode> getProxyNodes() {
        return new HashSet<>(proxyNodes);
    }

    public boolean removeProxyNode(ProxyNetworkNode proxy) {
        return proxyNodes.remove(proxy);
    }
    
    public Set<Map.Entry<String, MediaApplication> > getTemplates(){
        return applicationTemplates.entrySet();
    }

    public EndpointUserRole getMyEndpointUserRole(){
        return myEndpointUserRole;
    }
    
    public boolean getIsDistributor(){
        return isDistributor;
    }

    public boolean isDebugUsingDummyUG() {
        return debugUsingDummyUG;
    }

    public void setDebugUsingDummyUG(boolean debugUsingDummyUG) {
        this.debugUsingDummyUG = debugUsingDummyUG;
    }

    public boolean isDebugUsingInvisibleOverlays() {
        return debugUsingInvisibleOverlays;
    }

    public void setDebugUsingInvisibleOverlays(boolean debugUsingInvisibleOverlays) {
        this.debugUsingInvisibleOverlays = debugUsingInvisibleOverlays;
    }
    
    
    
    public ArrayList<EndpointUserRole> getPossibleRoles(){
        return possibleRoles;
    }
        
    public ArrayList<EndpointUserRole> getDesiredStreams(){
        return desiredStreams;
    }
    public String getUltraGridConsumerParameters(){
        return this.ultraGridConsumerParameters;
    }
    
    public ArrayList<Session> getSessions(){
        return this.sessions;
    }
    
    public void setMyEndpointUserRole(EndpointUserRole myEndpointUserRole){
        this.myEndpointUserRole=myEndpointUserRole;
    }
    
    public void setIsDistributor(boolean isDistributor){
        this.isDistributor=isDistributor;
    }
    
    public void setSessions(ArrayList<Session> sessions){
        this.sessions=sessions;
    }
    
    public void setPossibleRoles(ArrayList<EndpointUserRole> possibleRoles){
        this.possibleRoles=possibleRoles;
    }
    
    public void setUltraGridConsumerParameters(String ultraGridConsumerParameters){
        this.ultraGridConsumerParameters=ultraGridConsumerParameters;
    }
    
    public void setDesiredStreams(ArrayList<EndpointUserRole> desiredStreams){
        this.desiredStreams=desiredStreams;
    }
    
    @Override
    public boolean loadConfig(JSONObject configuration) {
        String tmpSiteName = getNodeSite().getSiteName();
        Set<EndpointNodeInterface> tmpNetworkInterfaces = new HashSet<EndpointNodeInterface>();
        float tmpGpuFlops;
        int tmpCpus;


        try {
            tmpSiteName = configuration.getString(ConfigKeySiteName);

            JSONArray interfaces = configuration.getJSONArray(ConfigKeyNodeInterfaces);
            for (int i = 0; i < interfaces.length(); ++i){
                JSONObject ifaceConf = interfaces.getJSONObject(i);
                EndpointNodeInterface iface = new EndpointNodeInterface();
                iface.loadConfig(ifaceConf);
                tmpNetworkInterfaces.add(iface);
            }

            tmpGpuFlops = (float)configuration.getDouble(ConfigKeyNodeGpuGFlops);
            tmpCpus = configuration.getInt(ConfigKeyNodeCpus);
            
            myEndpointUserRole = new EndpointUserRole(configuration.getString(ConfigKeyMyEndpointUserRole));
            isDistributor = Boolean.parseBoolean(configuration.getString(ConfigKeyIsDistributor));
            debugUsingDummyUG = Boolean.parseBoolean(configuration.getString(ConfigKeyDebugUsingDummyUG));
            debugUsingInvisibleOverlays = Boolean.parseBoolean(configuration.getString(ConfigKeyDebugUsingInvisibleOverlays));

        } catch (JSONException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Given config object does not contain all keys. Compare keys with defaultConfig().\nOriginal error was: " + ex.toString());
        }

        boolean updated = false;
        synchronized (this) {
            updated = super.loadConfig(configuration);
            updated |= !tmpSiteName.equals(getNodeSite().getSiteName());
            updated |= !tmpNetworkInterfaces.equals(getNodeInterfaces());
            updated |= tmpGpuFlops != gpuGFlops;
            updated |= tmpCpus != cpus;

            nodeSite.setSiteName(tmpSiteName);
            nodeInterfaces = new CopyOnWriteArraySet<>(tmpNetworkInterfaces);
            gpuGFlops = tmpGpuFlops;
            cpus = tmpCpus;
        }

        return updated;
    }

    @Override
    public JSONObject activeConfig() throws JSONException {
        JSONObject root = super.activeConfig();
        root.put(ConfigKeySiteName, getNodeSite().getSiteName());

        JSONArray ifaces = new JSONArray();
        for (EndpointNodeInterface iface : getNodeInterfaces()) {
            ifaces.put(iface.activeConfig());
        }
        root.put(ConfigKeyNodeInterfaces, ifaces);
        root.put(ConfigKeyNodeCpus, cpus);
        root.put(ConfigKeyNodeGpuGFlops, gpuGFlops);

        return root;
    }

    @Override
    public JSONObject defaultConfig() throws JSONException {
        JSONObject root = super.defaultConfig();
        root.put(ConfigKeySiteName, DefaultNetworkSiteName);

        JSONArray ifaces = new JSONArray();
        ifaces.put(new EndpointNodeInterface().defaultConfig());
        root.put(ConfigKeyNodeInterfaces, ifaces);
        root.put(ConfigKeyNodeCpus, 1);
        root.put(ConfigKeyNodeGpuGFlops, 5);

        return root;
    }
}
