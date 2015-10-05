package utils;

import org.apache.log4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstraction for connection to the actual node that is being proxied
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 8.4.2009
 * Time: 9:27:01
 */
public class ProxyNodeConnection {
    protected static Logger logger = Logger.getLogger(ProxyNodeConnection.class);

    private boolean active = false;

    public class ProxyNodeConnectionFailed extends Exception {
    }
    public class ProxyNodeCommandFailed extends Exception {
    }

    public static String PolycomHDXType = "PolycomHDX";
    public static String PolycomFXType = "PolycomFX";
    public static String PolycomFakeType = "PolycomFake";

    private ProxyNodeInterface proxyNodeInterface;

    public ProxyNodeConnection() {
        this.proxyNodeInterface = null;
    }

    public ProxyNodeConnection(ProxyNodeInterface proxyNodeInterface) {
        this.proxyNodeInterface = proxyNodeInterface;
    }

    /**
     * Types of commands issuable through ProxyCommand
     */
    protected enum ProxyCommandType {
        GENERIC, PING, KEEPALIVE
    }

    /**
     * Class to issue commands through ProxyNodeConnection and get the results. Output is returned as
     * general object and it is responsibility of the caller to know what should be returned and cast
     * result into appropriate type.
     *
     * TODO: make this generic w.r.t. returned object
     */
    protected class ProxyCommand {
        ProxyCommandType type; // type of the command to perform
        String cmd; // command to perform
        Object output; // output of the command to be returned to the caller

        /**
         * Construct the command to be performed.
         * <p/>
         *
         * @param type type of the command
         * @param cmd the command string
         */
        public ProxyCommand(ProxyCommandType type, String cmd) {
            this.type = type;
            this.cmd = cmd;
        }

        /**
         * Get the proxy command type
         * <p/>
         *
         * @return proxy command type
         */
        public ProxyCommandType getProxyCommandType() {
            return type;
        }

        /**
         * Get the command to be performed
         * <p/>
         *
         * @return command to be performed
         */
        public String getCmd() {
            return cmd;
        }

        /**
         * Get the output of the command that was performed.
         * <p/>
         *
         * @return output of the performed command
         */
        public synchronized Object getOutput() {
            return output;
        }

        /**
         * Set output of the performed command
         * <p/>
         *
         * @param output object containing output of the command
         */
        public synchronized void setOutput(Object output) {
            this.output = output;
        }
    }

    /**
     * Thread that maintains connection to the proxied device.
     */
    protected class ConnectionThread extends Thread {
        AtomicBoolean terminateFlag = new AtomicBoolean(false);
        LinkedBlockingQueue<ProxyCommand> proxyCommands = new LinkedBlockingQueue<ProxyCommand>(10);

        /**
         * Request ConnectionThread to terminate.
         */
        public void finish() {
            terminateFlag.set(true);
        }

        /**
         * Sends command to the device and returns results. 
         * @param type type of the issued command
         * @param cmd string of the command
         * @return object returning results comming from issuing the command; caller is responsible to cast the object into correct type
         * @throws ProxyNodeCommandFailed this exception is thrown when the command queue is full for more than timeout (currently 60s)
         */
        public Object sendCommand(ProxyCommandType type, String cmd) throws ProxyNodeCommandFailed {
            ProxyCommand proxyCommand = new ProxyCommand(type, cmd);
            synchronized (proxyCommand) {
                try {
                    if (!proxyCommands.offer(proxyCommand, 60, TimeUnit.SECONDS)) {
                        throw new ProxyNodeCommandFailed();
                    }
                } catch (InterruptedException e1) {
                    throw new ProxyNodeCommandFailed();
                }
                try {
                    // ConnectionThread should notify us
                    proxyCommand.wait();
                } catch (InterruptedException e) {
                }
            }
            return proxyCommand.getOutput();
        }

        public void run() {
            logger.debug("Establishing connection");
            proxyNodeInterface.connect();
            logger.info("Connection established");
            while (!terminateFlag.get()) {
                ProxyCommand proxyCommand = null;
                try {
                    proxyCommand = proxyCommands.poll(60000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    // test that we were not interrupted to finish
                    if (terminateFlag.get()) {
                        logger.trace("I should terminate, exiting command loop.");
                        break;
                    }
                }
                if (proxyCommand != null) {
                    synchronized (proxyCommand) {
                        switch (proxyCommand.getProxyCommandType()) {
                            case GENERIC:
                                logger.debug("Sending generic command: " + proxyCommand.getCmd());
                                proxyCommand.setOutput(proxyNodeInterface.send(proxyCommand.getCmd()));
                                logger.debug("Sending generic command output is: " + proxyCommand.getOutput());
                                break;
                            case PING:
                                logger.debug("Sending ping command: " + proxyCommand.getCmd());
                                proxyCommand.setOutput(proxyNodeInterface.sendPing(proxyCommand.getCmd()));
                                logger.debug("Sending ping command output is: " + proxyCommand.getOutput());
                                break;
                            case KEEPALIVE:
                                logger.debug("Sending explicit keepalive command");
                                proxyNodeInterface.sendKeepAlive();
                                break;
                        }
                        proxyCommand.notifyAll();
                    }
                }
                else {
                    logger.debug("Sending implicit keepalive command");
                    proxyNodeInterface.sendKeepAlive();
                }
                logger.trace("Command done");
            }
            logger.debug("Disconnecting");
            proxyNodeInterface.disconnect();
            logger.info("Disconnected");
        }

    }

    ConnectionThread connectionThread;

    public synchronized void start() {
        assert connectionThread == null;

        connectionThread = new ConnectionThread();
        connectionThread.start();

        active = true;
    }

    public synchronized void stop() {
        assert connectionThread != null;
        connectionThread.finish();
        connectionThread.interrupt();
        try {
            connectionThread.join();
        } catch (InterruptedException e) {
            logger.warn("Failed to join connection thread! " + e.getMessage());
        }
        active = false;
    }

    public synchronized boolean isActive() {
        return active;
    }

    public String send(String cmd) throws ProxyNodeCommandFailed {
        return (String) connectionThread.sendCommand(ProxyCommandType.GENERIC, cmd);
    }

    public Double sendPing(String targetIP) throws ProxyNodeCommandFailed {
        return (Double) connectionThread.sendCommand(ProxyCommandType.PING, targetIP);
    }


    protected static void infoOnException(Throwable e, String s) {
        if (logger.isDebugEnabled()) {
            e.printStackTrace();
        }
        logger.error(s + " " + e.getMessage());
    }

}
