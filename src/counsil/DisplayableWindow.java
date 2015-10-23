/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.util.logging.Level;
import java.util.logging.Logger;
import wddman.WDDMan;
import wddman.WDDManException;

/**
 *
 * @author desanka
 */
class DisplayableWindow {
    
    private int height;
    private int width;
    private Position position;
    public String role; 
    
    private final Window transparent;
    private final Window content;    
    
    DisplayableWindow(wddman.Window window, String role){
        
        this.role = role;
        this.transparent = new Window(window, false);
        this.content = new Window(window, true);
    }
    
    public void adjustWindow(WDDMan wd) throws WDDManException{
        try {
            transparent.getWindow().resize(position.x, position.y, width, height);
            transparent.getWindow().move(position.x, position.y);
            
            content.getWindow().resize(position.x, position.y, width, height);
            content.getWindow().move(position.x, position.y);
        } catch (WDDManException ex) {            
            Logger.getLogger(DisplayableWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public Boolean contains(wddman.Window window){
        return window == content.getWindow();
    }
    
    Position getPosition(){
        return position;
    }
    
    void setPosition(Position position){
        this.position = position;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public Window getContent() {
        return content;
    }

    public Window getTransparent() {
        return transparent;
    }

    public void getRole(String role) {
        this.role = role;
    }
   
    
}
