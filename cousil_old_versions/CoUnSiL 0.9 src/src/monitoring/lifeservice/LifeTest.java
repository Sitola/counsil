package monitoring.lifeservice;

import junit.framework.TestCase;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * JUnit test for Life :)
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 23.10.2007
 * Time: 17:56:49
 */
public class LifeTest extends TestCase {

    @Test
    public void testLocalTCPLife() {
        LifeServerTCP lifeServerTCP = new LifeServerTCP();
        lifeServerTCP.start();

        long rtt = -1;
        try {
            rtt = LifeClient.testAliveTCP(InetAddress.getLocalHost(), 500);
        } catch (UnknownHostException e) {
            assertTrue("Failed to resolve localhost!", false);
        }

        System.out.println("TCP RTT = " + rtt);
        assertTrue("Localhost reachability test should always succeed (unless you gave really funny firewall)!", rtt != -1);

        lifeServerTCP.stop();
    }

    @Test
    public void testLocalUDPLife() {
        LifeServerUDP lifeServerUDP = new LifeServerUDP();
        lifeServerUDP.start();

        long rtt = -1;
        try {
            rtt = LifeClient.testAliveUDP(InetAddress.getLocalHost(), 500);
        } catch (UnknownHostException e) {
            assertTrue("Failed to resolve localhost!", false);
        }

        System.out.println("UDP RTT = " + rtt);
        assertTrue("Localhost reachability test should always succeed (unless you gave really funny firewall)!", rtt != -1);

        lifeServerUDP.stop();
    }

}
