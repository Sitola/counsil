package networkRepresentation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.apache.log4j.Logger;

/**
 * Endpoints SubNetwork representation.
 * EndpointSubNetwork maintains information on endpoints connnected to a subnetwork via their interfaces.
 * <p/>
 * User: Milos Liska (xliska@fi.mui.cz), Pavel Troubil (pavel@ics.muni.cz)
 */
public class EndpointSubNetwork implements Serializable {
    static Logger logger = Logger.getLogger("NetworkRepresentation");

    String subNetName;
    private final CopyOnWriteArraySet<EndpointNodeInterface> nodeInterfaces = new CopyOnWriteArraySet<>();
    
    public static final String PublicSubnetName = "Public IP Internet";

    /**
     * JavaBean constructor
     */
    public EndpointSubNetwork() {
        this.subNetName = "";
    }

    /**
     * ReallyUseful(TM) constructor
     * <p/>
     *
     * @param name           subnet name (to identify it)
     * @param nodeInterfaces set of nodeInterfaces to add to it (maybe empty)
     */
    public EndpointSubNetwork(String name, CopyOnWriteArraySet<EndpointNodeInterface> nodeInterfaces) {
        this.subNetName = name;
        setSubNetworkNodeInterfaces(nodeInterfaces);
    }

    /**
     * Returns set of network interfaces belonging to this subnet
     * <p/>
     *
     * @return set of interfaces
     */
    public Set<EndpointNodeInterface> getSubNetworkNodeInterfaces() {
        return new HashSet<>(nodeInterfaces);
    }

    /**
     * Sets list of subnet interfaces to the given list. 
     * <p/>
     *
     * @param nodeInterfaces list of interfaces to set the subnet list to
     */
    public final void setSubNetworkNodeInterfaces(CopyOnWriteArraySet<EndpointNodeInterface> nodeInterfaces) {
        if (!CheckUnique(nodeInterfaces)) {
            throw new IllegalArgumentException("IP addresses of nodeInterface collection are not unique!");
        }
        
        this.nodeInterfaces.clear();
        this.nodeInterfaces.addAll(nodeInterfaces);
    }

    /**
     * Adds a list of interfaces to existing list for this subnet
     * <p/>
     *
     * @param nodeInterfaces list of interfaces to add
     */
    public void addSubNetworkNodeInterfaces(final CopyOnWriteArraySet<EndpointNodeInterface> nodeInterfaces) {
        if (!CheckUnique(nodeInterfaces)) {
            throw new IllegalArgumentException("IP addresses of nodeInterface collection are not unique!");
        }
        if (!CheckUnique(this.nodeInterfaces, nodeInterfaces)) {
            throw new IllegalArgumentException("IP addresses of at least one nodeInterface is already existing in the subnetwork!");
        }
        this.nodeInterfaces.addAll(nodeInterfaces);
    }

    /**
     * Adds a single interface to existing list for this subnet
     * <p/>
     *
     * @param nodeInterface an interface to add
     */
    public void addSubNetworkNodeInterface(final EndpointNodeInterface nodeInterface) {
        //noinspection CloneableClassInSecureContext
        if (nodeInterface == null) throw new NullPointerException("Sub-network interface cannot be null!");
        if (!CheckUnique(this.nodeInterfaces, new ArrayList<EndpointNodeInterface>() {
            {
                add(nodeInterface);
            }            
        })) {
            if (EndpointSubNetwork.logger.isTraceEnabled()) {
                EndpointSubNetwork.logger.trace("Already existing interfaces:");
                for (EndpointNodeInterface anInterface : nodeInterfaces) {
                    EndpointSubNetwork.logger.trace("   " + anInterface);
                }
            }
            throw new IllegalArgumentException("IP address " + nodeInterface.getIpAddress() + " already exists in the subnetwork!");
        }
        this.nodeInterfaces.add(nodeInterface);
    }

    /**
     * Removes a single interface from a list in this subnet
     *
     * @param nodeInterface an interface to remove
     */
    public void removeSubNetworkNodeInterface(final EndpointNodeInterface nodeInterface) {
        if (this.nodeInterfaces.contains(nodeInterface)) {
            this.nodeInterfaces.remove(nodeInterface);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EndpointSubNetwork that = (EndpointSubNetwork) o;

        return !(subNetName != null ? !subNetName.equals(that.subNetName) : that.subNetName != null);
    }
    
    public boolean deepEquals(EndpointSubNetwork that) {
        return deepEquals(that, false);
    }
    public boolean deepEquals(EndpointSubNetwork that, boolean unique) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;
        
        if (! this.subNetName.equals(that.subNetName)) return false;
        
        if (this.nodeInterfaces.size() != that.nodeInterfaces.size()) return false;
        for (EndpointNodeInterface thisIface : this.nodeInterfaces) {
            boolean found = false;
            for (EndpointNodeInterface thatIface : that.nodeInterfaces) {
                if (thisIface.equals(thatIface)) {
                    if (unique && thisIface == thatIface) return false;
                    if (! thisIface.deepEquals(thatIface, unique)) return false;
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
                    if (unique && thisIface == thatIface) return false;
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
        int hash = 5;
        hash = 59 * hash + (this.subNetName != null ? this.subNetName.hashCode() : 0);
        return hash;
    }

    /**
     * Checks whether interfaces inside an array have unique IP addresses
     * <p/>
     *
     * @param nodeInterfaces collection of interfaces
     * @return true iff all IP addresses are unique
     */
    private static boolean CheckUnique(Collection<EndpointNodeInterface> nodeInterfaces) {
        ArrayList<EndpointNodeInterface> a = new ArrayList<EndpointNodeInterface>(nodeInterfaces);
        if (a.isEmpty()) {
            return true;
        }
        for (int i = 0; i < a.size(); i++) {
            for (int j = 0; j < a.size(); j++) {
                if (i != j && a.get(i).getIpAddress().equals(a.get(j).getIpAddress())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks two arrays, whether they don't have duplicate IP addresses. Doesn't check the same
     * inside each of the array - for this purpose, see the same method with single parameter.
     * <p/>
     *
     * @param nodeInterfacesA a collection to compare
     * @param nodeInterfacesB a collection to compare
     * @return true iff IP addresses are unique under the condition stated above
     */
    private boolean CheckUnique(Collection<EndpointNodeInterface> nodeInterfacesA, Collection<EndpointNodeInterface> nodeInterfacesB) {
        ArrayList<EndpointNodeInterface> a = new ArrayList<EndpointNodeInterface>(nodeInterfacesA);
        ArrayList<EndpointNodeInterface> b = new ArrayList<EndpointNodeInterface>(nodeInterfacesB);
        if (a.isEmpty() || b.isEmpty()) {
            return true;
        }
        for (EndpointNodeInterface anA : a) {
            for (EndpointNodeInterface aB : b) {
                if (anA.getIpAddress().equals(aB.getIpAddress())) {
                    return false;
                }
            }
        }
        return true;
    }

    // JavaBean enforced setters/getters

    public String getSubNetName() {
        return subNetName;
    }

    public void setSubNetName(String subNetName) {
        this.subNetName = subNetName;
    }

}
