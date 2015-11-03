/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;
import wddman.UnsupportedOperatingSystemException;
import wddman.WDDMan;
import wddman.WDDManException;

/**
 * Represents displayable window - pair of transparent and non-transparent windows
 * @author desanka
 */
class DisplayableWindow {
    
    private int height;
    private int width;
    private Position position;
    public String role; 
    
    private final Window transparent;
    private final Window content;  
    
    private boolean talking;
    private boolean alerting;
    
    DisplayableWindow(String title, String role) throws WDDManException, UnsupportedOperatingSystemException{
        
        this.role = role;
        this.talking = false;
        this.alerting = false;
        this.transparent = new Window(title, position, height, width);
        this.content = new Window(title);     
        
        transparent.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (!role.equals("student")){
                     //! do something to get it to couniverse!
                }
            }
        });
        
        
    }
    
    public void adjustWindow(WDDMan wd) throws WDDManException{
        try {
            transparent.getWindow().resize(position.x, position.y, width, height);         
            content.getWindow().resize(position.x, position.y, width, height);
          
        } catch (WDDManException ex) {            
            Logger.getLogger(DisplayableWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public void talk(){
        if (talking){
            transparent.unshowFrame();         
        }
        else {
            alerting = false;            
            transparent.showFrame(Color.BLUE);
        }  
        talking = !talking;
        
    }
    
   
    public void alert(){
        if (alerting){            
            transparent.unshowFrame();
        }
        else {
            transparent.showFrame(Color.RED);            
        }
        alerting = !alerting;
        
    }   
    
    /**
     * Checks if DisplayableWindow is associated with argument wddman window
     * @param window
     * @return true, if Displayable window contains wddman window
     */
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

    public String getRole() {
        return role;
    }
   
    
}
