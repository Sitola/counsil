package networkRepresentation;

import utils.GeoLocation;
import core.Configurable;
import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.UUID;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import utils.ConfigUtils;

/**
 * General network node representation, including endpoints, network devices, and unknown networks.. The GeneralNetworkNode
 * class supports serializing the instance into a XML to be sent over JXTA pipe.
 * <p/>
 * User: Pavel Troubil (pavel@ics.muni.cz)
 */
public abstract class GeneralNetworkNode implements Serializable, Configurable {
    
    public static final String ConfigKeyLocation = "location";
    public static final String ConfigKeyNodeName = "nodeName";
    public static final String ConfigKeyUUID = "uuid";
        
    
    public static final int NODE_TYPE_ENDPOINT = 1;
    public static final int NODE_TYPE_PHYSICAL = 2;
    public static final int NODE_TYPE_UNKNOWN_NETWORK = 3;
    
    static Logger logger = Logger.getLogger("NetworkRepresentation");

    private String nodeName;  // human readable node name
    private String uuid;
    // TODO: replace with something serializable
    private GeoLocation geoLocation = new GeoLocation(0.0, 0.0);
    final public int nodeType;


    /**
     * @param nodeType
     */
    public GeneralNetworkNode(int nodeType) {
        this.nodeName = "";
        this.uuid = UUID.randomUUID().toString();

        this.geoLocation = new GeoLocation(0, 0);
        this.nodeType = nodeType;
    }

    /**
     * GeneralNetworkNode constructor.
     * <p/>
     *
     * @param nodeName         name of the node
     * @param nodeType
     */
    public GeneralNetworkNode(String nodeName, int nodeType) {
        this.nodeName = nodeName;
        this.uuid = UUID.randomUUID().toString();

        this.geoLocation = new GeoLocation();
        this.nodeType = nodeType;
    }

    /**
     * Gets node name.
     * <p/>
     *
     * @return node name
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     * JavaBean enforced setter
     * <p/>
     *
     * @param nodeName node name
     */
    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public void setGeoLocation(GeoLocation geoLocation) {
        this.geoLocation = geoLocation;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return "" + this.getNodeName();
    }

    /* TODO: update */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(this.getClass().isInstance(o))) return false;

        GeneralNetworkNode that = (GeneralNetworkNode) o;

        return uuid.equals(that.uuid);

    }

    public boolean deepEquals(GeneralNetworkNode that) {
        return deepEquals(that, false);
    }
    public boolean deepEquals(GeneralNetworkNode that, boolean unique) {

        if (this == that) return (!unique);
        if (that == null) return false;
        
        if (! this.getClass().isInstance(that)) return false;
        
        if (! this.uuid.equals(that.uuid)) return false;
        if (! this.nodeName.equals(that.nodeName)) return false;
        
        if (this.nodeType != that.nodeType) return false;
        if (this.geoLocation != that.geoLocation) return false;
        
        return true;
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
    
    /**
     * Copy the latest updates to this node;
     * @param node The recent snapshot of the node
     * @return Was modified?
     */
    public abstract boolean update(GeneralNetworkNode node);
    
    public GeoLocation getGeoLocation() {
        return geoLocation;
    }

    @Override
    public boolean loadConfig(JSONObject configuration) throws IllegalArgumentException {
        boolean updated = false;
        
        GeoLocation tmpGeoLocation = new GeoLocation();
        String tmpNodeName = getNodeName();
        String tmpUuid = getUuid();

        try {
            tmpGeoLocation.loadConfig(configuration.getJSONObject(ConfigKeyLocation));
            tmpNodeName = configuration.getString(ConfigKeyNodeName);
            tmpUuid = configuration.getString(ConfigKeyUUID);
        } catch (JSONException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Given config object does not contain all keys. Compare keys with defaultConfig().");
        }
        
        synchronized (this) {
            updated |= !tmpGeoLocation.equals(getGeoLocation());
            updated |= !tmpNodeName.equals(getNodeName());
            updated |= !tmpUuid.equals(getUuid());

            setGeoLocation(tmpGeoLocation);
            setNodeName(tmpNodeName);
            setUuid(tmpUuid);
        }
        
        return updated;
    }

    @Override
    public JSONObject activeConfig() throws JSONException {
        JSONObject root = new JSONObject();
        root.put(ConfigKeyLocation, geoLocation.activeConfig());

        // default applications
        root.put(ConfigKeyNodeName, getNodeName());
        root.put(ConfigKeyUUID, getUuid());

        return root;
    }

    @Override
    public JSONObject defaultConfig() throws JSONException {
        JSONObject root = new JSONObject();
        JSONObject loc = new GeoLocation().defaultConfig();
        root.put(ConfigKeyLocation, loc);

        // default applications
        String defaultNodeName = null;
        try {
            defaultNodeName = java.net.InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            defaultNodeName = "localhost";
        }        
        root.put(ConfigKeyNodeName, defaultNodeName);
        root.put(ConfigKeyUUID, UUID.randomUUID().toString());
        return root;
    }    
    
}
