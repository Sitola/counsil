/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package appControllers;

import static appControllers.ControllerImpl.logger;
import edu.emory.mathcs.backport.java.util.Arrays;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import mediaAppFactory.MediaApplication;
import mediaAppFactory.MediaApplicationConsumer;
import mediaAppFactory.MediaApplicationProducer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import p2p.CoUniverseMessage;
import p2p.MessageType;
import p2p.NodeGroupIdentifier;

/**
 *
 * @author maara
 */
public abstract class UltraGridController extends ControllerImpl {

    public final int RESPONSE_TIMEOUT = 1000;
    public final String DUMMY_IP = "0.0.0.0";
    
    private ServerSocket serverSocket;
    private Socket socket;
    private BufferedReader socketReader;
    private PrintWriter socketWriter;

    private ReaderThread readerThread;
    private ProcessGuardThread processGuardThread;
    
    protected final AtomicBoolean exited = new AtomicBoolean(false); // definitely turned off. Just wait for threads to note
    protected final AtomicBoolean isActive = new AtomicBoolean(false); // Application is inactive iff it is on stand-by, sending data nowhere
    
    LinkedBlockingDeque<String> messageDeque = new LinkedBlockingDeque<String>();

    private final Object changePropertyLock = new Object();

    public static final int UGControlPort = 5054;
    
    /**
     * Guard thread. Simply wait for the application to terminate, then close the socket.
     */
    private class ProcessGuardThread extends Thread {
        public void run() {
            boolean stop = false;
            while (! stop) {
                try {
                    process.waitFor();
                    stop = true;
                } catch (InterruptedException e) {
                }
            }
            try {
                serverSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private class ReaderThread extends Thread {

        private String stats, response;
        
        private long data = 0, received = 0, loss = 0;
        
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
            
            if (myLoss != 0 && isActive.get()) {
                reportDataLoss(myData - data, myLoss - loss);
            }
            
            data = myData;
            received = myReceived;
            loss = myLoss;
        }

        public void run() {
            String line = null;
            try {

                // TODO: HACK - remove the try block
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {}
                
                while (!exited.get()) {
                    if (!socketReader.ready()) {
                        Thread.sleep(100);
                        continue;
                    }
                        
                    while ((line = socketReader.readLine()) != null) {

                        reportMessageReception(line);
                        
                        if (!line.startsWith("stats")) {                                    
                            //controllerFrame.writeMessage(line, UltraGridController.this);
                            messageDeque.offer(line);
                        } else {
//                                    controllerFrame.writeMessage("On the fly statistics: " + line, UltraGridController.this);
                            processStatLine(line);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            synchronized(this) {
                exited.set(true);
                notifyAll();
            }
        }
    }

    public UltraGridController(MediaApplication application) {
        super(application);
    }
    
    private void reportDataLoss(long data, long loss) {
        reportPacketLoss(new long[]{data, loss});
    }
    
    protected boolean change(String what) throws IOException {
        synchronized(changePropertyLock) {

            if (what == null) {

                reportSendingCommand(DUMMY_IP);
                socketWriter.println(DUMMY_IP);
                
            } else {
                
                reportSendingCommand(what);
                socketWriter.println(what);
            }
            
            String line = null;
            while(true) {
                try {
                    line = messageDeque.poll(RESPONSE_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS);
                    break;
                } catch (InterruptedException ex) {
                    Logger.getLogger(UltraGridController.class.getName()).log(Level.FATAL, null, ex);
                }
            }
            
            if (line == null) {
                throw new IOException("Socket closed.");
            }

            Logger.getLogger(this.getClass()).info("CoUniverse request: " + what);
            Logger.getLogger(this.getClass()).info("UltraGrid reply: " + line);

            long code = Long.parseLong(line.split(" ")[0]);
            if(code >= 200 && code < 300) {
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public void runApplication() throws IOException {
        
        Runtime runtime = Runtime.getRuntime();
        serverSocket = new ServerSocket(0);
        
        String command = "" +
                application.getApplicationPath() + " " +
                application.getApplicationCmdOptions() + " " +
                "--control-port" + " " +
                (String.valueOf(serverSocket.getLocalPort())) + ":1" + " ";
        
        if (this.application instanceof MediaApplicationProducer) command += " " + DUMMY_IP;
        
        logger.info("Executing command: "+command);
        
        LinkedList<String> argList = new LinkedList<>();
        argList.add(application.getApplicationPath());
        argList.addAll(Arrays.asList(application.getApplicationCmdOptions().split("\\s+")));
        argList.add("--control-port");
        argList.add(String.valueOf(serverSocket.getLocalPort()) + ":1");
        if (this.application instanceof MediaApplicationProducer) argList.add(DUMMY_IP);
        
        ProcessBuilder pb = new ProcessBuilder(argList);

        // TODO: Linux only!?!
//        pb.redirectOutput(new File("/dev/null"));
//        pb.redirectError(new File("/dev/null"));

        process = pb.start();

        (new RumHDController.InputToOutputReader(new BufferedReader(new InputStreamReader(process.getInputStream())), System.out, application.getApplicationName() + ": out:")).start();
        (new RumHDController.InputToOutputReader(new BufferedReader(new InputStreamReader(process.getErrorStream())), System.out, application.getApplicationName() + ": err:")).start();
        
        // TODO: remember the hook, and unregister it upon exit of UG
        runtime.addShutdownHook(new Thread() {
        
            Process process;
            
            public Thread setProcess(Process process) {
                this.process = process;
                return this;
            }
            
            @Override
            public void run() {
                process.destroy();
            }
        
        }.setProcess(process));
        
//        readers.attachStderr(process.getErrorStream());
//        readers.attachStdout(process.getInputStream());
        
        processGuardThread = new ProcessGuardThread();
        processGuardThread.start();
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
        }
       
        System.out.println("UltraGrid process crerated: " + process);
        System.out.println("Accepting connection from UltrsGrid at port " + serverSocket.getLocalPort());
        
        socket = serverSocket.accept();
        socketReader = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
        socketWriter = new PrintWriter(
                socket.getOutputStream(), true);

        readerThread = new ReaderThread();
        readerThread.start();
        
//        controllerFrame.addControllerWriter(this, process);
    }

    @Override
    public void stopApplication() {
        readers.detachStdout();
        readers.detachStderr();
        
        ///controllerFrame.removeControllerWriter(this);
        if (process != null) {
            process.destroy();
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        exited.set(true);
    }
    
    

    @Override
    public String toString() {
        return "Controller of "+application.getApplicationName() + " (" + application.getUuid() + ")";
    }
    
    @Override
    public boolean isRunning() {
        return isActive.get();
    }
}
