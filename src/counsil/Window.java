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
public class Window {
    
    private wddman.Window window;
    private Boolean visible;       

    public Window(wddman.Window window, Boolean visible) {
        
      this.visible = visible;
      this.window = window;          
    }    

    public wddman.Window getWindow() {
        return window;
    }

    public Boolean getVisible() {
        return visible;
    }
        
}

//is position in pixels or in % of screen ??? probably pixels
class Position{
    int x;
    int y;
    public Position(int x, int y){
        this.x = x;
        this.y = y;
    }
    public Position(){
        x = 0;
        y = 0;
    }

    
}