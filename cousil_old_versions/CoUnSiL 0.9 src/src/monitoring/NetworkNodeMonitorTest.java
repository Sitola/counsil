package monitoring;

/**
 * Created by IntelliJ IDEA.
 * User: xsuchom1
 * Date: Oct 21, 2008
 * Time: 5:18:48 PM
 * To change this template use File | Settings | File Templates.
 */

import networkRepresentation.EndpointNetworkNode;
import networkRepresentation.EndpointNodeInterface;
import junit.framework.TestCase;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import utils.MyLogger;


public class NetworkNodeMonitorTest extends TestCase {
    static {
        MyLogger.setup();
    }
   private EndpointNetworkNode ntwNode = new EndpointNetworkNode();
   private NetworkNodeMonitor ntwNodeMonitor = new NetworkNodeMonitor();
   private EndpointNodeInterface nodeInterface = new EndpointNodeInterface();


    @Before
    public void create() {

    }

    @Test
    public void testMemory() {
        int usage = ntwNodeMonitor.getMemoryUsage(ntwNode);
        assertThat("-1: error code returned by nativ function ",usage, is(not(-1)));
        System.out.println("Memory usage " + usage + "% ");
        usage = ntwNodeMonitor.getPhysicalMemoryUsage(ntwNode);
        assertThat("-1: error code returned by nativ function ",usage, is(not(-1)));
        System.out.println("Physical memory usage " + usage + "% ");
    }

    @Test
    public void testCPU() {
        int usage = ntwNodeMonitor.getCPUUsage(ntwNode);
        assertThat("-1: error code returned by nativ function ",usage, is(not(-1)));
        System.out.println("CPU usage " + usage + "% ");

    }

    @Test
    public void testInterface() {
        this.nodeInterface.setNodeInterfaceName("eth0");
        this.nodeInterface.setParentNode(ntwNode);

        //test of local node, interface eth0
        int tx = ntwNodeMonitor.getInterfaceTransmit(nodeInterface);
        int rx =  ntwNodeMonitor.getInterfaceReceive(nodeInterface);
        assertThat("rx is -1: error code returned by nativ function ",tx,is(not(-1)));
        assertThat("tx is -1: error code returned by nativ function ",rx,is(not(-1)));
        System.out.println("On eth0 in last second was " + tx + " bytes transmitted and " + rx +
        " bytes received");
        System.out.println("Interface " + this.nodeInterface.getNodeInterfaceName() + " up =  " + this.ntwNodeMonitor.isInterfaceUp(nodeInterface));
        

    }

    @Test
    public void testThreads() {
         try {
             this.ntwNodeMonitor.startMonitoring();
             System.out.println("Waiting...");
             Thread.sleep(8000);
             testCPU();
             testMemory();
             testInterface();
             this.ntwNodeMonitor.stopMonitoring();
             Thread.sleep(1000);
         } catch(InterruptedException ex) {
             ex.printStackTrace();
         }
    }

    @Test
    public void testListener() {
        this.nodeInterface.setNodeInterfaceName("eth0");
        this.nodeInterface.setParentNode(ntwNode);

        ntwNodeMonitor.registerNetworkNodeInterfaceMonitorListener(nodeInterface, new NetworkNodeMonitorListener() {
            public void onNodeInterfaceDown(EndpointNodeInterface nodeInterface) {
                System.out.println("Listener 1 onNodeInterfaceDown method invoked.");
            }
            public void onNodeInterfaceUp(EndpointNodeInterface nodeInterface) {
                System.out.println("Listener 1 onNodeInterfaceUP method invoked.");                                                            
            }
        });
         ntwNodeMonitor.registerNetworkNodeInterfaceMonitorListener(nodeInterface, new NetworkNodeMonitorListener() {
            public void onNodeInterfaceDown(EndpointNodeInterface nodeInterface) {
                System.out.println("Listener 2 onNodeInterfaceDown method invoked.");
            }
            public void onNodeInterfaceUp(EndpointNodeInterface nodeInterface) {
                System.out.println("Listener 2 onNodeInterfaceUP method invoked.");
            }
        });
        ntwNodeMonitor.startMonitoring();
        try {
            Thread.sleep(5000);
        } catch(InterruptedException ex) {
            ex.printStackTrace();
        }
        ntwNodeMonitor.stopMonitoring();
        


    }

}
