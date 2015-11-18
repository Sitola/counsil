/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.awt.Color;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import wddman.UnsupportedOperatingSystemException;
import wddman.WDDMan;
import wddman.WDDManException;

/**
 * Represents displayable window - pair of transparent and non-transparent windows
 * @author desanka
 */
class DisplayableWindow {
    
    
    /**
     * Height of window
     */
    private int height;

    /**
     * Width of window
     */
    private int width;
  
    /**
     * Position of winodw
     */
    private Position position;
    
    /**
     * Window role
     */
    public String role; 
       
    /**
     * True if window is currently talking, false otherwise
     */
    private boolean talking;
    
    /**
     * True if window wants to speak, false otherwise
     */
    private boolean alerting;
    
    wddman.Window content;
    JFrame transparent;
    
    
    /**
     * Initializes arguments, creates transparent window to non-transparent window
     * @param title title of new window
     * @param role role of window
     * @throws WDDManException
     * @throws UnsupportedOperatingSystemException 
     */
    DisplayableWindow(WDDMan wd, String title, String role) throws WDDManException, UnsupportedOperatingSystemException{
               
        content = wd.getWindowByTitle(title);
        
        position = new Position(content.getLeft(), content.getTop()); 
        width = content.getWidth();
        height = content.getHeight();
        
        System.out.println(width + " " + height + " " + position.x + " " + position.y);
                
        this.role = role;
        talking = false;
        alerting = false;
        
        transparent = new JFrame();
        transparent.setName(title + "TW");
                
        transparent.setLayout(new GridBagLayout());
        transparent.setUndecorated(true);        
        transparent.setBackground(new Color(0, 0, 0, (float) 0.0025)); 
        transparent.setAlwaysOnTop(true);
        transparent.setResizable(false);
        transparent.setSize(width, height);
        transparent.setLocationRelativeTo(null);
        transparent.setLocation(position.x, position.y);
        transparent.setVisible(true); //! false
        //! doesnt work transparent.setBounds(position.x, position.y, width, height);
                
        
    }
    
    /**
     * Applies DisplayableWindow parameters to actual windows
     * @param wd wddman instance
     * @throws WDDManException 
     */  
    public void adjustWindow(WDDMan wd) throws WDDManException{
        content.move(position.x, position.y);
        content.resize(position.x, position.y, width, height);
        
        
        transparent.setSize(width, height);
        //! doesnt work transparent.setBounds(position.x, position.y, width, height);
        transparent.setLocation(position.x, position.y);
    }
    
    /**
     * Un/shows blue frame if was user chosen to speak
     */
    public void talk(){
        if (talking){
           transparent.getRootPane().setBorder(BorderFactory.createEmptyBorder());           
        }
        else {
            alerting = false;            
            transparent.getRootPane().setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.BLUE));
        }  
        talking = !talking;
        
    }
    
    /**
     * Un/shows red frame if user wants to speak
     */
    public void alert(){
        if (alerting){            
            transparent.getRootPane().setBorder(BorderFactory.createEmptyBorder());   
        }
        else {
           transparent.getRootPane().setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.RED));          
        }
        alerting = !alerting;
        
    } 
    
    /**
     * Checks if DisplayableWindow is associated with argument wddman window
     * @param window
     * @return true, if Displayable window contains wddman window
     */
    public Boolean contains(wddman.Window window){
        return window == content;
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

    public wddman.Window getContent() {
        return content;
    }

    public JFrame getTransparent() {
        return transparent;
    }

    public String getRole() {
        return role;
    }
   
    
}
