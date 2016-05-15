/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import wddman.UnsupportedOperatingSystemException;
import wddman.WDDMan;
import wddman.WDDManException;

/**
 * Represents window attributes * 
 * @author desanka
 */
class DisplayableWindow{

    /**
     * Height of window
     */
    private int height;

    /**
     * Width of window
     */
    private int width;

    /**
     * Position of winodow
     */
    private Position position;


    
    /**
     * Window temporary role in layout
     */
    private String role;
    
    
    private final String defaultRole;

    /**
     * Window title
     */
    private final String title;

    /**
     * Initializes arguments    
     *
     * @param title title of new window
     * @param role role of window
     * @throws WDDManException
     * @throws UnsupportedOperatingSystemException
     */
    DisplayableWindow(WDDMan wd, String title, String role) throws WDDManException, UnsupportedOperatingSystemException {
        
        wddman.Window content;        
        while ((content = wd.getWindowByTitle(title)) == null) {          
            try {
                TimeUnit.MILLISECONDS.sleep(3); 
            } catch (InterruptedException ex) {
                Logger.getLogger(DisplayableWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        position = new Position(content.getLeft(), content.getTop());
        width = content.getWidth();
        height = content.getHeight();

        this.title = title;    
        this.role = role;
        defaultRole = role;
    }

    /**
     * Applies DisplayableWindow parameters to actual windows
     *
     * @param wd wddman instance
     * @throws WDDManException
     */
    public void adjustWindow (WDDMan wd) throws WDDManException {

        //System.out.print("[Window information]: " + title + " " + width + "x" + height + " [" + position.x + "," + position.y + "]\n");
        //System.out.print("[Role information]: Default role - " + defaultRole + ", current role - " + role + "\n");
        try {
            TimeUnit.SECONDS.sleep(1);

            wddman.Window win = wd.getWindowByTitle(title);
            if (win != null) {
                win.resize(position.x, position.y, width, height);
            } else {
                TimeUnit.SECONDS.sleep(5);
                win = wd.getWindowByTitle(title);
                if (win != null) {
                    win.resize(position.x, position.y, width, height);
                }
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(DisplayableWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
               
    }

    /**
     * Checks if DisplayableWindow is associated with argument title
     *
     * @param title
     * @return true, if Displayable window contains wddman window
     */
    public Boolean contains(String title) {
        return title.equals(this.title);

    }

    Position getPosition() {
        return position;
    }

    void setPosition(Position position) {
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
  
    
    public  String getRole (){
        return role;
    }
  
    public String getTitle(){
        return title;
    }
    
    public void setRole (String role){
        this.role = role;
    }

    @Override
    public int hashCode() {
        int hash = 5;      
        hash = 53 * hash + Objects.hashCode(this.title);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DisplayableWindow other = (DisplayableWindow) obj;  
        return Objects.equals(this.title, other.title);
    }
    
    String getDefaultRole(){
        return defaultRole;
    }

    
}
