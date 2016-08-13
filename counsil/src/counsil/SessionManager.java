package counsil;

import java.io.IOException;

/**
 * @author pkajaba, xdaxner
 */
public interface SessionManager {

    /**
     * Starts Couniverse core and counsil components
     * @throws IOException if it's not possible to start Couniverse core or
     * producer on local node
     * @throws InterruptedException if it's not possible to start Couniverse
     * core
     */
    public void initCounsil() throws IOException, InterruptedException;

    public void stopCounsil();

    public String getStatus();
}
