package appControllers;

import agc.PlanElementBean;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import mediaApplications.RumHD;
import org.apache.log4j.Logger;

/**
 * Updated controller for the RUM-HD application.
 * // TODO: prevent multi-thread access to methods!
 * @author maara
 */
public class RumHDController extends ControllerImpl implements DistributorController {

    static Logger logger = Logger.getLogger(RumHDController.class);

    
    private static class Target {
        
        public final String ip, port;

        public Target(String ip, String port) {
            this.ip = ip;
            this.port = port;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 79 * hash + Objects.hashCode(this.ip);
            hash = 79 * hash + Objects.hashCode(this.port);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Target other = (Target) obj;
            if (!Objects.equals(this.ip, other.ip)) {
                return false;
            }
            if (!Objects.equals(this.port, other.port)) {
                return false;
            }
            return true;
        }
        
        @Override
        public String toString() {
            return ip + ":" + port;
        }
    }
    
    private class RumHDProcessController {
        
        private class ReaderThread extends Thread {

            private String stats, response;

            private long data = 0, received = 0, loss = 0;
            private final BufferedReader socketReader;

            ReaderThread(BufferedReader socketReader) {
                this.socketReader = socketReader;
                setDaemon(true);
            }

            private void processStatLine(String line) throws IOException {
                String bits[] = line.split(" ");

                if (bits.length % 2 != 1) {
                    throw new IOException("The stats line <"+line+"> is not well-formed! ");
                }

                long myData = -1, myLoss = -1, myReceived = -1;
                for (int i = 0; i < bits.length/2; i++) {
                    if (bits[2*i+1].equals("data")) {
                        myData = Long.decode(bits[2*i+2]);
                    } else if (bits[2*i+1].equals("received")) {
                        myReceived = Long.decode(bits[2*i+2]);
                    } else if (bits[2*i+1].equals("loss")) {
                        myLoss = Long.decode(bits[2*i+2]);
                    }
                }

    //            if (myLoss != 0 && isActive.get()) {
    //                reportDataLoss(myData - data, myLoss - loss);
    //            }

                data = myData;
                received = myReceived;
                loss = myLoss;
            }

            @Override
            public void run() {
                String line = null;
                try {

                    Thread.sleep(1000);

                    while (! exited.get()) {
                        if (!socketReader.ready()) {
                            Thread.sleep(50);
                            continue;
                        }

                        while ((line = socketReader.readLine()) != null) {

                            if (!line.startsWith("stats")) {                                    
                                messageDeque.offer(line);
                            } else {
                                processStatLine(line);
                            }
                            
                            reportMessageReception(line);
                            reportDebugMessage(
                                    new Serializable[] {
                                        new LinkedList<String>() {
                                            {
                                                add("#"+id);
                                                add("COM");
                                            }
                                        },
                                        " >> " +line
                                    }
                            );
                        }
                    }
                } catch (IOException | InterruptedException e) {
                }

                synchronized(this) {
                    exited.set(true);
                    notifyAll();
                }
            }
        }

        private final HashMap<Target, Integer> targetToIndexMap = new HashMap<>();
        private final HashMap<Target, String>  targetToFormatMap = new HashMap<>();
        private int nextPortIndex = 0;

        private Thread shutdownHook;
        private Socket socket;
        private BufferedReader socketReader;
        private PrintWriter socketWriter;
        private final AtomicBoolean exited = new AtomicBoolean(false);
        private ReaderThread readerThread;
        private final int id;

        public final String sourcePort;
        Process process;
        
        private final LinkedBlockingDeque<String> messageDeque = new LinkedBlockingDeque<>();
        private final LinkedBlockingDeque<CommandTuple> commandDeque = new LinkedBlockingDeque<>();
        
        private class CommandTuple {
            public final String command;
            public final Object payload;

            public CommandTuple(String command, Object payload) {
                this.command = command;
                this.payload = payload;
            }
        }
        
        /**
         * Create a new process hd-rum-transcode.
         */
        public RumHDProcessController(String sourcePort, int id) {
            this.sourcePort = sourcePort;
            this.id = id;
        }
        
        public void init() throws IOException {

            log("Starting controller for " + application.getApplicationName() + "(#"+id+") at port " + sourcePort);
            try {
                Runtime runtime = Runtime.getRuntime();
                ServerSocket serverSocket = new ServerSocket(0);

                String command = application.getApplicationPath() + " " +
                        "--control-port " + serverSocket.getLocalPort() + ":1 " +
                        application.getApplicationCmdOptions() + " " + 
                        sourcePort;

                ProcessBuilder pb = new ProcessBuilder(Arrays.asList(command.split(" ")));

                // pb.redirectErrorStream(true);
                
                process = pb.start();

                log("Process = " + process);
                
                
                shutdownHook = new Thread() {
                    @Override
                    public void run() {
                        process.destroy(); // Upon failure with exception, exits anyway => no eception handling required
                    }
                };
                runtime.addShutdownHook(shutdownHook);

                // <editor-fold desc="Threads forwarding out/err stream messages to debug">
                new PipeReader(
                        new BufferedReader(new InputStreamReader(process.getInputStream())), 
                        new PipeReader.Reporter() {
                            @Override
                            public void report(String line) {
                                reportDebugMessage(
                                    new Serializable[] {
                                        new LinkedList<String>() {
                                            {
                                                add("#"+id);
                                                add("OUT");
                                            }
                                        },
                                        line
                                    });
                            }
                        }).start();
                new PipeReader(
                        new BufferedReader(new InputStreamReader(process.getErrorStream())), 
                        new PipeReader.Reporter() {
                            @Override
                            public void report(String line) {
                                reportDebugMessage(
                                    new Serializable[] {
                                        new LinkedList<String>() {
                                            {
                                                add("#"+id);
                                                add("ERR");
                                            }
                                        },
                                        line
                                    });
                            }
                        }).start();
                // </editor-fold>

                log("Accepting socket at " + serverSocket.getLocalPort());
                
                // Guard thread to close the serverSocket after a timeout.
                //   if not guarded, the method freezes upon serverSocket.accept()
                Thread acceptGuardThread = new Thread() {
                    private Closeable closeable;
                    private long deadline;
                    Thread setCloseable(Closeable closeable, long timeout) {
                        this.closeable = closeable;
                        this.deadline = System.currentTimeMillis() + timeout;
                        return this;
                    }
                    @Override
                    public void run() {
                        while (deadline > System.currentTimeMillis()) {
                            try {
                                Thread.sleep(deadline - System.currentTimeMillis());
                            } catch (InterruptedException ex) {
                            }
                        }
                        try {
                            closeable.close();
                        } catch (IOException ex) {
                        }
                    }
                }.setCloseable(serverSocket, 1000);
                acceptGuardThread.start();

                // If the socket is not opened within timeout, IOException is thrown
                socket = serverSocket.accept();
                    
                log("Socket opened at port " + (socket == null ? "null" : socket.getLocalPort()));
                
                socketReader = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                socketWriter = new PrintWriter(
                        socket.getOutputStream(), true);
                nextPortIndex = 0;

                readerThread = new ReaderThread(socketReader);

                readerThread.start();
                log("Socket reader thread started");

            } catch (IOException ex) {
                stopApplication();
                throw ex;
            }
        }

        
        /**
         * Send the command to process
         * @param command String to be sent
         * @return true if acknowledgment arrives from the process
         */
        private boolean sendCommand(String command) throws IOException {
            if (command == null) throw new NullPointerException("Cannot send null command!");
            if (socket == null) throw new IOException("The application is not running!");
            
            reportSendingCommand(command);
            socketWriter.println(command);
            reportDebugMessage(
                    new Serializable[] {
                        new LinkedList<String>() {
                            {
                                add("#"+id);
                                add("COM");
                            }
                        },
                        " << " + command
                    }
            );

            
            
            String line = null;
            while(true) {
                try {
                    line = messageDeque.poll(RESPONSE_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS);
                    break;
                } catch (InterruptedException ex) {
                    Logger.getLogger(UltraGridController.class.getName()).log(org.apache.log4j.Level.FATAL, null, ex);
                }
            }

            if (line == null) {
                throw new IOException("Socket closed.");
            }

            log("CoUniverse request: " + command);
            log("UltraGrid reply   : " + line);

            long code = Long.parseLong(line.split(" ")[0]);

            return code >= 200 && code < 300;
            
        }
        
        /**
         * 
         * @param peTargets
         * @throws IOException Upon failure of process configuration. Attempt to stop the process is made prior to throwing this exception
         */
        private void setTargets(List<PlanElementBean.Target> peTargets) throws IOException {
        
            log("#"+id+": Received new target set (" + peTargets+"), configuring...");
            LinkedList<Target> seenTargets = new LinkedList<>();

            for (PlanElementBean.Target peTarget : peTargets) {

                Target target = new Target(peTarget.getTargetIp(), peTarget.getTargetPort());

                seenTargets.add(target);

                if (targetToIndexMap.containsKey(target)) {

                    // this target is already present; update its config
                    if (! targetToFormatMap.get(target).equals(peTarget.getFormat())) {
                        // The format has changed, update it

                        int index = targetToIndexMap.get(target);
                        String targetFormat = peTarget.getFormat();

                        String command = "port[" + index + "] compress " + targetFormat;
                        sendCommand(command);
                        targetToFormatMap.put(target, targetFormat);
                    }
                } else {
                    // the target is new; create a new port
                    String command = "root create-port " + peTarget.getTargetIp() + " " + peTarget.getTargetPort() + " " + peTarget.getFormat();
                    sendCommand(command);

                    targetToIndexMap.put(target, nextPortIndex++);
                    targetToFormatMap.put(target, peTarget.getFormat());
                }

            }

            // Remove unseen targets
            //   note: to maintain consistency, ths should be the only method allowed to remove targets!
            LinkedList<Target> pairsToRemove = new LinkedList<>();

            for (Map.Entry<Target, Integer> entry : targetToIndexMap.entrySet()) {
                Target currentTargetPair = entry.getKey();
                if (! seenTargets.contains(currentTargetPair)) {
                    pairsToRemove.add(currentTargetPair);
                }
            }

            for (Target pair : pairsToRemove) {
                // This target pair is obsolete, should be removed
                int index = targetToIndexMap.get(pair);
                sendCommand("root delete-port " + index);
                targetToIndexMap.remove(pair);
                targetToFormatMap.remove(pair);
                nextPortIndex--;

                // Decrement the indexes of other targets
                LinkedList<Target> pairsToDecrement = new LinkedList<>();
                for (Map.Entry<Target, Integer> entry : targetToIndexMap.entrySet()) {
                    if (entry.getValue() > index) {
                        pairsToDecrement.add(entry.getKey());
                    }
                }

                for (Target pairToRemove : pairsToDecrement) {
                    targetToIndexMap.put(pairToRemove, targetToIndexMap.get(pairToRemove)-1);
                }
            }
        }
        
        final public void stopApplication() {
            logger.debug("Stopping application " + process);
            if (process != null) process.destroy();

            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException | NullPointerException ex) {
            }    

            try {
                if (socket != null) socket.close();
            } catch (IOException ex) {
            }

            try {
                if (socketReader != null) socketReader.close();
            } catch (IOException ex) {
            }

            if (socketWriter != null) socketWriter.close();

            if (readerThread != null) readerThread.interrupt();
        }
    }
    
    public final int RESPONSE_TIMEOUT = 10000;
    public final String DUMMY_IP = "0.0.0.0";
    private final static String BUFFER_SIZE = "8M";
    
    public static final String UG_DEFAULT_PORT = "5020";
    
    private volatile RumHDProcessController processController;
    private int nextProcessControllerId = 0;
    // Initialized with application, so is destroyed
    
    public RumHDController(RumHD rumHD) {
        super(rumHD);
    }

    @Override
    public synchronized void stopApplication() {
        if (processController == null) return; // application is already stopped
        processController.stopApplication();
        processController = null;
    }

    @Override
    public synchronized void runApplication() throws IOException {
        // TODO: Mapovat porty jeste pred inicializaci aplikace
        String receivingPort = UG_DEFAULT_PORT;
        
        if (application != null && application.getPreferredReceivingPort() != null) {
            receivingPort = application.getPreferredReceivingPort();
        }
        
        runApplication(receivingPort);
    }
    
    public synchronized void runApplication(String sourcePort) throws IOException {
        if (processController != null) throw new IllegalStateException("Application " + application + " is already running!");
        
        processController = new RumHDProcessController(sourcePort, nextProcessControllerId++);
        try {
            processController.init();
        } catch (IOException ex) {
            System.err.println("Failed to start process: " + ex);
            processController.stopApplication();
            processController = null;
            throw ex;
        }
    }

    @Override
    public String toString() {
        return "Controller of "+application.getApplicationName();
    }
    
    @Override
    public synchronized void applyPatch(PlanElementBean patch) throws IOException {
        
        log("Applying patch:");
        log("  " + patch); 
        log("Current status = " + (processController==null?"not running":"running"));
        
        // check if the application is alive?
        if (patch == null) {
            stopApplication();
            log("Application " + application + " stopped.");
            return;
        }
        
        if (processController == null || ! patch.getSourcePort().equals(processController.sourcePort)) {
            logger.debug("The new patch introduces different receiving port for rum-hd! (current port=" +
                    (processController == null ? "null" : processController.sourcePort) +
                    ", new=" + 
                    patch.getSourcePort() + 
                    ")");
            stopApplication();
            runApplication(patch.getSourcePort());
            log("Application restarted with new source port " + patch.getSourcePort());
        }
        
        processController.setTargets(patch.getTargets());
    }

    @Override
    public boolean isRunning() {
        // TODO: implement
        throw new UnsupportedOperationException(this.getClass().getCanonicalName() + ": isRunning is not supported yet.");
    }
    
    @Deprecated
    private void log(String message) {
        logger.info(message);
    }
    
    // TODO: for debug only
    @Deprecated
    public void sendCommand(String command) throws IOException {
        processController.sendCommand(command);
    }

    /**
     * Forwarding input stream to output stream thread.
     * @deprecated Designated for debugging only
     */
    @Deprecated
    static class InputToOutputReader extends Thread {
        
        final BufferedReader in;
        final PrintStream out;
        final String caption;
        
        InputToOutputReader(BufferedReader in, PrintStream out, String caption) {
            setDaemon(true);
            this.in = in;
            this.out = out;
            this.caption = caption;
            System.out.println("NEw I->O reader thread created: " + caption);
        }
        
        @Override
        public void run() {
            System.out.println("I->O reader thread started: " + caption);
            try {
            
                while (true) {
                    String ln = in.readLine();
                    
                    if (ln == null) {
                        System.out.println("  >> " + caption + " " + "EOF");
                        break;
                    }
                    
                    System.out.println("  >> " + caption + " " + ln);
                }
            } catch (IOException ex) {
                logger.error(ex);
            }
        }
    }
}
