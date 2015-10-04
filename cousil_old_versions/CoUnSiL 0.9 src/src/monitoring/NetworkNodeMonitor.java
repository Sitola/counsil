package monitoring;

/**
 * NetworkNodeMonitor monitors network node
 * User: xsuchom1
 * Date: May 21, 2008
 * Time: 10:43:04 AM
 * To change this template use File | Settings | File Templates.
 */

import networkRepresentation.EndpointNodeInterface;
import networkRepresentation.EndpointNetworkNode;
import java.util.concurrent.*;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.*;
import org.apache.log4j.Logger;

import utils.TimeUtils;


public class NetworkNodeMonitor {
    static Logger logger = Logger.getLogger(NetworkNodeMonitor.class);

    private static final int RECEIVE = 0;   //constant for native method
    private static final int TRANSMIT = 1;  //constant for native method
    private static final int SWAPANDRAM = 1;//constant for native method
    private static final int RAM = 0;       //constant for native method
    private AtomicInteger measurePeriod = new AtomicInteger(1); //time to measure data


    private String[] nodeInterfacesNames; //names of local interfaces
    private AtomicBoolean startMonitoring = new AtomicBoolean(false);
    private AtomicBoolean stopMonitoring = new AtomicBoolean(false);
    private ConcurrentHashMap<EndpointNodeInterface, CopyOnWriteArrayList<NetworkNodeMonitorListener>> nodeInterfaceListenersMap;
    //private CopyOnWriteArrayList<Thread> monitoringThreads;//list of all threads
  //  private CopyOnWriteArraySet<NetworkNode> nodes;

    
//last values of monitoring data in order not to wait for them
    private AtomicInteger lastCPUUsage = new AtomicInteger(-1);
    private AtomicInteger lastMemoryUsage = new AtomicInteger(-1);
    private AtomicInteger lastPhysicalMemoryUsage = new AtomicInteger(-1);
    private ConcurrentHashMap<String, Integer> lastInterfacesTransmit;
    private ConcurrentHashMap<String, Integer> lastInterfacesReceive;
    private ConcurrentHashMap<String, Boolean> lastInterfacesStatus;
  //monitoring threads
    private Thread cpuMonitor, memoryMonitor, physicalMemoryMonitor, interfaceReceiveMonitor, interfaceTransmitMonitor, interfaceStatusMonitor;


    static {
        //String path = System.getProperty("java.library.path");
        //System.out.println(path);
        
        System.loadLibrary("Monitoring_NetworkNodeMonitor");
    }

    private class CPUMonitorThread extends Thread{
        public void run() {
            while(!stopMonitoring.get()) {
                TimeUtils.sleepFor(Math.round(200 * Math.random())); //sleep random of time
                int result = getLocalCPUUsage(measurePeriod.get());
                if (result != -1) {
                    lastCPUUsage.set(result);
                }
                logger.debug("Updating last CPU usage to " + lastCPUUsage.get() + "%");
            }
        }

    }

    private class MemoryMonitorThread extends Thread{
        public void run() {
            while(!stopMonitoring.get()) {
                TimeUtils.sleepFor(Math.round(3000 * Math.random())); //sleep random of time
                int result = getLocalMemoryUsage(SWAPANDRAM);
                if (result != -1) {
                    lastMemoryUsage.set(result);
                }
                logger.debug("Updating last Memory usage to " + lastMemoryUsage.get() + "%");
            }
        }
    }

    private class PhysicalMemoryMonitorThread extends Thread{
        public void run() {
            while(!stopMonitoring.get()) {
                TimeUtils.sleepFor(Math.round(3000 * Math.random())); //sleep random of time
                int result = getLocalMemoryUsage(RAM);
                if (result != -1) {
                    lastPhysicalMemoryUsage.set(result);
                }
                logger.debug("Updating last Physical memory usage to " + lastPhysicalMemoryUsage.get() + "%");
            }
        }
    }

    private class InterfacesTransmitMonitorThread extends Thread {
        public void run(){
            while(!stopMonitoring.get()) {
                TimeUtils.sleepFor(Math.round(1000 * Math.random())); //sleep random of time
                assert nodeInterfacesNames != null: "Local interfaces names are missing";
                for (String interfaceName: nodeInterfacesNames) {
                    int tx = getLocalInterfaceUsage(interfaceName, TRANSMIT, measurePeriod.get());
                    if (tx != -1){
                        lastInterfacesTransmit.replace(interfaceName, new Integer(tx));
                        logger.debug("Updating interface " + interfaceName + " transmit to " + tx);
                    }
                }
            }
        }
    }

     private class InterfacesReceiveMonitorThread extends Thread {
        public void run(){
            while(!stopMonitoring.get()) {
                TimeUtils.sleepFor(Math.round(1000 * Math.random())); //sleep random of time
                assert nodeInterfacesNames != null: "Local interfaces names are missing";
                for (String interfaceName: nodeInterfacesNames) {
                    int rx = getLocalInterfaceUsage(interfaceName, RECEIVE,measurePeriod.get());
                    if (rx != -1)  {
                        lastInterfacesReceive.replace(interfaceName, new Integer(rx));
                        logger.debug("Updating interface " + interfaceName + " receive to " + rx);
                    }
                }
            }
        }
    }

    private class InterfacesStatusMonitorThread extends Thread {
        public void run() {

            while(!stopMonitoring.get()) {
                TimeUtils.sleepFor(Math.round(5000 * Math.random())); //sleep random of time
                assert nodeInterfacesNames != null: "Local interfaces names are missing";

                for (String interfaceName: nodeInterfacesNames) {
                    if(isLocalInterfaceUp(interfaceName) == 0) { //current interface is momentally down
                        //listeners check
                        if (lastInterfacesStatus.get(interfaceName)) { //change from previous state - interface went down
                            for (EndpointNodeInterface currentNodeInterface: nodeInterfaceListenersMap.keySet()) {
                                if (currentNodeInterface.getNodeInterfaceName().equals(interfaceName)) {//listener exists
                                    //call all registered to this node interface listeners
                                    for (NetworkNodeMonitorListener listener: nodeInterfaceListenersMap.get(currentNodeInterface)) {
                                        listener.onNodeInterfaceDown(currentNodeInterface);
                                    }
                                }
                            }
                        }
                        lastInterfacesStatus.replace(interfaceName, false);
                    }
                    else { //current interface is momentally up
                        //listeners check
                        if (!lastInterfacesStatus.get(interfaceName)) { //change from previous state - interface went up
                            for (EndpointNodeInterface currentNodeInterface: nodeInterfaceListenersMap.keySet()) {
                                if (currentNodeInterface.getNodeInterfaceName().equals(interfaceName)) {//listener exists
                                    //call all registered to this node interface listeners
                                    for (NetworkNodeMonitorListener listener: nodeInterfaceListenersMap.get(currentNodeInterface)) {
                                        listener.onNodeInterfaceUp(currentNodeInterface);
                                    }
                                }
                            }
                        }
                        lastInterfacesStatus.replace(interfaceName, true);
                    }
                    logger.debug("Updating interface " + interfaceName + " status to " + lastInterfacesStatus.get(interfaceName));
                }
            }
        }
    }

    /**
     * Constructor
     */
    public NetworkNodeMonitor() {

        this.nodeInterfacesNames = getLocalInterfacesNames();
        assert this.nodeInterfacesNames != null : "Interfaces names are missing";

        //this.monitoringThreads = new CopyOnWriteArrayList<Thread>();
        this.nodeInterfaceListenersMap = new ConcurrentHashMap<EndpointNodeInterface, CopyOnWriteArrayList<NetworkNodeMonitorListener>>();

        this.lastInterfacesTransmit = new ConcurrentHashMap<String, Integer>(3);
        this.lastInterfacesReceive = new ConcurrentHashMap<String, Integer>(3);
        this.lastInterfacesStatus = new ConcurrentHashMap<String, Boolean>(3);

        for(String interfaceName: this.nodeInterfacesNames){
            this.lastInterfacesTransmit.put(interfaceName, new Integer(-1));
            this.lastInterfacesReceive.put(interfaceName, new Integer(-1));
            this.lastInterfacesStatus.put(interfaceName, false);
        }

        cpuMonitor = new CPUMonitorThread();
        memoryMonitor = new MemoryMonitorThread();
        physicalMemoryMonitor = new PhysicalMemoryMonitorThread();
        interfaceTransmitMonitor = new InterfacesTransmitMonitorThread();
        interfaceReceiveMonitor = new InterfacesReceiveMonitorThread();
        interfaceStatusMonitor = new InterfacesStatusMonitorThread();
     }
    /**
     * Native method for usage of this class only
     * @param period for how long the measuring is taken
     * @return int as percentage load of cpu, max 100
     */
    private native synchronized int getLocalCPUUsage(int period);

    /**
     * Native method for usage of this class only
     * @param  swapOrRAM 0 = RAM only, 1 = RAM + SWAP
     * @return int as percentage of alocated memory max 100
     */
    private native synchronized int getLocalMemoryUsage(int swapOrRAM);

    /**
     * Native method for usage of this class only
     * @return String[] of names of local interfaces in this machine
     */
    private native synchronized String[] getLocalInterfacesNames();

    /**
     *
     * @param interfaceName name of interface
     * @param rxTx  0 = rx receive, 1 = tx transmit
     * @param period period for how long the measuring is taken
     * @return tx
     */
    private native synchronized int getLocalInterfaceUsage(String interfaceName, int rxTx, int period);

    /**
     *
     * @param interfaceName name of interface
     * @return 1 if interface is up, 0 if it's down
     */
    private native synchronized int isLocalInterfaceUp(String interfaceName);

    /**
     * Sets measurign period of time for waiting native methods in seconds (default 1)  
     * @param period int > 0
     */
    public void setMeasureLengthPeriod(int period) {
        if (period > 0) {
            this.measurePeriod.set(period);
        }
        else logger.debug("period must be grater than 0");
    }

    /**
     * Must be called in order to get monitoring information immediately, otherwise it's waiting call
     */
    public void startMonitoring() {
        this.startMonitoring.set(true);
        cpuMonitor.start();
        memoryMonitor.start();
        physicalMemoryMonitor.start();
        interfaceReceiveMonitor.start();
        interfaceTransmitMonitor.start();
        interfaceStatusMonitor.start();
    }

    /**
     * Stops monitoring
     */
    public void stopMonitoring() {
        this.stopMonitoring.set(true);
        try {
          this.cpuMonitor.join();
          this.memoryMonitor.join();
          this.physicalMemoryMonitor.join();
          this.interfaceReceiveMonitor.join();
          this.interfaceTransmitMonitor.join();
          this.interfaceStatusMonitor.join();

        } catch (InterruptedException e) {
            e.printStackTrace();
       }

    }

    /**
     *
     * @param networkNode NetworkNode to get data from
     * @return  percentual CPU load
     */
    public int getCPUUsage(EndpointNetworkNode networkNode){
        if (networkNode.isLocalNode()) {
            if (this.startMonitoring.get()) {
                return this.lastCPUUsage.get();
            }
            else {
                return this.getLocalCPUUsage(measurePeriod.get());
            }
        }
        else { //redirect to another node

            return -1;
        }
    }



    /**
     * @param networkNode NetworkNode to get data from
     * @return  int percentual usage of memory (RAM + SWAP)
     */
    public int getMemoryUsage(EndpointNetworkNode networkNode) {
        if (networkNode.isLocalNode()) {
            if (this.startMonitoring.get()) {
                return this.lastMemoryUsage.get();
            }
            else {
                return this.getLocalMemoryUsage(SWAPANDRAM);
            }
        }
        else { //redirect to another node

            return -1;
        }   
    }

    /**
     *
     * @param networkNode NetworkNode to get data from
     * @return int percentual usage of memory (RAM only)
     */
    public int getPhysicalMemoryUsage(EndpointNetworkNode networkNode) {
        if (networkNode.isLocalNode()) {
            if (this.startMonitoring.get()) {
                return this.lastPhysicalMemoryUsage.get();
            }
            else {
                return this.getLocalMemoryUsage(RAM);
            }
        }
        else { //redirect to another node

            return -1;
        }
    }

    /**
     * This method is called via getInterfaceReceive() or getInterfaceTransmit public methods
     * @param nodeInterface, int receiveTransmit
     * @param receiveTransmit whether receive or transmit
     * @return  int
     */
    private int getInterfaceReceiveTransmit(EndpointNodeInterface nodeInterface, int receiveTransmit)  {
      try {

        EndpointNetworkNode parentNode = nodeInterface.getParentNode();

        assert this.nodeInterfacesNames != null :"names of interfaces are missing";
        assert parentNode != null:"Parent node is null";
          
        if (parentNode.isLocalNode()) {
            for (String nodeInterfacesName : this.nodeInterfacesNames) {
                if (nodeInterfacesName.equals(nodeInterface.getNodeInterfaceName())) { //interface name match
                    if (startMonitoring.get()) { //should have last info
                        if (receiveTransmit == RECEIVE) {
                            return this.lastInterfacesReceive.get(nodeInterface.getNodeInterfaceName());
                        } else if (receiveTransmit == TRANSMIT) {
                            return this.lastInterfacesTransmit.get(nodeInterface.getNodeInterfaceName());
                        } else {
                            throw new RuntimeException("You should never get here!");
                        }
                    } else { //waiting method
                        return getLocalInterfaceUsage(nodeInterfacesName, receiveTransmit, measurePeriod.get());
                    }
                }
            }
         //no interface name match
            throw new UnknownHostException("No Interface Match");

        }
          else {     //redirect to another node
            return -1;
        }
      }
      catch (UnknownHostException ex) {
          ex.printStackTrace();
          return -1;
      }
        
    }

    /**
     *
     * @param ndI - NodeInterface
     * @return returns int as number of bytes Received through NodeInterface in last mesured period
     */
    public int getInterfaceReceive(EndpointNodeInterface ndI) {
        return this.getInterfaceReceiveTransmit(ndI, RECEIVE);
    }

    /**
     *
     * @param ndI NodeInterface
     * @return returns int as number of bytes Transmitted through NodeInterface in mesured period
     */
    public int getInterfaceTransmit(EndpointNodeInterface ndI) {
        return this.getInterfaceReceiveTransmit(ndI, TRANSMIT);
    }

    /**
     *
     * @param nodeInterface NodeInterface to test
     * @return true if nodeInterface is currently up
     */
    public boolean isInterfaceUp(EndpointNodeInterface nodeInterface) {
        EndpointNetworkNode parentNode = nodeInterface.getParentNode();

        assert this.nodeInterfacesNames != null :"names of interfaces are missing";
        assert parentNode != null:"Parent node is null";

        if (parentNode.isLocalNode()) {
            for (String nodeInterfacesName : this.nodeInterfacesNames) {
                if (nodeInterfacesName.equals(nodeInterface.getNodeInterfaceName())) { //interface name match
                    if (this.startMonitoring.get()) { //should have last info - use cached data
                        return this.lastInterfacesStatus.get(nodeInterface.getNodeInterfaceName());
                    } else {
                        return isLocalInterfaceUp(nodeInterface.getNodeInterfaceName()) != 0;
                    }
                }
            }
             //no interface name match
             throw new IllegalArgumentException("No interface match with local interfaces: " + nodeInterface.getNodeInterfaceName());
        }
        else {
            throw new IllegalArgumentException("nodeInterface is not from local node");
        }

    }

    /**
     * Registers new NetworkNodeMonitorListener to one nodeInterface of local node
     * @param nodeInterface register for
     * @param listener to register
     */
    public void registerNetworkNodeInterfaceMonitorListener(EndpointNodeInterface nodeInterface, NetworkNodeMonitorListener listener) {
        EndpointNetworkNode parentNode = nodeInterface.getParentNode();

        assert this.nodeInterfacesNames != null :"names of interfaces are missing";
        assert parentNode != null:"Parent node is null";

        if (parentNode.isLocalNode()) {
            for (String nodeInterfacesName : this.nodeInterfacesNames) {
                if (nodeInterfacesName.equals(nodeInterface.getNodeInterfaceName())) { //interface name match, adding listener
                    if (!this.nodeInterfaceListenersMap.containsKey(nodeInterface)) {
                        nodeInterfaceListenersMap.put(nodeInterface, new CopyOnWriteArrayList<NetworkNodeMonitorListener>());
                    }
                    nodeInterfaceListenersMap.get(nodeInterface).add(listener);
                    logger.debug("adding new listener for  " + nodeInterface.getNodeInterfaceName() + " " + listener);
                    return;
                }
            }
            throw new IllegalArgumentException("No interface match with local interfaces: " + nodeInterface.getNodeInterfaceName());

        }
        else {
           throw new IllegalArgumentException("nodeInterface is not from local node");
        }

    }




    @Override
    public String toString() {
        String result = "NetworkNodeMonitor, local interafces are: ";
        assert this.nodeInterfacesNames != null :"names of interfaces are missing";
        for (String nodeInterfacesName : this.nodeInterfacesNames) {
            result += " " + nodeInterfacesName;

        }
        return result;
    }



}
