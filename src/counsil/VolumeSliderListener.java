/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

/**
 *
 * @author Desanka
 */
public interface VolumeSliderListener {
    
    /**
     * Sends new changed value to the listeners
     * @param newValue 
     */

    public void volumeChangeActionPerformed(int newValue);
}
