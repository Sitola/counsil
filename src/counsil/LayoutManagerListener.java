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
     * 
     */
    void alertActionPerformed();
    
    /**
     *  what to do when window is chosen
     * @param title of window
     */
    
    void windowChosenActionPerformed(String title);
}
