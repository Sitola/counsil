/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

/**
 *
 * @author xminarik
 */
public class Window{
    
    boolean demanding;
    boolean active;

    public Window() {
        demanding = false;
        active = false;
    }
    
    public void setActive(){
        active = true;
    }
    
    void unsetActive() {
        active = false;
    }

    void setDemanding() {
        demanding = true;
    }
    
    void unsetDemanding() {
        demanding = false;
    }
    
}