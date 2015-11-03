/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

/**
 * Represents structure which manipulates with layout
 * @author xminarik
 */
public interface LayoutManager {

    /**
     * Adds new node window to layout
     * @param title window title
     * @param role tole of window user
     */
    public void addNode(String title, String role);
    
    /**
     * Removes window from layout
     * @param title window title
     */
    public void removeNode(String title);

}
