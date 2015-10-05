package networkRepresentation;

import java.util.ArrayList;
import java.io.Serializable;

/**
 * Representation of lambda aspect of NetworkLink implementing Internet2 DCN OSCARS protocol
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 3.10.2007
 * Time: 18:31:30
 */
public class LambdaLink implements Serializable {
    public enum LambdaLinkType {
        NSI1,
        NSI2,
        OSCARS,
    }

    public enum Status {
        UNKNOWN,
        PENDING,
        ACTIVE,
        FAILED,
        FINISHED,
        CANCELLED
    }

    LambdaLinkEndPoint fromLambdaLinkEndPoint;
    LambdaLinkEndPoint toLambdaLinkEndPoint;
    double bandwidth;
    long lastAllocationAttempt;
    boolean toAllocate;
    boolean isAllocated;
    public Status status;
    ArrayList<LogicalNetworkLink> associatedNetworkLinks;

    String lambdaReservationId;

    public LambdaLink() {
        this.fromLambdaLinkEndPoint = null;
        this.toLambdaLinkEndPoint = null;
        this.bandwidth = 0;
        this.lastAllocationAttempt = 0;
        this.toAllocate = false;
        this.isAllocated = false;
        this.status = Status.UNKNOWN;
        this.associatedNetworkLinks = null;

        this.lambdaReservationId = null;
    }

    public LambdaLink(LambdaLinkEndPoint fromLambdaLinkEndPoint, LambdaLinkEndPoint toLambdaLinkEndPoint, double bandwidth) {
        if (fromLambdaLinkEndPoint == null || toLambdaLinkEndPoint == null)
            throw new NullPointerException();
        if (fromLambdaLinkEndPoint.getClass() != toLambdaLinkEndPoint.getClass())
            throw new IllegalArgumentException("Lambda links of different types");
        this.toAllocate = false;
        this.isAllocated = false;
        this.status = Status.UNKNOWN;
        this.associatedNetworkLinks = new ArrayList<LogicalNetworkLink>();
        this.lastAllocationAttempt = 0;

        this.fromLambdaLinkEndPoint = fromLambdaLinkEndPoint;
        this.toLambdaLinkEndPoint = toLambdaLinkEndPoint;
        this.bandwidth = bandwidth;
    }

    Boolean isActive() {
        return status == Status.ACTIVE;
    }

    void addNetworkLink(LogicalNetworkLink networkLink) {
        associatedNetworkLinks.add(networkLink);
    }

    void removeNetworkLink(LogicalNetworkLink networkLink) {
        assert(associatedNetworkLinks.contains(networkLink));
        associatedNetworkLinks.remove(networkLink);
    }

    public double getBandwidth() {
        return bandwidth;
    }

    public LambdaLinkType getType() {
        if (fromLambdaLinkEndPoint == null)
            throw new IllegalStateException();

        if (fromLambdaLinkEndPoint instanceof LambdaLinkEndPointNSI1)
            return LambdaLinkType.NSI1;
        else if (fromLambdaLinkEndPoint instanceof LambdaLinkEndPointNSI2)
            return LambdaLinkType.NSI2;
        else if (fromLambdaLinkEndPoint instanceof LambdaLinkEndPointOSCARS)
            return LambdaLinkType.OSCARS;
        else
            throw new IllegalStateException();
    }

    public void setBandwidth(double bandwidth) {
        this.bandwidth = bandwidth;
    }

    public LambdaLinkEndPoint getFromLambdaLinkEndPoint() {
        return fromLambdaLinkEndPoint;
    }

    public void setFromLambdaLinkEndPoint(LambdaLinkEndPoint fromLambdaLinkEndPoint) {
        this.fromLambdaLinkEndPoint = fromLambdaLinkEndPoint;
    }

    public LambdaLinkEndPoint getToLambdaLinkEndPoint() {
        return toLambdaLinkEndPoint;
    }

    public void setToLambdaLinkEndPoint(LambdaLinkEndPoint toLambdaLinkEndPoint) {
        this.toLambdaLinkEndPoint = toLambdaLinkEndPoint;
    }

    synchronized public long getLastAllocationAttempt() {
        return lastAllocationAttempt;
    }

    synchronized public void setLastAllocationAttempt(long lastAllocationAttempt) {
        this.lastAllocationAttempt = lastAllocationAttempt;
    }

    public boolean isAllocated() {
        return isAllocated;
    }

    public void setAllocated(boolean allocated) {
        isAllocated = allocated;
    }

    public ArrayList<LogicalNetworkLink> getAssociatedNetworkLinks() {
        return associatedNetworkLinks;
    }

    public void setAssociatedNetworkLinks(ArrayList<LogicalNetworkLink> associatedNetworkLinks) {
        this.associatedNetworkLinks = associatedNetworkLinks;
    }

    public String getLambdaReservationId() {
        return lambdaReservationId;
    }

    public void setLambdaReservationId(String lambdaReservationId) {
        this.lambdaReservationId = lambdaReservationId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isToAllocate() {
        return toAllocate;
    }

    public void setToAllocate(boolean toAllocate) {
        this.toAllocate = toAllocate;
    }

    @Override
    public String toString() {
        LambdaLinkEndPoint fromEP =  this.getFromLambdaLinkEndPoint();
        LambdaLinkEndPoint toEP =  this.getToLambdaLinkEndPoint();
        return "" + fromEP + (fromEP.isLambdaLinkEndpointTagged() ? " [vlan" + fromEP.getLambdaLinkEndpointVlan() + "]" : "")
                + "  --->  "
                + toEP + (toEP.isLambdaLinkEndpointTagged() ? " [vlan" + toEP.getLambdaLinkEndpointVlan() + "]" : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LambdaLink that = (LambdaLink) o;

        if (this.getBandwidth() != that.getBandwidth()) return false;

        if (this.getFromLambdaLinkEndPoint().equals(that.getFromLambdaLinkEndPoint()) && this.getToLambdaLinkEndPoint().equals(that.getToLambdaLinkEndPoint())) return true;
        if (this.getFromLambdaLinkEndPoint().equals(that.getToLambdaLinkEndPoint()) && this.getToLambdaLinkEndPoint().equals(that.getFromLambdaLinkEndPoint())) return true;

        return false;
    }
}
