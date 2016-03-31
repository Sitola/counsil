/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

/**
 *
 * @author desanka
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

    /**
     * what to do when volume is muted
     * @param windowName
     */
    void muteActionPerformed(String windowName);
    
    /**
     * what to do when volume is increased
     * @param windowName
     */
    void volumeIncreasedActionPerformed(String windowName);
   
    /**
     * what to do when volume is decreased
     * @param windowName
     */
    void volumeDecreasedActionPerformed(String windowName);
    
    /**
     * what to do when volume is unmuted
     * @param windowName
     */
    void unmuteActionPerformed(String windowName);
}
