package counsil;

/**
 * @author xdaxner
 */
public interface LayoutManagerListener {
    
    /**
     * what to do when user wants to talk
     */
    void alertActionPerformed();
    
    /**
     *  what to do when window is chosen
     * @param windowName of window
     */
    
    void windowChoosenActionPerformed(String windowName); 
}
