package monitoring;

import monitoring.lifeservice.LifeClient;
import monitoring.lifeservice.LifeServerUDP;
import networkRepresentation.LogicalNetworkLink;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;
import utils.ProxyNodeConnection;
import utils.TimeUtils;

/**
 * Class for autonomous network monitoring. Cycles through a set of network links and tests if
 * they are alive. In case of some event, appropriate listeners are called provided they are defined.
 * The testing cycle tries to do and complete testing once per specified monitorRoundTime.
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 18.9.2007
 * Time: 11:45:18
 */
public class NetworkMonitor {
    static Logger logger = Logger.getLogger(NetworkMonitor.class);

    public static class NetworkMonitorClass {
        public final int targetRoundTime;

        public NetworkMonitorClass(int targetRoundTime) {
            this.targetRoundTime = targetRoundTime;
        }

        @Override
        public String toString() {
            return this.getClass().getName() + "@RTT=" + targetRoundTime;
        }
    }

    private ConcurrentHashMap<LogicalNetworkLink, CopyOnWriteArrayList<NetworkMonitorListener>> linkListenersMap; // keyset() of this also serves for maintaining complete set of monitored links across all monitoring classes
    private ConcurrentHashMap<LogicalNetworkLink, AtomicBoolean> activeLinks;
    private CopyOnWriteArraySet<NetworkMonitorListener> listenersForAll;
    private ConcurrentHashMap<NetworkMonitorClass, CopyOnWriteArrayList<LogicalNetworkLink>> monitorClassMap;
    private ConcurrentHashMap<LogicalNetworkLink, NetworkMonitorClass> linkClassMap;
    private ConcurrentHashMap<NetworkMonitorClass, Thread> monitorThreads;
    ThreadPoolExecutor threadPoolExecutor;
    int threadPoolSize = 10;
    final AtomicBoolean startMonitoring = new AtomicBoolean(false);
    AtomicBoolean stopMonitoring = new AtomicBoolean(false);
    final int linkMinTimeout = 30; // in milliseconds
    final int linkTestNumber = 3;  // specifies how many times in succession has the change occur in order to be considered stable
    final boolean linkInitState = true;
    LifeServerUDP lifeServerUDP = null;

    /**
     * NetworkMonitor constructor. Adds provided links to the monitored link set.
     * <p/>
     */
    public NetworkMonitor() {
        linkListenersMap = new ConcurrentHashMap<>();
        activeLinks = new ConcurrentHashMap<>();
        listenersForAll = new CopyOnWriteArraySet<>();
        monitorClassMap = new ConcurrentHashMap<>();
        linkClassMap = new ConcurrentHashMap<>();
        monitorThreads = new ConcurrentHashMap<>();
        threadPoolExecutor = new ThreadPoolExecutor(threadPoolSize, 2 * threadPoolSize, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }

    /**
     * Registers new monitoring class into the system
     * <p/>
     *
     * @param monitorClass to be registered with the system
     */
    public void addMonitoringClass(final NetworkMonitorClass monitorClass) {
        assert !monitorClassMap.containsKey(monitorClass) : "Class is already registered!";
        NetworkMonitor.logger.debug("onAdding network monitoring class " + monitorClass);
        Thread monitorThread = new Thread() {
            @Override
            public void run() {
                // wait until we are supposed to start
                synchronized (startMonitoring) {
                    if (!startMonitoring.get()) {
                        //noinspection EmptyCatchBlock
                        try {
                            startMonitoring.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
                // sleep for random time up to 200ms so that not all the threads start synchronously
                TimeUtils.sleepFor(Math.round(200 * Math.random()));
                // monitoring loop
                NetworkMonitor.logger.debug("Entering monitoring loop for class " + monitorClass);
                while (!stopMonitoring.get()) {
                    long sleepUntilTime = System.currentTimeMillis() + monitorClass.targetRoundTime;
                    ArrayList<FutureTask> linkThreads = new ArrayList<FutureTask>();
                    NetworkMonitor.logger.trace("Initiating tests for links in class " + monitorClass);
                    synchronized (NetworkMonitor.this) {
                        for (final LogicalNetworkLink networkLink : monitorClassMap.get(monitorClass)) {
                            logger.debug("Adding link " + networkLink + " for monitoring");
                            @SuppressWarnings({"unchecked"})
                            FutureTask ft = new FutureTask(new Runnable() {
                                @Override
                                public void run() {
                                    networkTest(networkLink);
                                }
                            }, null);
                            linkThreads.add(ft);
                        }
                    }
                    NetworkMonitor.logger.trace("Executing tests for links in class " + monitorClass);
                    for (FutureTask futureTask : linkThreads) {
                        threadPoolExecutor.execute(futureTask);
                    }
                    NetworkMonitor.logger.trace("Collecting test results for links in class " + monitorClass);
                    for (FutureTask futureTask : linkThreads) {
                        //noinspection EmptyCatchBlock
                        try {
                            futureTask.get();
                        } catch (InterruptedException e) {
                            NetworkMonitor.logger.debug("Encountered InterruptedException in monitoring");
                        } catch (ExecutionException e) {
                            NetworkMonitor.logger.debug("Encountered ExecutionException in monitoring");
                        }
                    }
                    NetworkMonitor.logger.trace("Test results collected for links in class " + monitorClass);
                    sleepUntil(sleepUntilTime);
                }
            }
        };
        synchronized(this) {
            monitorClassMap.put(monitorClass, new CopyOnWriteArrayList<LogicalNetworkLink>());
            monitorThreads.put(monitorClass, monitorThread);
        }
        monitorThread.start();
    }

    /**
     * Adds network link to monitored link set
     * <p/>
     *
     * @param networkLink  link to be added
     * @param monitorClass monitoring class for given link
     */
    public synchronized void addNetworkLink(LogicalNetworkLink networkLink, NetworkMonitorClass monitorClass) {
        assert monitorClassMap.containsKey(monitorClass) : "Monitoring class hasn't been registered!";
        if (!linkListenersMap.containsKey(networkLink)) {
            NetworkMonitor.logger.info("Adding network link " + networkLink + " to be monitored in class " + monitorClass);
            linkListenersMap.put(networkLink, new CopyOnWriteArrayList<NetworkMonitorListener>());
            activeLinks.put(networkLink, new AtomicBoolean(linkInitState));
            monitorClassMap.get(monitorClass).add(networkLink);
            linkClassMap.put(networkLink, monitorClass);
        } else if (!linkClassMap.get(networkLink).equals(monitorClass)) {
            // move the link from old monitorClass to requested one
            NetworkMonitor.logger.debug("Changing network link " + networkLink + " from class " + linkClassMap.get(networkLink) + " to class " + monitorClass);
            monitorClassMap.get(linkClassMap.get(networkLink)).remove(networkLink);
            linkClassMap.remove(networkLink);
            monitorClassMap.get(monitorClass).add(networkLink);
            linkClassMap.put(networkLink, monitorClass);
        } else {
            NetworkMonitor.logger.debug("Network link " + networkLink + " left untouched in class " + monitorClass);
        }
    }

    /**
     * Removes network link from monitored link set
     * <p/>
     *
     * @param networkLink link to be removed
     */
    public synchronized void removeNetworkLink(LogicalNetworkLink networkLink) {
        NetworkMonitor.logger.debug("Removing network link " + networkLink);
        if (linkListenersMap.containsKey(networkLink)) {
            for (NetworkMonitorListener networkMonitorListener : linkListenersMap.get(networkLink)) {
                linkListenersMap.get(networkLink).remove(networkMonitorListener);
            }
            linkListenersMap.remove(networkLink);
            activeLinks.remove(networkLink);
            monitorClassMap.get(linkClassMap.get(networkLink)).remove(networkLink);
            linkClassMap.remove(networkLink);
        }
    }

    /**
     * Adds listener for a specific networkLink
     * <p/>
     *
     * @param networkLink            network link to be monitored
     * @param networkMonitorListener listerner to be called on change
     */
    public synchronized void addNetworkMonitorListener(LogicalNetworkLink networkLink, NetworkMonitorListener networkMonitorListener) {
        NetworkMonitor.logger.debug("Adding network listener for " + networkLink + ": " + networkMonitorListener);
        if (!linkListenersMap.containsKey(networkLink)) {
            throw new IllegalArgumentException("Network link " + networkLink + " is not registered with the NetworkMonitor!");
        }
        linkListenersMap.get(networkLink).add(networkMonitorListener);
    }

    /**
     * Adds listener for all networkLinks
     * <p/>
     *
     * @param networkMonitorListener network listner to add
     */
    public synchronized void addNetworkMonitorListenerForAll(NetworkMonitorListener networkMonitorListener) {
        NetworkMonitor.logger.info("Adding network listener for all: " + networkMonitorListener);
        listenersForAll.add(networkMonitorListener);
    }

    /**
     * Returns set of monitored links.
     * <p/>
     *
     * @return set of monitored links
     */
    public synchronized Set<LogicalNetworkLink> getMonitoredLinks() {
        return linkListenersMap.keySet();
    }

    /**
     * For given link, returns its current monitoring class
     * <p/>
     *
     * @param networkLink link to query
     * @return monitoring class which the link currently belongs to
     */
    public NetworkMonitorClass getNetworkMonitorClass(LogicalNetworkLink networkLink) {
        return linkClassMap.get(networkLink);
    }


    /**
     * Starts monitoring
     */
    public void startMonitoring() {
        NetworkMonitor.logger.info("Starting liveness testing server");
        lifeServerUDP = new LifeServerUDP();
        lifeServerUDP.start();

        NetworkMonitor.logger.debug("Starting monitoring");
        startMonitoring.set(true);
        synchronized (startMonitoring) {
            startMonitoring.notifyAll();
        }
    }

    /**
     * Requests to stop monitoring     *
     */
    public void stopMonitoring() {
        NetworkMonitor.logger.debug("Stoping monitoring");
        stopMonitoring.set(true);
        for (Thread thread : monitorThreads.values()) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                if (NetworkMonitor.logger.isDebugEnabled()) {
                    e.printStackTrace();
                }
                NetworkMonitor.logger.error("Unable to join thread " + thread);
            }
        }
        threadPoolExecutor.shutdown();

        NetworkMonitor.logger.info("Stopping liveness testing server");
        lifeServerUDP.stop();
    }

    /**
     * Actual implementation of monitoring for a single networkLink
     * <p/>
     *
     * @param networkLink to perform monitoring upon
     */
    private void networkTest(LogicalNetworkLink networkLink) {
        try {
            NetworkMonitor.logger.trace("Testing network link: " + networkLink);
            InetAddress linkTgtAddress = InetAddress.getByName(networkLink.getToInterface().getIpAddress());
            //noinspection UnusedAssignment
            long rtt = -1;
            boolean status = activeLinks.get(networkLink).get(); // initialization is just bogus here to keep the compiler happy
            if (!networkLink.getFromNode().isProxyNode() && networkLink.getToNode().isProxyNode()) {
                logger.warn("Can't monitor from non-proxy node to proxy node!");
                // TODO: Why?
            }
            else {
                for (int i = 0; i < linkTestNumber; i++) {
                    double timeOut = 2000; // 4 * networkLink.getLatency();
                    timeOut = (timeOut > linkMinTimeout ? timeOut : linkMinTimeout);
                    NetworkMonitor.logger.debug("Testing target " + linkTgtAddress + " (link " + networkLink + ")");
                    if (networkLink.getFromNode().isProxyNode()) {
                        rtt = (int) Math.round(networkLink.getFromNode().getProxyNodeConnection().sendPing(linkTgtAddress.toString()));
                    }
                    else {
                        rtt = LifeClient.testAliveUDP(linkTgtAddress, (int) Math.round(timeOut));
                    }
                    if (rtt != -1) {
                        status = true;
                        // we set the latency based on floating average
                    } else {
                        status = false;
                    }

                    NetworkMonitor.logger.trace("Measured RTT for " + linkTgtAddress + " is " + rtt + "ms with active status=" + status);
                    
                    if (status == activeLinks.get(networkLink).get()) {
                        
                        NetworkMonitor.logger.trace("The link status has not changed, updating latency:");
                        
                        if (i > 0) {
                            // link flap detected
                            NetworkMonitor.logger.trace("Network link: " + networkLink + " flap detected.");
                            for (NetworkMonitorListener networkMonitorListener : linkListenersMap.get(networkLink)) {
                                networkMonitorListener.onNetworkLinkFlap(networkLink);
                            }
                            for (NetworkMonitorListener networkMonitorListener : listenersForAll) {
                                networkMonitorListener.onNetworkLinkFlap(networkLink);
                            }
                        }
                        
                        try {
                            boolean tmp = networkLink.logLatency(rtt / 2);
                        } catch (Exception e) {
                            e.printStackTrace(System.out);
                        }
                        
                        if (status && networkLink.logLatency(rtt / 2)) {
                            NetworkMonitor.logger.trace("Network link: " + networkLink + " latency change detected.");
                            networkLink.adjustLatencyToHistory();
                            for (NetworkMonitorListener networkMonitorListener : linkListenersMap.get(networkLink)) {
                                networkMonitorListener.onNetworkLinkLatencyChange(networkLink);
                            }
                            for (NetworkMonitorListener networkMonitorListener : listenersForAll) {
                                networkMonitorListener.onNetworkLinkLatencyChange(networkLink);
                            }
                        }
                        
                    }
                }
            }
            
            activeLinks.get(networkLink).set(status);
            if (status) {
                NetworkMonitor.logger.trace("Network link: " + networkLink + " link up detected.");
                for (NetworkMonitorListener networkMonitorListener : linkListenersMap.get(networkLink)) {
                    networkMonitorListener.onNetworkLinkReestablished(networkLink);
                }
                for (NetworkMonitorListener networkMonitorListener : listenersForAll) {
                    networkMonitorListener.onNetworkLinkReestablished(networkLink);
                }
            } else {
                NetworkMonitor.logger.trace("Network link: " + networkLink + " link down detected.");
                for (NetworkMonitorListener networkMonitorListener : linkListenersMap.get(networkLink)) {
                    networkMonitorListener.onNetworkLinkLost(networkLink);
                }
                for (NetworkMonitorListener networkMonitorListener : listenersForAll) {
                    networkMonitorListener.onNetworkLinkLost(networkLink);
                }
            }
        } catch (UnknownHostException e) {
            NetworkMonitor.logger.error("Invalid IP adress. Bailing out!");
        } catch (ProxyNodeConnection.ProxyNodeCommandFailed proxyNodeCommandFailed) {
            if (NetworkMonitor.logger.isDebugEnabled()) {
                proxyNodeCommandFailed.printStackTrace();
            }
            NetworkMonitor.logger.error("Network error occured - unable to monitor network through proxied node!");
        }
    }


    /**
     * Sleeps until specified time.
     * <p/>
     *
     * @param untilTime time specified in milliseconds from epoch (System.currentTimeMillis())
     */
    private void sleepUntil(long untilTime) {
        final long until = untilTime - System.currentTimeMillis();
        if (until < 0) {
            NetworkMonitor.logger.warn("Requested to sleep for negative time. If appears multiple times, it may diagnose insuffcient number of NetworkMonitor threads to make monitoring in timely fashion.");
            return;
        }
        //noinspection EmptyCatchBlock
        try {
            Thread.sleep(until);
        } catch (InterruptedException e) {
        }
    }
}
