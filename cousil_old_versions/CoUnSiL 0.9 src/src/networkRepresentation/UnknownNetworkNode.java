package networkRepresentation;

import java.io.Serializable;

/**
 * Representation of a subnetwork with unknown internal topology as a network node. The UnknownNetworkNode class supports serializing the instance into a XML to be sent over JXTA pipe.
 * <p/>
 * User: Pavel Troubil (pavel@ics.muni.cz)
 */
public class UnknownNetworkNode extends GeneralNetworkNode implements Serializable {


    /**
     * This is an empty JavaBean constructor in order to support XMLEncoder and XMLDecoder
     */
    public UnknownNetworkNode() {
        super(GeneralNetworkNode.NODE_TYPE_UNKNOWN_NETWORK);
    }

    /**
     * UnknownNetworkNode constructor.
     * <p/>
     *
     * @param nodeName         name of the node
     */
    public UnknownNetworkNode(String nodeName) {
        super(nodeName, GeneralNetworkNode.NODE_TYPE_UNKNOWN_NETWORK);
    }

    @Override
    public String toString() {
        return "" + this.getNodeName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !getClass().isInstance(o)) return false;

        UnknownNetworkNode that = (UnknownNetworkNode) o;

        return super.equals(o);
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
