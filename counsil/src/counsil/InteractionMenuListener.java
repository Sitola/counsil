package counsil;

/**
 * Listener of the interaction menu
 * @author xdaxner
 */
public interface InteractionMenuListener {
    
    /**
     * Listens for alert
     */
    void alertActionPerformed();
    
    
    /**
     * Listens for layout refresh 
     */
    void refreshActionPerformed();

    /**
     * Listens for save layout action
     * @param filePath path to the new layout file
     * @param useLayout true, if layout should be used in current run
     */
    void saveLayoutActionPerformed(String filePath, Boolean useLayout);
}
