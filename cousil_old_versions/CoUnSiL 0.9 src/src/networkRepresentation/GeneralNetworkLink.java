package networkRepresentation;

import java.io.Serializable;
import org.jgrapht.WeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

/**
 * Representation of the link between NetworkNodes and their NodeInterfaces.
 * <p/>
 * User: Pavel Troubil (pavel@ics.muni.cz)
 */
public abstract class GeneralNetworkLink extends DefaultWeightedEdge implements Serializable {

    double weight = WeightedGraph.DEFAULT_EDGE_WEIGHT; // this is actually a capacity of the link (which is sometimes pretty inconvenient)
    private double capacity = 0.0;
    GeneralNetworkNode fromNode = null;
    GeneralNetworkNode toNode = null;
    private boolean active = true;

    public GeneralNetworkLink() {
        super();
        this.active = true;
    }

    public GeneralNetworkLink(double capacity, GeneralNetworkNode fromNode, GeneralNetworkNode toNode) {
        super();
        this.capacity = capacity;
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.active = true;
    }
    
    public void setParameters(double capacity, GeneralNetworkNode fromNode, GeneralNetworkNode toNode) {
        this.capacity = capacity;
        this.fromNode = fromNode;
        this.toNode = toNode;
    }

    public double getCapacity() {
        return capacity;
    }

    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

    public GeneralNetworkNode getFromNode() {
        return fromNode;
    }

    public void setFromNode(GeneralNetworkNode fromNode) {
        this.fromNode = fromNode;
    }

    public GeneralNetworkNode getToNode() {
        return toNode;
    }

    public void setToNode(GeneralNetworkNode toNode) {
        this.toNode = toNode;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "" + this.getFromNode() + "  --->  " + this.getToNode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GeneralNetworkLink that = (GeneralNetworkLink) o;

        if (! fromNode.equals(that.fromNode)) return false;
        if (! toNode.equals(that.toNode)) return false;
        

        return true;
    }
    
    public boolean deepEquals(GeneralNetworkLink that) {
        return deepEquals(that, false);
    }
    
    public boolean deepEquals(GeneralNetworkLink that, boolean unique) {
        if (! equals(that)) return false;
        if (this == that) return false;
        
        if (unique) {
            if (getFromNode() == that.getFromNode()     ) return false;
            if (getToNode()   == that.getToNode()       ) return false;
        }
        
        if (! this.fromNode.deepEquals(that.fromNode, unique)) return false;
        if (! this.toNode.deepEquals(that.toNode, unique)) return false;
        
        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = fromNode.hashCode();
        result = 31 * result + toNode.hashCode();
        return result;
    }

}
