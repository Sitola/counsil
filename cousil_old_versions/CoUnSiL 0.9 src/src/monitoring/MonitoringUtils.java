package monitoring;

import net.jxta.peer.PeerID;

import java.net.*;

import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: Milos Liska (xliska@fi.muni.cz)
 * Date: 12.8.2007
 * Time: 20:26:19
 */
@Deprecated
public class MonitoringUtils {
    // this intentionally uses NetworkMonitor logger to avoid way too many loggers
    static Logger logger = Logger.getLogger(NetworkMonitor.class);

    /**
     * Checks whether a remote host is allive.
     * <p>
     *
     * @param hostname remote peer hostame
     * @param timeout ping timeout
     * @return true if a remote host responds to isReachable, false otherwise
     */
    public static boolean isAlive(String hostname, int timeout) {
        try {
            InetAddress remoteAddr = InetAddress.getByName(hostname);

            return remoteAddr.isReachable(timeout);
        } catch (Exception e) {
            if (NetworkMonitor.logger.isDebugEnabled()) {
                e.printStackTrace();
            }
            NetworkMonitor.logger.error("Failed to ping host " + hostname);
            return false;
        }
    }

    /**
     * Checks whether a remote host is allive.
     * <p>
     *
     * @param id remote peer JXTA peerID
     * @param timeout ping timeout
     * @return true if a remote host responds to isReachable, false otherwise
     */
    public static boolean isAlive(PeerID id, int timeout) {
        try {
            InetAddress remoteAddr = InetAddress.getByName(id.toURI().getHost());

            return remoteAddr.isReachable(timeout);
        } catch (Exception e) {
            if (NetworkMonitor.logger.isDebugEnabled()) {
                e.printStackTrace();
            }
            NetworkMonitor.logger.error("Failed to ping peer " + id.toString());

            return false;
        }
    }

    /**
     * Getter for local machine ingress.
     * <p>
     * @return ingress in Mbps
     */
    public static int getLocalIngress() {
        int ingress = 0;

        // TODO

        return ingress;
    }

    /**
     * Getter for local machine egress.
     * <p>
     * @return egress in Mbps
     */
    public static int getLocalEgress() {
        int egress = 0;

        // TODO

        return egress;
    }

    /**
     * Getter for local machine CPU usage
     * <p>
     * @return CPU usage in percents
     */
    public static int getLocalCpuLoad () {
        int load = 0;

        // TOOD

        return load;
    }
}
