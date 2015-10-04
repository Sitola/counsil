package networkRepresentation;

/**
 * Created with IntelliJ IDEA.
 * User: martin
 * Date: 9.9.13
 * Time: 14:21
 * To change this template use File | Settings | File Templates.
 */
public class LambdaLinkEndPointNSI1 extends LambdaLinkEndPoint {
    private String networkId;
    private String localId;

    public LambdaLinkEndPointNSI1() {
        super(false, null);
        networkId = null;
        localId = null;
    }

    public LambdaLinkEndPointNSI1(String networkId, String localId, boolean lambdaLinkEndpointTagged, String lambdaLinkEndpointVlan) {
        super(lambdaLinkEndpointTagged, lambdaLinkEndpointVlan);
        if (networkId == null || localId == null)
            throw new IllegalArgumentException();
        this.networkId = networkId;
        this.localId = localId;
    }

    @Override
    public String toString() {
        return "" + this.getLambdaLinkNetworkId() + " " + this.getLambdaLinkLocalId() ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LambdaLinkEndPointNSI1 that = (LambdaLinkEndPointNSI1) o;

        return this.getLambdaLinkNetworkId().equals(that.getLambdaLinkNetworkId()) &&
                this.getLambdaLinkLocalId().equals(that.getLambdaLinkLocalId());
    }

    public String getLambdaLinkNetworkId() {
        return networkId;
    }

    public void setLambdaLinkNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public String getLambdaLinkLocalId() {
        return localId;
    }

    public void setLambdaLinkLocalId(String localId) {
        this.localId = localId;
    }
}
