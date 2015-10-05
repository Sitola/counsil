package utils;

/**
 * This is an application proxy class to facilitate running and terminating external applications,
 * e.g. by local peer agents as initiated by Application Group Controller.
 * <p/>
 * Created by IntelliJ IDEA.
 * User: xsuchom1
 * Date: Nov 26, 2008
 * Time: 10:23:38 AM
 * To change this template use File | Settings | File Templates.
 */
public class ApplicationProxyJNI {
    
// <editor-fold desc="Legacy controller">
//    extends ApplicationProxy {
//    static {
//        System.loadLibrary("utils_ApplicationProxyJNI");
//    }
//    static Logger logger = Logger.getLogger(ApplicationProxyJNI.class);
//
//    private int pid = 0;  //pid of running proces 0 not yet started
//    private int pipeOutFd;
//    private int pipeErrFd;
//    private MediaApplicationMonitor applicationMonitor;
//    private Thread runApplicationThread;
//    private AtomicBoolean running = new AtomicBoolean(false); //whether the application is running
////    private Thread holdingFifoReader; //only opens fifo, actually reading nothing
//    private Thread stdOutReader; 
//    private Thread stdErrReader;
//
// //   private final String STDOUTFIFO ="/home/xsuchom1/pokus/fifoFronta"; //stdOutFIFO";
// //   private final String STDERRFIFO ="/home/xsuchom1/";
//
//
//    /**
//     * Runs program from path with cmdOptions
//     *
//     * @param path       path to executable
//     * @param cmdOptions command line options
//     * @return -1 if error
//     */
//    private synchronized native int[] runApplication(String path, String cmdOptions);//, String stdout, String stderr);
//
//    /**
//     * Interrupts process pid SIGINT
//     * @param pid pid of new process
//     * @return result of kill function
//     */
//    private synchronized native int interruptApplication(int pid);
//    /**
//     * Terminates process pid SIGTERM
//     * @param pid pid of new process
//     * @return result of kill function
//     */
//    private synchronized native int terminateApplication(int pid);
//    /**
//     * kill process pid SIGKILL
//     * @param pid pid of new process
//     * @return result of wait function
//     */
//    private synchronized native int killApplication(int pid);
//
//    /**
//     * @param pid pid of new process
//     * @return 0 if it is runnig, 1 if not, pid if terminated via signal, -1 on error
//     */
//    private synchronized native int isNotRunning(int pid);
//
//    /**
//     *
//     * @param pipefd descriptor of pipe
//     * @return text of standard output of application 
//     */
//    private synchronized native String readStdOutPipe(int pipefd);
//
//    /**
//     *
//     * @param pipefd descriptor of pipe
//     * @return text of standard output of application
//     */
//    private synchronized native String readStdErrPipe(int pipefd);
//
//
//    /**
//     * Thread in witch the application is run
//     */
//    private class RunApplicationThread extends Thread {
//        public void run() {
//
//            int[] result = runApplication(getPath(), getOpts());//, STDOUTFIFO, STDERRFIFO);
//            running.set(true);
//            synchronized (this) {
//                pid = result[0];
//                pipeOutFd = result[1];
//                pipeErrFd = result[2];
//                if (pid == 0 ) {
//                    running.set(false);
//                    throw new RuntimeException("Pid of running application is 0, which is not possible.");
//                }
//                ApplicationProxyJNI.logger.debug("pid, pipeOutFD  a pipeErrFdjsou:" + pid + ", " + pipeOutFd + " a "+ pipeErrFd);
//            }
//            while(running.get()) {
//                try {
//                    if (checkRunning()) {
//                        TimeUtils.sleepFor(Math.round(200 * Math.random()));
//                    }
//                    else {
//                        applicationStopped();
//                    }
//                } catch (ApplicationProxyException e) {
//                    e.printStackTrace();
//                }
//            }
//
//        }
//    }
//
////   private class HoldingFifoReader extends Thread {
//  //     public void run() {
//    //       openHoldingFifoReader(STDOUTFIFO);
//      // }
//   //}
//
//    /**
//     * Constructor, this application is not monitored
//     *
//     * @param path path to executable
//     * @param opts options to executable
//     * @throws ApplicationProxyException when file to exec doesn't exist or is not accesible
//     */
//    public ApplicationProxyJNI(String path, String opts) throws ApplicationProxyException {
//        super(path, opts, false);
//   //     this.holdingFifoReader = new HoldingFifoReader();
//        this.runApplicationThread = new RunApplicationThread();
//    }
//
//    /**
//     * Constructor, this application is monitored via MediaApplicationMonitor
//     *
//     * @param path path to executable
//     * @param opts options to executable
//     * @param mon  MediaApplicationMonitor whitch is to monitored this application
//     * @throws ApplicationProxyException  when file to exec doesn't exist or is not accesible
//     */
//    public ApplicationProxyJNI(String path, String opts, MediaApplicationMonitor mon) throws ApplicationProxyException {
//        super(path, opts, true);
//   //     this.holdingFifoReader = new HoldingFifoReader();
//        this.runApplicationThread = new RunApplicationThread();
//
//        assert mon != null;
//
//        this.applicationMonitor = mon;
//    }
//
//    /**
//     * executes application from ApplicationProxy's attribute - path
//     */
//    public void run() {
//     //   this.holdingFifoReader.start();
//        logger.debug(new StringBuilder().append("starting application ").append(this));
//        this.runApplicationThread.start();
//
//        if (isMonitored()) {
//            this.applicationMonitor.registerApplication(this);
//        }
//
//        if (getLocalControllerFrame() != null) { //we have gui
//          //  logger.debug("GUI");
//        }
//        else {// we don't have GUI, thus using console
//             stdOutReader = new Thread(new Runnable() {
//                public void run() {
//                    String output;
//                        while (running.get()) {
//                            output = readStdOutPipe(pipeOutFd);
//                            if (!output.equals("")) {
//                                System.out.println(new StringBuilder().append(this).append(" stdout:").append(output));
//                            }
//                            TimeUtils.sleepFor(Math.round(1000 * Math.random()));
//                        }
//                    }
//             });
//             stdErrReader = new Thread(new Runnable() {
//                public void run() {
//                    String output;
//                    while(running.get()) {
//                        output = readStdErrPipe(pipeErrFd);
//                        if (!output.equals("")) {
//                            System.err.println(new StringBuilder().append(this).append(" stderr:").append(output));
//                        }
//                     TimeUtils.sleepFor(Math.round(1000 * Math.random()));
//                    }
//                }
//             });
//
//            stdOutReader.start();
//            stdErrReader.start();
//        }
//    }
//
//    private void applicationStopped() {
//        if (isMonitored()) {
//            this.applicationMonitor.removeApplication(this);
//        }
//        this.running.set(false);
//        synchronized (this) {
//            this.pid = 0;
//        }
//        try {
//            stdOutReader.join();
//            stdErrReader.join();
//           // this.runApplicationThread.join();
//
//           // this.holdingFifoReader.interrupt();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }
//    /**
//     * Terminates Media Application
//     */
//    public boolean kill() {
//        int status;
//        if (this.pid == 0) {return false;}
//        if (!this.running.get()) {return false;}
//        try {
//            if (!(checkRunning() && this.running.get())) {
//                applicationStopped();
//                return false;
//            }
//            else {
//                status = interruptApplication(this.pid); //try to interrupt first
//                assert status != -1;
//                Thread.sleep(1000);
//                if (checkRunning()) {
//                    status = terminateApplication(this.pid); //try to terminated
//                    assert status != -1;
//                    Thread.sleep(1000);
//                    if (checkRunning()) {
//                        Thread.sleep(1000);
//                    }
//                    else {
//                        logger.debug("Application terminated " + status);
//           //             applicationStopped();
//                        return false; //we assume termination as normal?
//                    }
//                }
//                else {
//                    logger.debug("Application interrupted " + status);
//         //           applicationStopped();
//                    return false; //we assume interrupt as normal termination ?
//                }
//            }
//        }
//        catch (ApplicationProxyException ex) {
//            ex.printStackTrace();
//        }
//        catch (InterruptedException ex) {
//            ex.printStackTrace();
//        }
//        status = killApplication(this.pid);
//        logger.debug("Application killed, waitpid returned " + status);
//
//       // applicationStopped();
//        return true;
//    }
//
//    /**
//     * Method checks whether application is running or not
//     * @return true if the application is running
//     * @throws ApplicationProxyException
//     */
//    public boolean checkRunning() throws ApplicationProxyException {
//        int result = this.isNotRunning(pid);
//
//        switch(result) {
//            case -1: throw new ApplicationProxyException("Error in function waitpid.");
//            case 0: return true;
//            case 1:
//                this.running.set(false);
//                return false;
//            default:
//                this.running.set(false);
//                logger.debug("Process was terminated with signal " + result);
//                return false;
//        }
//    }
//
//    /**
//     * Pid getter 
//     * @return pid
//     */
//    public synchronized int getPid() {
//        return pid;
//    }
//
//    @Override
//    public String toString() {
//      return "Media Application: " + getPath();  
//    }
//}
// </editor-fold>
    
    }
