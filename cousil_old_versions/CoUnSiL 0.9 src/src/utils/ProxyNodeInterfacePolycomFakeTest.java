package utils;

import junit.framework.TestCase;
import static utils.TimeUtils.sleepFor;

/**
 * JUnit test for ProxyNodeInterfacePolycomFake
 * <p/>
 * 
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 27.4.2009
 * Time: 19:48:08
 */
public class ProxyNodeInterfacePolycomFakeTest extends TestCase {
    String targetHost = "polycom2.ics.muni.cz";
    String targetIP = "147.251.3.53";
    int targetPort = 24;
    String password = "";

    String reachableIP = "147.251.3.1";
    String unreachableIP = "192.168.1.1";
    String connectIP = "147.251.54.17";

    public void testPolycomConnection() {
        String output;
        Double rtt;

        System.out.println("---------------------------------------------------------");
        System.out.println("Testing Polycom connection using raw interface");
        System.out.println("---------------------------------------------------------");

        System.out.println("Creating new connection instance to a Polycom device");
        ProxyNodeInterfacePolycomFake polycom = new ProxyNodeInterfacePolycomFake(targetHost, targetPort, password);

        System.out.println("Connecting to the device");
        polycom.connect();

        System.out.println("Pinging default gateway");
        output = polycom.send("ping " + reachableIP);
        System.out.println("ping output:\n" + output);

        System.out.println("Pinging non-existant host");
        output = polycom.send("ping " + unreachableIP);
        System.out.println("ping output:\n" + output);

        System.out.println("Pinging default gateway using ping method");
        rtt = polycom.sendPing(reachableIP);
        System.out.println("ping rtt: " + rtt + "\n");

        System.out.println("Pinging non-existant host using ping method");
        rtt = polycom.sendPing(unreachableIP);
        System.out.println("ping rtt: " + rtt + "\n");


        System.out.println("Sample call");
        output = polycom.send("dial manual \"1920\" \"" + connectIP + "\"");
        System.out.println("call output:\n" + output);

        sleepFor (10000);

        System.out.println("Hanging up");
        output = polycom.send("hangup all");
        System.out.println("call output:\n" + output);

        System.out.println("Disconnecting from the device");
        polycom.disconnect();

        System.out.println("Finished communication with the Polycom device\n");
    }


    public void testPolycomConnection2() {
        String output;
        Double rtt;

        System.out.println("---------------------------------------------------------");
        System.out.println("Testing Polycom connection using abstract interface");
        System.out.println("---------------------------------------------------------");

        System.out.println("Creating new connection instance to a Polycom device");
        ProxyNodeConnection connection = new ProxyNodeConnection(new ProxyNodeInterfacePolycomFake(targetHost, targetPort, password));

        System.out.println("Connecting to the device");
        connection.start();

        System.out.println("Pinging default gateway using ping method");
        try {
            rtt = connection.sendPing(reachableIP);
            System.out.println("ping rtt: " + rtt + "\n");
        } catch (ProxyNodeConnection.ProxyNodeCommandFailed proxyNodeCommandFailed) {
            proxyNodeCommandFailed.printStackTrace();
        }

        System.out.println("Pinging non-existant host using ping method");
        try {
            rtt = connection.sendPing(unreachableIP);
            System.out.println("ping rtt: " + rtt + "\n");
        } catch (ProxyNodeConnection.ProxyNodeCommandFailed proxyNodeCommandFailed) {
            proxyNodeCommandFailed.printStackTrace();
        }


        System.out.println("Sample call");
        try {
            output = connection.send("dial manual \"1920\" \"" + connectIP + "\"");
            System.out.println("call output:\n" + output);
        } catch (ProxyNodeConnection.ProxyNodeCommandFailed proxyNodeCommandFailed) {
            proxyNodeCommandFailed.printStackTrace();
        }

        sleepFor (10000);

        System.out.println("Hanging up");
        try {
            output = connection.send("hangup all");
            System.out.println("call output:\n" + output);
        } catch (ProxyNodeConnection.ProxyNodeCommandFailed proxyNodeCommandFailed) {
            proxyNodeCommandFailed.printStackTrace();
        }

        System.out.println("Disconnecting from the device");
        connection.stop();

        System.out.println("Finished communication with the Polycom device");
    }

}

