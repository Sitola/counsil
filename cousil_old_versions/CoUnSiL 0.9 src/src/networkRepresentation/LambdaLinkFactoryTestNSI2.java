package networkRepresentation;

import junit.framework.TestCase;
import org.junit.Test;
import utils.MyLogger;
import utils.TimeUtils;

/**
 * Created by IntelliJ IDEA.
 * User: xliska
 * Date: 16.1.2009
 * Time: 7:39:25
 */
public class LambdaLinkFactoryTestNSI2 extends TestCase {
    
    @Test
    public void testLambdaAllocation() {
        MyLogger.setup();

        LambdaLinkEndPoint fromLambdaLinkEndPoint = new LambdaLinkEndPointNSI2("urn:ogf:network:aist.go.jp:2013:topology", "urn:ogf:network:aist.go.jp:2013:bi-ps", false, "any");
        LambdaLinkEndPoint toLambdaLinkEndPoint = new LambdaLinkEndPointNSI2("urn:ogf:network:aist.go.jp:2013:topology", "urn:ogf:network:aist.go.jp:2013:bi-aist-jgn-x", false, "any");

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
