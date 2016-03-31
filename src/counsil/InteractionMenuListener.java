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
public interface InteractionMenuListener {
    
    /**
     * what to do when raise hand button is pushed
     */
    void raiseHandActionPerformed();

      /**
     * what to do when mute button is pushed
     */
    void muteActionPerformed();

      /**
     * what to do when unmute button is pushed
     */
    void unmuteActionPerformed();
    
     /**
     * what to do when volume is increased
     */
    void increaseActionPerformed();
    
     /**
     * what to do when volume is decreased
     */
    void decreaseActionPerformed();
    
}
