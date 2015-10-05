package monitoring;

/**
 * Created by IntelliJ IDEA.
 * User: xsuchom1
 * Date: Feb 21, 2009
 * Time: 5:21:14 PM
 * To change this template use File | Settings | File Templates.
 */

import org.apache.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import utils.TimeUtils;

public class AGCMonitor {
    static Logger logger = Logger.getLogger(AGCMonitor.class);

    public enum AGCPlanStatus{STARTED, FINISHED, DISTRIBUTED, DEPLOYED}
    private AGCPlanStatus currentStatus;
    private BlockingQueue<AGCPlanStatus> requestQueue;
    private CopyOnWriteArrayList<AGCMonitorListener> agcListeners;
    private AtomicBoolean monitoring;
    private Thread listenersThread;

    private class ProcessListeners extends Thread {
        public void run() {
            AGCPlanStatus status = null;
            while(monitoring.get()) {
                if (requestQueue.size() < 1) {
                    TimeUtils.sleepFor(Math.round(2000 * Math.random())); //sleep random of time
                }
                else {
                    try {
                        status = requestQueue.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    for(AGCMonitorListener lis: agcListeners) {
                        assert status != null;
                        switch (status) {
                            case STARTED:
                                lis.onPlanStarted();
                                break;
                            case FINISHED:
                                lis.onPlanFinished();
                                break;
                            case DISTRIBUTED:
                                lis.onPlanDistributed();
                                break;
                            case DEPLOYED:
                                lis.onPlanDeployed();
                                break;
                        }
                    }
                }

            }

        }
    }
    /**
     * Constructor
     */
    public AGCMonitor() {
        this.requestQueue = new LinkedBlockingQueue<AGCPlanStatus>();
        this.agcListeners = new CopyOnWriteArrayList<AGCMonitorListener>();
        this.monitoring = new AtomicBoolean(false);
        this.listenersThread = new ProcessListeners();
    }

    public AGCPlanStatus getPlanStatus() {
        return this.currentStatus;
    }

    /**
     * Sets status of AGC, must be called from AGC only!!
     * @param status a constant from type AGCPlanStatus, which became current 
     */
    public void setAGCPlanStatus(AGCPlanStatus status) {
        this.currentStatus = status;
        try {
            this.requestQueue.put(status);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void registerAGCMonitorListener(AGCMonitorListener lis) {
        if (this.agcListeners.contains(lis)) {
            throw new IllegalArgumentException("Listener already registrated.");
        }
        if (lis == null) {
            throw new IllegalArgumentException("Given object is null");
        }
        this.agcListeners.add(lis);
        logger.debug("registering new listener " + lis);

    }
    public void startMonitoring(){
        this.monitoring.set(true);
        this.listenersThread.start();
    }

    public void stopMonitoring() {
        this.monitoring.set(false);
        try {
            this.listenersThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    
}
