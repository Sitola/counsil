package counsil;

/**
 * Listener of the GUI, listens for window clicks and menu interaction
 * @author xdaxner
 */
public interface LayoutManagerListener {
    
    /**
     * Listens for the alert action
     */
    void alertActionPerformed();
    
    /**
     * Listens for choosen window
     * @param windowName of window
     */    
    void windowChoosenActionPerformed(String windowName); 
}
