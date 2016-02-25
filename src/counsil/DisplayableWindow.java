/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import wddman.UnsupportedOperatingSystemException;
import wddman.WDDMan;
import wddman.WDDManException;

/**
 * Represents displayable window - pair of transparent and non-transparent
 * windows
 *
 * @author desanka
 */
class DisplayableWindow extends JFrame {

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

    /**
     * Window title
     */
    String title;

    /**
     * Initializes arguments, creates transparent window to non-transparent
     * window
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
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ex) {
                Logger.getLogger(DisplayableWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        position = new Position(content.getLeft(), content.getTop());
        width = content.getWidth();
        height = content.getHeight();

        this.title = title;
        this.role = role;
        talking = false;
        alerting = false;

    }

    /**
     * Applies DisplayableWindow parameters to actual windows
     *
     * @param wd wddman instance
     * @throws WDDManException
     */
    public void adjustWindow (WDDMan wd) throws WDDManException {

        System.out.print("[Window information]: " + title + " " + width + "x" + height + " [" + position.x + "," + position.y + "]\n");
        wd.getWindowByTitle(title).resize(position.x, position.y, width, height);
    }

    /**
     * Un/shows blue frame if was user chosen to speak
     */
    public void talk() {       
        talking = !talking;
    }

    /**
     * Un/shows red frame if user wants to speak
     */
    public void alert() {      
        alerting = !alerting;
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

    @Override
    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public Boolean getTalking(){
        return talking;
    }
    
    public Boolean getAlerting(){
        return alerting;
    }

    public String getRole() {
        return role;
    }
}
