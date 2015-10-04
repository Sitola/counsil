package utils;

/**
 * Abstraction for connection to a Polycom that is being proxied, but can't be conctacted really.
 * This class is useful for Polycom consumers, that don't need to be contacted, but their proxy
 * node is required to make scheduling work.
 * <p/>
 * 
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 27.4.2009
 * Time: 19:32:40
 */
public class ProxyNodeInterfacePolycomFake extends ProxyNodeInterfacePolycom {

    /**
     * Constructor to build connection to the device.
     * <p/>
     *
     * @param targetHost of type String - hostname or IP address of the Polycom to be controlled
     * @param targetPort of type int - TCP port to be connected to (usually port 24 for Polycoms)
     * @param password   of type String - admin password for the Polycom
     */
    public ProxyNodeInterfacePolycomFake(String targetHost, int targetPort, String password) {
        super(targetHost, targetPort, password);
    }

    /**
     * @inheritDocs
     */
    @Override
    public boolean connect() {
        logger.debug("Doing fake connect");
        return true;
    }

    /**
     * @inheritDocs
     */
    @Override
    public void disconnect() {
        logger.debug("Doing fake disconnect");
    }

    /**
     * @inheritDocs
     */
    @Override
    public String send(String cmd) {
        logger.debug("Doing fake command: " + cmd);
        return "";
    }

    /**
     * @inheritDocs
     */
    @Override
    public void sendKeepAlive() {
        logger.debug("Sending fake keep-alive.");
    }

    /**
     * @inheritDocs
     */
    @Override
    public Double sendPing(String targetIP) {
        logger.debug("Sending fake node ping response.");
        return 100.0d;
    }


}
