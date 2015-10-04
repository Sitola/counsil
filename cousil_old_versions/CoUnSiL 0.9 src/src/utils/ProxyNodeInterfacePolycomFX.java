package utils;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstraction for connection to the Polycom FX that is being proxied
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 25.4.2009
 * Time: 18:08:32
 */
public class ProxyNodeInterfacePolycomFX extends ProxyNodeInterfacePolycom {

    /**
     * Constructor to build connection to the device.
     * <p/>
     *
     * @param targetHost of type String - hostname or IP address of the Polycom to be controlled
     * @param targetPort of type int - TCP port to be connected to (usually port 24 for Polycoms)
     * @param password   of type String - admin password for the Polycom
     */
    public ProxyNodeInterfacePolycomFX(String targetHost, int targetPort, String password) {
        super(targetHost, targetPort, password);

        prompt = "(\\x0d\\x0a-> ?|\\x0d\\x0a-> ?$)";
        promptPattern = Pattern.compile(prompt);
        passwordPrompt = "^.*Password:.*$";
        passwordPattern = Pattern.compile(passwordPrompt, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    }

    /**
     * @inheritDocs
     */
    @Override
    protected String normalizeCommand(String cmd) {
        StringBuffer cmdBuffer = new StringBuffer(cmd);
        // we need to make sure the command ends with two newlines to get the prompt from Polycom
        while (!cmdBuffer.toString().endsWith("\n\n")) {
            cmdBuffer.append("\n");
        }
        cmdBuffer.append("\r\n");
        return cmdBuffer.toString();
    }

    /**
     * @inheritDocs
     */
    @Override
    public Double sendPing(String targetIP) {
        String pingSucceeded = "testlan ping " + targetIP + ": passed = 1; failed = 0";
        Pattern pingSucceededPattern = Pattern.compile(pingSucceeded);
        String pingFailed = "testlan ping " + targetIP + ": passed = 0; failed = 1";
        Pattern pingFailedPattern = Pattern.compile(pingFailed);

        String output = send(pingString + " " + targetIP);
        Matcher succeededMatcher = pingSucceededPattern.matcher(output);
        Matcher failedMatcher = pingFailedPattern.matcher(output);
        if (!succeededMatcher.find() && !failedMatcher.find()) {
            int readbytes;
            byte[] buffer = new byte[1024];
            StringBuffer outputsb = new StringBuffer(output);
            do {
            try {
                readbytes = inputStream.read(buffer);
                appendByteArray(outputsb, buffer, readbytes);
            } catch (IOException e) {
                infoOnException(e, "Failed to read from input stream!");
                return -1.0d;
            }
            } while (!pingSucceededPattern.matcher(outputsb.toString()).find() && !pingFailedPattern.matcher(outputsb.toString()).find());

            output = outputsb.toString();
            succeededMatcher = pingSucceededPattern.matcher(output);
            failedMatcher = pingFailedPattern.matcher(output);
        }
        succeededMatcher.reset();
        failedMatcher.reset();
        if (succeededMatcher.find()) {
            // XXX
            return 100d;
        }
        else if (failedMatcher.find()) {
            return -1.0d;
        }
        logger.error("Unknown format of ping response!");
        return -1.0d;
    }

}