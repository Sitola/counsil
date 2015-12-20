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
     * @param title of window
     */
    
    void windowChosenActionPerformed(String title);

    /**
     * what to do when volume is muted
     */
    void muteActionPerformed();
    
    /**
     * what to do when volume is increased
     */
    void volumeIncreasedActionPerformed();
   
    /**
     * what to do when volume is decreased
     */
    void volumeDecreasedActionPerformed();
    
    /**
     * what to do when volume is unmuted
     */
    void unmuteActionPerformed();

}
