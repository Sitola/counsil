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
     * restart consumer application by title
     * @param title 
     */
    public void windowRestartActionPerformed(String title);
}
