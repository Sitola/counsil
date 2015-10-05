package networkRepresentation;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: xliska
 * Date: 28.4.2009
 * Time: 18:51:16
 */
public abstract class LambdaLinkEndPoint implements Serializable {
    private boolean lambdaLinkEndpointTagged;
    private String lambdaLinkEndpointVlan;

    public LambdaLinkEndPoint() {
        this.lambdaLinkEndpointTagged = false;
        this.lambdaLinkEndpointVlan = "any";
    }

    public LambdaLinkEndPoint(boolean lambdaLinkEndpointTagged, String lambdaLinkEndpointVlan) {
        this.lambdaLinkEndpointTagged = lambdaLinkEndpointTagged;
        this.lambdaLinkEndpointVlan = lambdaLinkEndpointVlan;
    }

    public boolean isLambdaLinkEndpointTagged() {
        return lambdaLinkEndpointTagged;
    }

    public void setLambdaLinkEndpointTagged(boolean lambdaLinkEndpointTagged) {
        this.lambdaLinkEndpointTagged = lambdaLinkEndpointTagged;
    }

    public String getLambdaLinkEndpointVlan() {
        return lambdaLinkEndpointVlan;
    }

    public void setLambdaLinkEndpointVlan(String lambdaLinkEndpointVlan) {
        this.lambdaLinkEndpointVlan = lambdaLinkEndpointVlan;
    }
}
