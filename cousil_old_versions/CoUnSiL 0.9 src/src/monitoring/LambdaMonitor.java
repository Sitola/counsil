package monitoring;

import networkRepresentation.LambdaLink;
import networkRepresentation.LambdaLinkFactory;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by IntelliJ IDEA.
 * User: xliska
 * Date: 27.1.2009
 * Time: 22:17:05
 */
public class LambdaMonitor {
    static Logger logger = Logger.getLogger(NetworkMonitor.class);

    private ArrayList<LambdaLink> monitoredLambdas;
    Thread lambdaMonitorThread;
    ThreadPoolExecutor threadPoolExecutor;
    int threadPoolSize = 10;
    final AtomicBoolean startMonitoring = new AtomicBoolean(false);
    AtomicBoolean stopMonitoring = new AtomicBoolean(false);

    long targetRoundTime = 20000; // time in miliseconds

    public LambdaMonitor() {
        monitoredLambdas = new ArrayList<LambdaLink>();
        threadPoolExecutor = new ThreadPoolExecutor(threadPoolSize, 2 * threadPoolSize, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }

    public void startMonitoring() {
        NetworkMonitor.logger.debug("Starting monitoring for lambda links.");
        lambdaMonitorThread = new Thread() {
            public void run() {
                NetworkMonitor.logger.debug("Entering monitoring loop for lambda links.");
                while (!stopMonitoring.get()) {
                    long sleepUntilTime = System.currentTimeMillis() + targetRoundTime;
                    ArrayList<FutureTask> linkThreads = new ArrayList<FutureTask>();
                    NetworkMonitor.logger.trace("Initiating tests for monitored lambda links");
                    synchronized (this) {
                        for (final LambdaLink lambdaLink : monitoredLambdas) {
                            @SuppressWarnings({"unchecked"})
                            FutureTask ft = new FutureTask(new Runnable() {
                                public void run() {
                                    lambdaLinkTestAndModify(lambdaLink);
                                }
                            }, null);
                            linkThreads.add(ft);
                        }
                    }
                    NetworkMonitor.logger.debug("Executing tests for monitored lambda links");
                    for (FutureTask futureTask : linkThreads) {
                        threadPoolExecutor.execute(futureTask);
                    }
                    NetworkMonitor.logger.trace("Collecting test results for monitored lambda links");
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
                    NetworkMonitor.logger.debug("Test results collected for monitored lambda links");
                    sleepUntil(sleepUntilTime);

                }
            }
        };
        lambdaMonitorThread.start();
        startMonitoring.set(true);
    }

    public void stopMonitoring() {
        NetworkMonitor.logger.debug("Stoping monitoring");
        stopMonitoring.set(true);

        try {
            lambdaMonitorThread.join();
        } catch (InterruptedException e) {
            if (NetworkMonitor.logger.isDebugEnabled()) {
                e.printStackTrace();
            }
            NetworkMonitor.logger.error("Unable to join lambdaMonitoringThread");

        }
        threadPoolExecutor.shutdown();
    }


    public synchronized void addLambdaLink(LambdaLink lambdaLink) {
        if (!monitoredLambdas.contains(lambdaLink)) {
            NetworkMonitor.logger.debug("Adding lambda link " + lambdaLink + " to be monitored.");
            monitoredLambdas.add(lambdaLink);
        } else {
            NetworkMonitor.logger.debug("Lambda link " + lambdaLink + " is already monitored.");
        }
    }

    public synchronized void removeLambdaLink(LambdaLink lambdaLink) {
        NetworkMonitor.logger.debug("Removing lambda link " + lambdaLink);
        if (monitoredLambdas.contains(lambdaLink)) {
            monitoredLambdas.remove(lambdaLink);
        }
    }

    public synchronized ArrayList<LambdaLink> getMonitoredLinks() {
        return monitoredLambdas;
    }  

    private void lambdaLinkTest(LambdaLink lambdaLink) {
        LambdaLinkFactory lambdaLinkFactory = new LambdaLinkFactory(lambdaLink);
        lambdaLinkFactory.query(lambdaLink);
    }

    private void lambdaLinkTestAndModify(LambdaLink lambdaLink) {
        LambdaLinkFactory lambdaLinkFactory = new LambdaLinkFactory(lambdaLink);
        lambdaLinkFactory.queryAndModify(lambdaLink);
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
