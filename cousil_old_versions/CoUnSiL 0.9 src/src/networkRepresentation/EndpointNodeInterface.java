package networkRepresentation;

import core.Configurable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Description of particular interface of an endpoint network node.
 * Node interfaces are connected to a particular SubNetworks. 
 * <p/>
 * User: Pavel Troubil (pavel@ics.muni.cz)
 */
public class EndpointNodeInterface implements Serializable, Configurable {
    public static final String ConfigKeyBandwidth = "bandwidth";
    public static final String ConfigKeyDuplex = "fullDuplex";
    public static final String ConfigKeyAddress = "ipaddr";
    public static final String ConfigKeyNetmask = "netmask";
    public static final String ConfigKeyIfName = "iface";
    public static final String ConfigKeySubnetName = "lanName";
    public static final String ConfigKeyUuid = GeneralNetworkNode.ConfigKeyUUID;
        
    private String nodeInterfaceName;
    private String ipAddress;
    private String netMask;
    private double bandwidth;
    private String subnetName;
    private LambdaLinkEndPoint lambdaLinkEndpoint;
    private EndpointNetworkNode parentNode;
    private String uuid;
    private boolean isFullDuplex = false;

    public EndpointNodeInterface() {
        this.nodeInterfaceName = "lo";
        this.ipAddress = "127.0.0.1";
        this.netMask = "255.0.0.0";
        this.bandwidth = 0;
        this.subnetName = "";
        this.parentNode = null;
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
    }

    public EndpointNodeInterface(String hostInterface, String ipAddress, String netMask, long bandwidth, String subnetName, EndpointNetworkNode parentNode) {
        this.nodeInterfaceName = hostInterface;
        this.ipAddress = ipAddress;
        this.netMask = netMask;
        this.bandwidth = bandwidth;
        this.subnetName = subnetName;
        this.parentNode = parentNode;
        this.uuid = UUID.randomUUID().toString();
    }

    /**
     * Getter for node interface IP address
     * <p>
     *
     * @return String IP of the interface
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Setter for the node interface  IP address
     * <p>
     *
     * @param ipAddress String
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Getter for the node interface netmask
     * <p>
     *
     * @return String interface network mask
     */
    public String getNetMask() {
        return netMask;
    }

    /**
     * Setter for the interface netmask
     * <p>
     *
     * @param netMask String
     */
    public void setNetMask(String netMask) {
        this.netMask = netMask;
    }

    /**
     * Getter for the interface bandwidth
     * <p>
     *
     * @return double bandwidth
     */
    public double getBandwidth() {
        return bandwidth;
    }

    /**
     * Setter for the interface bandwidth
     * <p>
     *
     * @param bandwidth double
     */
    public void setBandwidth(double bandwidth) {
        this.bandwidth = bandwidth;
    }

    /**
     * Setter for interface subnet
     * <p>
     *
     * @param subnet SubNetwork
     */
    public void setSubNetwork(String subnet) {
        this.subnetName = subnet;
    }

    /**
     * Getter for node interface name
     * <p>
     *
     * @return String node interface name
     */
    public String getNodeInterfaceName() {
        return nodeInterfaceName;
    }

    /**
     * Setter for node interface name
     * <p>
     *
     * @param nodeInterfaceName String
     */
    public void setNodeInterfaceName(String nodeInterfaceName) {
        this.nodeInterfaceName = nodeInterfaceName;
    }

    /**
     * Getter for subnet which is interface connected to
     * <p>
     * @return SubNetwork
     */
    public String getSubnet() {
        return subnetName;
    }

    public void setSubnet(String subnet) {
        this.subnetName = subnet;
    }
    
    public void setFullDuplex(boolean fullDuplex) {
        this.isFullDuplex = fullDuplex;
    }
    
    public boolean isFullDuplex() {
        return this.isFullDuplex;
    }

    public LambdaLinkEndPoint getLambdaLinkEndpoint() {
        return lambdaLinkEndpoint;
    }

    public void setLambdaLinkEndpoint(LambdaLinkEndPoint lambdaLinkEndpoint) {
        this.lambdaLinkEndpoint = lambdaLinkEndpoint;
    }

    public EndpointNetworkNode getParentNode() {
        return parentNode;
    }

    public void setParentNode(EndpointNetworkNode parentNode) {
        this.parentNode = parentNode;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EndpointNodeInterface that = (EndpointNodeInterface) o;

        if (!uuid.equals(that.uuid)) return false;

        return true;
    }

    public boolean deepEquals(EndpointNodeInterface that) {
        return deepEquals(that, false);
    }
    public boolean deepEquals(EndpointNodeInterface that, boolean unique) {
        if (that == null) return false;
        if (that == this) return (! unique);
        
        if (getClass() != that.getClass()) return false;
        
        final EndpointNodeInterface other = (EndpointNodeInterface) that;
        if (!Objects.equals(this.nodeInterfaceName, other.nodeInterfaceName)) return false;
        if (!Objects.equals(this.ipAddress, other.ipAddress)) return false;
        if (!Objects.equals(this.netMask, other.netMask)) return false;
        if (Double.doubleToLongBits(this.bandwidth) != Double.doubleToLongBits(other.bandwidth)) return false;
        if (!Objects.equals(this.subnetName, other.subnetName)) return false;
        if (!Objects.equals(this.parentNode, other.parentNode)) return false;
        if (!Objects.equals(this.uuid, other.uuid)) return false;
        if (this.isFullDuplex != other.isFullDuplex) return false;

        if (unique) {
            if (this.parentNode == that.parentNode) return false;
        }
        
        return true;
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public String toString() {
        return "EndpointNodeInterface " + this.nodeInterfaceName + "/" + this.ipAddress + "/" + this.netMask;
    }

    @Override
    public boolean loadConfig(JSONObject configuration) throws IllegalArgumentException {
        boolean updated = false;
        
        String tmpNodeInterfaceName = getNodeInterfaceName();
        String tmpIpAddress = getIpAddress();
        String tmpNetMask = getNetMask();
        double tmpBandwidth = getBandwidth();
        String tmpSubnetName = getSubnet();
        String tmpUuid = getUuid();
        boolean tmpFullDuplex = isFullDuplex();
        
        try {
            tmpNodeInterfaceName = configuration.getString(ConfigKeyIfName);
            tmpIpAddress = configuration.getString(ConfigKeyAddress);
            tmpNetMask = configuration.getString(ConfigKeyNetmask);
            tmpBandwidth = configuration.getDouble(ConfigKeyBandwidth);
            tmpSubnetName = configuration.getString(ConfigKeySubnetName);
            tmpFullDuplex = configuration.getBoolean(ConfigKeyDuplex);
            tmpUuid = configuration.getString(ConfigKeyUuid);
        } catch (JSONException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Given config object does not contain all keys. Compare keys with defaultConfig().");
        }
        
        synchronized (this) {
            updated |= !tmpNodeInterfaceName.equals(getNodeInterfaceName());
            updated |= tmpIpAddress.equals(getIpAddress());
            updated |= tmpNetMask.equals(getNetMask());
            updated |= tmpBandwidth == getBandwidth();
            updated |= tmpSubnetName.equals(getSubnet());
            updated |= tmpFullDuplex == isFullDuplex();
            updated |= !tmpUuid.equals(getUuid());

            setNodeInterfaceName(tmpNodeInterfaceName);
            setIpAddress(tmpIpAddress);
            setNetMask(tmpNetMask);
            setBandwidth(tmpBandwidth);
            setSubnet(tmpSubnetName);
            setFullDuplex(tmpFullDuplex);
            setUuid(tmpUuid);
        }
        
        return updated;
    }

    @Override
    public JSONObject activeConfig() throws JSONException {
        JSONObject root = new JSONObject();
        root.put(ConfigKeyIfName, getNodeInterfaceName());
        root.put(ConfigKeyAddress, getIpAddress());
        root.put(ConfigKeyNetmask, getNetMask());
        root.put(ConfigKeyDuplex, isFullDuplex());
        root.put(ConfigKeyBandwidth, getBandwidth());
        root.put(ConfigKeySubnetName, getSubnet());
        root.put(ConfigKeyUuid, getUuid());
        return root;
    }

    @Override
    public JSONObject defaultConfig() throws JSONException {
        JSONObject root = new JSONObject();
        root.put(ConfigKeyIfName, "eth0");
        root.put(ConfigKeyAddress, "169.254.0.0");
        root.put(ConfigKeyNetmask, "255.255.0.0");
        root.put(ConfigKeyDuplex, true);
        root.put(ConfigKeyBandwidth, 1.0e10);
        root.put(ConfigKeySubnetName, "world");
        root.put(ConfigKeyUuid, UUID.randomUUID().toString());
        return root;
    }
}
