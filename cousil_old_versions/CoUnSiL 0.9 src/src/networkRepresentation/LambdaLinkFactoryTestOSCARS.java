package networkRepresentation;

import junit.framework.TestCase;
import org.junit.Test;
import utils.TimeUtils;
import utils.MyLogger;

/**
 * Created by IntelliJ IDEA.
 * User: xliska
 * Date: 16.1.2009
 * Time: 7:39:25
 */
public class LambdaLinkFactoryTestOSCARS extends TestCase {
    
    @Test
    public void testLambdaAllocation() {
        MyLogger.setup();

        LambdaLinkEndPoint fromLambdaLinkEndPoint = new LambdaLinkEndPointOSCARS("test-newy.dcn.internet2.edu", "https://test-idc.internet2.edu:8443/axis2/services/OSCARS/", false, "any");
        LambdaLinkEndPoint toLambdaLinkEndPoint = new LambdaLinkEndPointOSCARS("test-chic.dcn.internet2.edu", "https://test-idc.internet2.edu:8443/axis2/services/OSCARS/", false, "any");

        // LambdaLinkEndPoint fromLambdaLinkEndPoint = new LambdaLinkEndPoint("urn:ogf:network:domain=dcn.internet2.edu:node=CHIC:port=S28415:link=10.100.100.33", "https://ndb3-blmt.abilene.ucaid.edu:8443/axis2/services/OSCARS/", true, "any");
        // LambdaLinkEndPoint toLambdaLinkEndPoint = new LambdaLinkEndPoint("urn:ogf:network:domain=tamu.edu:node=tamu-sw1:port=1-0-24:link=*", "https://ndb3-blmt.abilene.ucaid.edu:8443/axis2/services/OSCARS/", false, "any");

        // LambdaLinkEndPoint fromLambdaLinkEndPoint = new LambdaLinkEndPoint("urn:ogf:network:domain=tamu.edu:node=tamu-sw1:port=1-0-24:link=*", "https://128.194.197.114:8443/axis2/services/OSCARS/", false, "any");
        // LambdaLinkEndPoint toLambdaLinkEndPoint = new LambdaLinkEndPoint("urn:ogf:network:domain=dcn.internet2.edu:node=CHIC:port=S28415:link=10.100.100.33", "https://128.194.197.114:8443/axis2/services/OSCARS/", true, "any");

        LambdaLink lambdaLink = new LambdaLink(fromLambdaLinkEndPoint, toLambdaLinkEndPoint, 1000);

        LambdaLinkFactory factory = new LambdaLinkFactory(lambdaLink);

        factory.allocate(lambdaLink);

        while (!(lambdaLink.status.equals("ACTIVE") || lambdaLink.status.equals("FINISHED"))) {
            factory.query(lambdaLink);
            System.out.println(lambdaLink.status);
            TimeUtils.sleepFor(5000);
        }

        System.out.println("");
        System.out.println("Final lambda status: " + lambdaLink.status);

        TimeUtils.sleepFor(60000);

        factory.modify(lambdaLink);
    }
}
