package networkRepresentation;

import core.Reportable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Representation of logical end-to-end links between EndpointNetworkNodes
 * <p/>
 * User: Pavel Troubil (pavel@ics.muni.cz)
 */
@ThreadSafe
public class LogicalNetworkLink extends GeneralNetworkLink implements Serializable, Reportable {
    static Logger logger = Logger.getLogger("NetworkRepresentation");

    @Override
    public JSONObject reportStatus() throws JSONException {
        JSONObject status = new JSONObject();
        
        status.put("fromInterface", getFromInterface().getUuid());
        status.put("toInterface", getToInterface().getUuid());
        status.put("latency", getLatency());
        status.put("status", isActive() ? "active" : "down");
        
        return status;
    }

    class LatencyHistory implements Serializable {
        double memo[] = new double[3];
        int currentId = 0;
        double currentLatency;

        double tolerance;
        final double TOLERANCE_MINIMUM = 10;

        LatencyHistory() {
            currentLatency = DEFAULT_LINK_LATENCY;
            tolerance = 0;
            memo[0] = 0;
            memo[1] = 0;
            memo[2] = 0;
        }

        void add(double latency) {
            memo[currentId] = latency;
            currentId++;
            if (currentId >= 3) currentId = 0;
        }

        void set(double latency) {
            currentLatency = latency;
            tolerance = (latency/2 < TOLERANCE_MINIMUM ? TOLERANCE_MINIMUM : latency/2);
            for (int i = 0; i < 3; i++) {
                memo[i] = latency;
            }
        }

        boolean exceedsTolerance() {
            for (int i = 0; i < 3; i++) {
                if (memo[i] >= currentLatency+tolerance || memo[i] <= currentLatency-tolerance) return true;
            }
            return false;
        }

        double getAverage() {
            return (memo[0] + memo[1] + memo[2]) / 3;
        }
    }

    LambdaLink lambda = null;

    public static final double DEFAULT_LINK_LATENCY = 150;

    private double latency = DEFAULT_LINK_LATENCY;
    private final LatencyHistory latencyHistory = new LatencyHistory();

    private EndpointNodeInterface fromInterface = null;
    private EndpointNodeInterface toInterface = null;
    private final CopyOnWriteArraySet<PhysicalNetworkLink> physicalLinksOnThePath = new CopyOnWriteArraySet<>();

    private int hashCode = 01;
    
    @Deprecated
    transient public int variableIndex;

    public LogicalNetworkLink() {
        super();
        this.variableIndex = -1;
    }

    public void setParameters(double capacity, double latency,
            EndpointNetworkNode fromNode,
            EndpointNodeInterface fromInterface,
            EndpointNetworkNode toNode,
            EndpointNodeInterface toInterface) {
        super.setParameters(capacity, fromNode, toNode);
        this.latency = latency;
        this.fromInterface = fromInterface;
        this.toInterface = toInterface;

        // TODO: what the heck? Verify
        hashCode = calculateHashCode();
    }

    public double getLatency() {
        return latency;
    }

    public void setLatency(double latency) {
        this.latency = latency;
        latencyHistory.set(latency);
    }

    /**
     * Log the currently measured latency
     * @param latency Measured latency
     * @return True if the measured value exceeds tolerance and the link should be updated
     */
    public boolean logLatency(double latency) {
        latencyHistory.add(latency);
        return latencyHistory.exceedsTolerance();
    }

    public double adjustLatencyToHistory() {
        this.latency = latencyHistory.getAverage();
        latencyHistory.set(this.latency);
        return latency;
    }

    @Override
    public EndpointNetworkNode getFromNode() {
        return (EndpointNetworkNode) super.getFromNode();
    }

    @Override
    public void setFromNode(GeneralNetworkNode fromNode) {
        if (! (fromNode instanceof EndpointNetworkNode) ) throw new IllegalArgumentException("From node of logical network link must be EndpointNetworkNode! (was "+fromNode.getClass().getSimpleName()+": "+fromNode+")");
        super.setFromNode(fromNode);
    }

    public EndpointNodeInterface getFromInterface() {
        return fromInterface;
    }

    public void setFromInterface(EndpointNodeInterface fromInterface) {
        super.setFromNode(fromInterface.getParentNode());
        this.fromInterface = fromInterface;
    }

    @Override
    public EndpointNetworkNode getToNode() {
        return (EndpointNetworkNode) super.getToNode();
    }

    @Override
    public void setToNode(GeneralNetworkNode toNode) {
        if (! (toNode instanceof EndpointNetworkNode) ) throw new IllegalArgumentException("To node of logical network link must be EndpointNetworkNode! (was "+toNode.getClass().getSimpleName()+": "+toNode+")");
        super.setToNode(toNode);
    }

    public EndpointNodeInterface getToInterface() {
        return toInterface;
    }

    public void setToInterface(EndpointNodeInterface toInterface) {
        this.toInterface = toInterface;
    }

    @Override
    public String toString() {
        return "Logical link " + fromInterface + " --> " + toInterface + " @" + this.getLatency() + " ms (" + (isActive() ? "Active" : "Down") + ")";
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

//        // is there are no lambdas along the path, the link is understood to be allocated permanently
//        if (this.physicalLinksOnThePath.isEmpty()) {
//            return true;
//        }
//        for (PhysicalNetworkLink physicalLink : physicalLinksOnThePath) {
//            if (!physicalLink.isLinkAllocated()) {
//                return false;
//            }
//        }
//        return true;
    }

    void associateLambda(LambdaLink lambdaLink) {
        this.lambda = lambdaLink;
    }

    public LambdaLink getLambda() {
        return lambda;
    }

    void addPhysicalLink(PhysicalNetworkLink link) {
        assert link != null;
        this.physicalLinksOnThePath.add(link);
    }

    void removePhysicalLink(PhysicalNetworkLink link) {
        assert link != null;
        this.physicalLinksOnThePath.remove(link);
    }

    public HashSet<PhysicalNetworkLink> getPhysicalLinksOnThePath() {
        return new HashSet<>(this.physicalLinksOnThePath);
    }

    void setPhysicalLinksOnThePath(Set<PhysicalNetworkLink> physicalLinks) {
        assert physicalLinks != null;

        this.physicalLinksOnThePath.clear();
        this.physicalLinksOnThePath.addAll(physicalLinks);
    }

    public ArrayList<LambdaLink> getAssociatedLambdas() {
        ArrayList<LambdaLink> lambdas = new ArrayList<>();
        /*
        for (PhysicalNetworkLink physical : physicalLinksOnThePath) {
            if (physical.getLambda() != null) {
                lambdas.add(physical.getLambda());
            }
        } */
        if (getLambda() != null)
            lambdas.add(getLambda());
        return lambdas;
    }

    void removeAllPhysicalLinks() {
        this.physicalLinksOnThePath.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LogicalNetworkLink that = (LogicalNetworkLink) o;

        if (true) return this.hashCode == that.hashCode;
        
        // TODO: to remove - none of the values should be null
        if (getFromInterface() == null || that.getFromInterface() == null || getToInterface() == null || that.getToInterface() == null) {
            Logger.getLogger(this.getClass()).log(Priority.ERROR, "LogicalNetworkLink in errorneous state - some member is null");
            throw new NullPointerException("Interface or parrent node null during equality test of LogicalNetworkLinks: " + this + "  and  " + that);
        }

        if (fromInterface == null && that.fromInterface == null && toInterface == null && that.toInterface == null) return true;
        if ((fromInterface == null && that.fromInterface != null) || (fromInterface != null && that.fromInterface == null)) return false;
        if ((toInterface == null && that.toInterface != null) || (toInterface != null && that.toInterface == null)) return false;

        if (! getFromInterface().equals(that.getFromInterface())) return false;
        if (! getToInterface().equals(that.getToInterface())) return false;
        if (! getFromNode().equals(that.getFromNode())) return false;
        if (! getToNode().equals(that.getToNode())) return false;

        return true;
    }

    public boolean deepEquals(LogicalNetworkLink o) {
        return deepEquals(o, false);
    }

    public boolean deepEquals(LogicalNetworkLink o, boolean unique) {
        if (this == o) return (!unique);
        if (o == null || getClass() != o.getClass()) return false;

        LogicalNetworkLink that = (LogicalNetworkLink) o;

        if (! super.deepEquals(that)) return false;

        // TODO: to remove - none of the values should be null
        if (getFromInterface() == null || that.getFromInterface() == null || getToInterface() == null || that.getToInterface() == null) {
            Logger.getLogger(this.getClass()).log(Priority.ERROR, "LogicalNetworkLink in errorneous state - some member is null");
            throw new NullPointerException("Interface or parrent node null during equality test of LogicalNetworkLinks");
        }

        if (fromInterface == null && that.fromInterface == null && toInterface == null && that.toInterface == null) return true;
        if ((fromInterface == null && that.fromInterface != null) || (fromInterface != null && that.fromInterface == null)) return false;
        if ((toInterface == null && that.toInterface != null) || (toInterface != null && that.toInterface == null)) return false;

        // Interfaces cascade to their parrent nodes, hence deepEquals on from/to nodes is redundant (and GeneralNetworkLink checks them anyway)
        if (unique) {
            if (getFromInterface() == that.getFromInterface()) return false;
            if (getToInterface()   == that.getToInterface()  ) return false;
        }

        if (! getFromInterface().deepEquals(that.getFromInterface(), unique)) return false;
        if (! getToInterface().deepEquals(that.getToInterface(), unique)) return false;

        if (this.physicalLinksOnThePath.size() != that.physicalLinksOnThePath.size()) return false;
        for (PhysicalNetworkLink thisLink : this.physicalLinksOnThePath) {
            boolean found = false;
            for (PhysicalNetworkLink thatLink : that.physicalLinksOnThePath) {
                if (thisLink.equals(thatLink)) {
                    if (found) throw new IllegalStateException("Physical link " + thisLink + " matches multiple physical links in the corresponding logical link!");
                    if (unique && thisLink == thatLink) return false;
                    if (! thisLink.deepEquals(thatLink, unique)) return false;
                    found = true;
                }
            }
        }

        for (PhysicalNetworkLink thatLink : that.physicalLinksOnThePath) {
            boolean found = false;
            for (PhysicalNetworkLink thisLink : this.physicalLinksOnThePath) {
                if (thatLink.equals(thisLink)) {
                   if (found) throw new IllegalStateException("Physical link " + thisLink + " matches multiple physical links in the corresponding logical link!");
                   if (unique && thisLink == thatLink) return false;
                   if (! thatLink.deepEquals(thisLink, unique)) return false;
                   found = true;
                }
            }
        }

        return true;
    }

    public int calculateHashCode() {
        
        int result = 0;

        try {
            result = getFromNode().hashCode();
            result = 31 * result + getToNode().hashCode();
            result = 39 * result + getFromInterface().hashCode();
            result = 37 * result + getToInterface().hashCode();
        } catch (NullPointerException e) {
            Logger.getLogger(this.getClass()).log(Priority.ERROR, "LogicalNetworkLink in errorneous state - some member is null");
            throw new NullPointerException("Interface or parrent node null during hash code of LogicalNetworkLinks: " + this);
        }
        return result;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
