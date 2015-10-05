package networkRepresentation;

/**
 * Created with IntelliJ IDEA.
 * User: martin
 * Date: 9.9.13
 * Time: 14:21
 * To change this template use File | Settings | File Templates.
 */
public class LambdaLinkEndPointOSCARS extends LambdaLinkEndPoint {
    private String lambdaLinkEndpoint;
    private String lambdaLinkEndpointIDC;

    public LambdaLinkEndPointOSCARS() {
        super(false, null);
        this.lambdaLinkEndpoint = null;
        this.lambdaLinkEndpointIDC = null;
    }

    public LambdaLinkEndPointOSCARS(String lambdaLinkEndpoint, String lambdaLinkEndpointIDC, boolean lambdaLinkEndpointTagged, String lambdaLinkEndpointVlan) {
        super(lambdaLinkEndpointTagged, lambdaLinkEndpointVlan);
        this.lambdaLinkEndpoint = lambdaLinkEndpoint;
        this.lambdaLinkEndpointIDC = lambdaLinkEndpointIDC;
    }

    @Override
    public String toString() {
        return "" + this.getLambdaLinkEndpoint();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LambdaLinkEndPointOSCARS that = (LambdaLinkEndPointOSCARS) o;

        return this.getLambdaLinkEndpoint().equals(that.getLambdaLinkEndpoint());

    }

    public String getLambdaLinkEndpoint() {
        return lambdaLinkEndpoint;
    }

    public void setLambdaLinkEndpoint(String lambdaLinkEndpoint) {
        this.lambdaLinkEndpoint = lambdaLinkEndpoint;
    }

    public String getLambdaLinkEndpointIDC() {
        return lambdaLinkEndpointIDC;
    }

    public void setLambdaLinkEndpointIDC(String lambdaLinkEndpointIDC) {
        this.lambdaLinkEndpointIDC = lambdaLinkEndpointIDC;
    }
}