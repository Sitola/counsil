package utils;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstraction for connection to the Polycom that is being proxied.
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 26.4.2009
 * Time: 0:52:16
 */
public abstract class ProxyNodeInterfacePolycom implements ProxyNodeInterface {
    /** Logger for the class  */
    protected static Logger logger = Logger.getLogger("utils");

    /** hostname or IP address to connect to */
    protected String targetHost;
    /** port to connect to  */
    protected int targetPort;
    /** password to use for connection */
    protected String password;
    /** socket to use for the connection */
    protected Socket hostSocket;
    /** input stream of the hostSocket */
    protected InputStream inputStream;
    /** output stream of the hostSocket */
    protected OutputStream outputStream;

    // the problem with prompt is that it can come also in the middle of the output sometimes :(
    /** Regex to match prompt */
    protected String prompt = "(\\x0d\\x0d\\x0a-> ?|\\x0d\\x0a-> ?$)";
    /** Compiled prompt pattern  */
    protected Pattern promptPattern = Pattern.compile(prompt);
    /** Regex to match password prompt */
    protected String passwordPrompt = "^.*Password:\\s*$";
    /** Compiled password prompt pattern */
    protected Pattern passwordPattern = Pattern.compile(passwordPrompt, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    /** String to disconnect gracefully from the Polycom */
    protected String disconnectString = "exit";
    /** Keep-alive string */
    protected String keepaliveString = "\n";
    /** String to generate ping command */
    protected String pingString = "ping";

    /** Charset used to convert bytes to string */
    public static String polycomCharset = "US-ASCII";

    /**
     * Method to append byte array to the string buffer. Uses polycomCharset to convert bytes to string.
     * <p/>
     *
     * @param sb stringbuffer to work on
     * @param array byte array to append
     * @param len lenght of the byte array to use for appending; not full length of the byte array needs to be appended
     */
    public static void appendByteArray(StringBuffer sb, byte[] array, int len) {
        if (len < 1) {
            return;
        }
        byte[] tmparray = new byte[len];
        System.arraycopy(array, 0, tmparray, 0, len);
        sb.append(new String(tmparray, Charset.forName(polycomCharset)));
    }

    /**
     * Return normalized version of the command string. This namely ensures that the command ends with two
     * newlines, which is required to get prompt back from the device.
     * <p/>
     *
     * @param cmd command to normalize
     * @return normalized command
     */
    protected String normalizeCommand(String cmd) {
        StringBuffer cmdBuffer = new StringBuffer(cmd);
        // we need to make sure the command ends with two newlines to get the prompt from Polycom
        while (!cmdBuffer.toString().endsWith("\n\n")) {
            cmdBuffer.append("\n");
        }
        return cmdBuffer.toString();
    }

    /**
     * Constructor to build connection to the device.
     * <p/>
     *
     * @param targetHost of type String - hostname or IP address of the Polycom to be controlled
     * @param targetPort of type int - TCP port to be connected to (usually port 24 for Polycoms)
     * @param password   of type String - admin password for the Polycom
     */
    public ProxyNodeInterfacePolycom(String targetHost, int targetPort, String password) {
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.password = password;
        hostSocket = new Socket();
    }

    /**
     * Reads the output of the previously issued command as long as prompt string is received and there is
     * nothing more to read. It replaces the prompt substring by an empty substring.
     * <p/>
     *
     * @return String - output of the command; it also includes the original command
     */
    private String getStringToPrompt() {
        StringBuffer output = new StringBuffer();
        byte[] buffer = new byte[1024];
        int readbytes;
        try {
            do {
                do {
                    readbytes = inputStream.read(buffer);
                    appendByteArray(output, buffer, readbytes);
                } while (inputStream.available() > 0);

                // we exit if either the prompt is matched or when end of stream is reached
            } while (!promptPattern.matcher(output.toString()).find() && readbytes != -1);
            Matcher m = promptPattern.matcher(output.toString());
            if (!m.find()) {
                logger.error("Unable to get prompt from remote host!");
                return "";
            } else {
                if (m.groupCount() > 1) {
                    logger.warn("Got more than one prompt from the remote host");
                }
                //noinspection UnnecessaryLocalVariable
                String result = m.replaceAll("");
                // System.out.println("final output:\n" + result);
                return result;
            }
        } catch (IOException e) {
            infoOnException(e, "Failed to read from input stream!");
            return "";
        }
    }

    /**
     * @inheritDocs
     */
    @Override
    public boolean connect() {
        // preconditions
        assert hostSocket != null;
        assert !hostSocket.isConnected();

        InetSocketAddress addr = new InetSocketAddress(targetHost, targetPort);
        try {
            hostSocket.connect(addr);
            hostSocket.setOOBInline(false);
        } catch (IOException e) {
            infoOnException(e, "Failed to connect host " + targetHost + ":" + targetPort);
            return false;
        }
        try {
            inputStream = hostSocket.getInputStream();
            outputStream = hostSocket.getOutputStream();
        } catch (IOException e) {
            infoOnException(e, "Failed to get input or output stream! " + e.getMessage());
        }

        StringBuffer output = new StringBuffer();
        byte[] buffer = new byte[1024];
        int readbytes = 0;
        do {
            try {
                readbytes = inputStream.read(buffer);
                appendByteArray(output, buffer, readbytes);

            } catch (IOException e) {
                infoOnException(e, "Failed to read from input stream!");
            }
        } while (!passwordPattern.matcher(output.toString()).matches() && !(readbytes == -1));
        if (!passwordPattern.matcher(output.toString()).matches()) {
            logger.error("Unable to get password prompt from remote host!");
            return false;
        }
        try {
            outputStream.write((normalizeCommand(password)).getBytes(Charset.forName(polycomCharset)));
        } catch (IOException e) {
            infoOnException(e, "Failed to write to socket");
        }

        getStringToPrompt();
        return true;
    }

    /**
     * @inheritDocs
     */
    @Override
    public void disconnect() {
        // preconditions
        assert outputStream != null;
        assert hostSocket != null;
        assert hostSocket.isConnected();
        assert !hostSocket.isClosed();
        assert !hostSocket.isInputShutdown();
        assert !hostSocket.isOutputShutdown();

        try {
            outputStream.write(normalizeCommand(disconnectString).getBytes());
            hostSocket.shutdownInput();
            hostSocket.shutdownOutput();
            inputStream.close();
            outputStream.close();
            hostSocket.close();
        } catch (IOException e) {
            infoOnException(e, "Disconnecting from host " + targetHost + ":" + targetPort + " failed!");
        }
    }

    /**
     * @inheritDocs
     */
    @Override
    public String send(String cmd) {
        assert hostSocket != null;
        assert hostSocket.isConnected();
        assert outputStream != null;
        assert !hostSocket.isOutputShutdown();
        try {
            outputStream.write(normalizeCommand(cmd).getBytes());
            return getStringToPrompt();
        } catch (IOException e) {
            infoOnException(e, "Failed to write to socket");
            return "";
        }
    }

    /**
     * @inheritDocs
     */
    @Override
    public void sendKeepAlive() {
        send(keepaliveString);
    }


    /**
     * Process exception based on settings of the logger.
     * <p/>
     *
     * @param e of type Throwable - exception to be processed
     * @param s of type String - information on the context of the exception
     */
    protected static void infoOnException(Throwable e, String s) {
        if (logger.isDebugEnabled()) {
            e.printStackTrace();
        }
        logger.error(s + " " + e.getMessage());
    }
}