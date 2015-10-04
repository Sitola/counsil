package utils;

/**
 * Interface for implementation of device-specific operations
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Time: 13:01:29
 */
public interface ProxyNodeInterface {

    /**
     * Initiates connection to the target device.
     * <p/>
     *
     * @return boolean - true if the connection was established successfully, false otherwise
     */
    boolean connect();

    /**
     * Disconnect from the device. The disconnection process should be as gracefull as possible at first,
     * but needs to make sure that it is disconnected even in pathological cases.
     */
    void disconnect();

    /**
     * Send command to the device and return the results of the command.
     * <p/>
     *
     * @param cmd command to be issued
     * @return String - value of the command
     */
    String send(String cmd);

    /**
     * Send keepalive command to the device.
     */
    void sendKeepAlive();

    /**
     * Send ping from the device to targetIP. This method is primarily to be used in monitoring.
     * <p/>
     *
     * @param targetIP - IP address (not hostname!) to be pinged
     * @return Double - RTT in milliseconds if node is reachable, -1 otherwise
     */
    Double sendPing(String targetIP);
    
}
