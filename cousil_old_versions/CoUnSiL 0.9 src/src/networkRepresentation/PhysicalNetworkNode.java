package networkRepresentation;

import java.io.Serializable;

/**
 * Physical network node representation, used to maintain information about routers, switches, etc. The PhysicalNetworkNode
 * class supports serializing the instance into a XML to be sent over JXTA pipe.
 * <p/>
 * User: Pavel Troubil (pavel@ics.muni.cz)
 */
public class PhysicalNetworkNode extends GeneralNetworkNode implements Serializable {


    /**
     * This is an empty JavaBean constructor in order to support XMLEncoder and XMLDecoder
     */
    public PhysicalNetworkNode() {
        super(GeneralNetworkNode.NODE_TYPE_PHYSICAL);
    }

    /**
     * PhysicalNetworkNode constructor.
     * <p/>
     *
     * @param nodeName         name of the node
     */
    public PhysicalNetworkNode(String nodeName) {
        super(nodeName, GeneralNetworkNode.NODE_TYPE_PHYSICAL);
    }

    @Override
    public String toString() {
        return "" + this.getNodeName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PhysicalNetworkNode that = (PhysicalNetworkNode) o;

        return super.equals(that);

    }
    
    @Override
    public boolean deepEquals(GeneralNetworkNode node) {
        return super.deepEquals(node);
    }

    @Override
    public int hashCode() {
        return getUuid().hashCode();
    }

    @Override
    public boolean update(GeneralNetworkNode node) {
        throw new UnsupportedOperationException(this.getClass().getCanonicalName() + ": update is not supported yet.");
    }
}
