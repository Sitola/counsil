package networkRepresentation;

import java.io.Serializable;
import java.util.HashSet;

/**
 * Representation of known physical link between GeneralNetworkNodes
 * <p/>
 * User: Pavel Troubil (pavel@ics.muni.cz)
 */
public class PhysicalNetworkLink extends GeneralNetworkLink implements Serializable {

    String linkName = "";
    boolean fullDuplex = true;
    LambdaLink lambda = null;
    HashSet<LogicalNetworkLink> traversingLogicalLinks = new HashSet<LogicalNetworkLink>();
    EndpointNodeInterface nodeInterface = null; // since physical link can not connect two endpoint nodes, it can also have only one interface associated
    PhysicalNetworkLink backLink = null;
    public int variableIndex;

    public PhysicalNetworkLink() {
        super();

    }

    public PhysicalNetworkLink(String linkName, double capacity, GeneralNetworkNode fromNode, GeneralNetworkNode toNode, boolean fullDuplex) {
        this(linkName, capacity, fromNode, toNode, fullDuplex, null, null);
    }
    
    public PhysicalNetworkLink(String linkName, double capacity, GeneralNetworkNode fromNode, GeneralNetworkNode toNode, boolean fullDuplex, EndpointNodeInterface iface) {
        this(linkName, capacity, fromNode, toNode, fullDuplex, iface, null);
    }
        
    public PhysicalNetworkLink(String linkName, double capacity, GeneralNetworkNode fromNode, GeneralNetworkNode toNode, boolean fullDuplex, EndpointNodeInterface iface, PhysicalNetworkLink backLink) {
        super(capacity, fromNode, toNode);

        assert !(fromNode.getClass() == EndpointNetworkNode.class && toNode.getClass() == EndpointNetworkNode.class) : "Physical link connecting two endpoint nodes!";
        this.linkName = linkName;
        this.nodeInterface = iface;
        this.backLink = backLink;
    }
    
    public void setParameters(String linkName, double capacity, GeneralNetworkNode fromNode, GeneralNetworkNode toNode, boolean fullDuplex) {
        this.setParameters(linkName, capacity, fromNode, toNode, fullDuplex, null, null);
    }
    
    public void setParameters(String linkName, double capacity, GeneralNetworkNode fromNode, GeneralNetworkNode toNode, boolean fullDuplex, EndpointNodeInterface nodeInterface) {
        this.setParameters(linkName, capacity, fromNode, toNode, fullDuplex, nodeInterface, null);
    }

    public void setParameters(String linkName, double capacity, GeneralNetworkNode fromNode, GeneralNetworkNode toNode, boolean fullDuplex, EndpointNodeInterface iface, PhysicalNetworkLink backLink) {
        assert !(fromNode.getClass() == EndpointNetworkNode.class && toNode.getClass() == EndpointNetworkNode.class) : "Physical link connecting two endpoint nodes!";
        super.setParameters(capacity, fromNode, toNode);
        this.linkName = linkName;
        this.fullDuplex = fullDuplex;
        this.nodeInterface = iface;
        this.backLink = backLink;
    }

    @Override
    public String toString() {
        return "" + this.linkName + ": " + this.getFromNode().getNodeName() + "  --->  " + this.getToNode().getNodeName();
    }

    boolean isLambdaBased() {
        return this.lambda != null;
    }

    boolean isLinkAllocated() {
        // is there are no lambdas associated, the link is understood to be allocated permanently
        if (this.lambda != null && !this.lambda.isAllocated) {
            return false;
        }
        return true;
    }

    void associateLambda(LambdaLink lambdaLink) {
        this.lambda = lambdaLink;
    }

    boolean isFullDuplex() {
        return fullDuplex;
    }

    void setFullDuplex(boolean fullDuplex) {
        this.fullDuplex = fullDuplex;
    }

    LambdaLink getLambda() {
        return lambda;
    }

    void setLambda(LambdaLink lambda) {
        this.lambda = lambda;
    }

    EndpointNodeInterface getEndpointNodeInterface() {
        return nodeInterface;
    }

    public PhysicalNetworkLink getBackLink() {
        return backLink;
    }

    public void setBackLink(PhysicalNetworkLink backLink) {
        this.backLink = backLink;
    }
    
    void addTraversingLogicalLink(LogicalNetworkLink link) {
        this.traversingLogicalLinks.add(link);
    }
    
    void removeTraversingLogicalLink(LogicalNetworkLink link) {
        this.traversingLogicalLinks.remove(link);
    }
    
    void clearTraversingLogicalLinks() {
        this.traversingLogicalLinks.clear();
    }

    public HashSet<LogicalNetworkLink> getTraversingLogicalLinks() {
        return traversingLogicalLinks;
    }

    public void setTraversingLogicalLinks(HashSet<LogicalNetworkLink> traversingLogicalLinks) {
        this.traversingLogicalLinks = traversingLogicalLinks;
    }
    public String getLinkName() {
        return linkName;
    }

    public void setLinkName(String linkName) {
        this.linkName = linkName;
    }    

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PhysicalNetworkLink that = (PhysicalNetworkLink) o;

        if (!getFromNode().equals(that.getFromNode())) return false;
        if (!getToNode().equals(that.getToNode())) return false;
        if (!linkName.equals(that.linkName)) return false;

        return true;
    }
    
    @Override
    public boolean deepEquals(GeneralNetworkLink that) {
        return this.deepEquals(that, false);
    }
    
    @Override
    public boolean deepEquals(GeneralNetworkLink that, boolean unique) {
        if (unique && this == that) return false;
        return equals(that) && super.deepEquals(that, unique);
    }

    @Override
    public int hashCode() {
        int result;

        result = getFromNode().hashCode();
        result = 31 * result + getToNode().hashCode();
        return result;
    }

}
