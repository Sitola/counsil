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
    private final String role;

    /**
     * Window title
     */
    private final String title;

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
    }

    /**
     * Applies DisplayableWindow parameters to actual windows
     *
     * @param wd wddman instance
     * @throws WDDManException
     */
    public void adjustWindow (WDDMan wd) throws WDDManException {

        // System.out.print("[Window information]: " + title + " " + width + "x" + height + " [" + position.x + "," + position.y + "]\n");
        wd.getWindowByTitle(title).resize(position.x, position.y, width, height);
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

    public String getRole() {
        return role;
    }
    
    @Override
    public String getTitle(){
        return title;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + Objects.hashCode(this.role);
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
        if (!Objects.equals(this.role, other.role)) {
            return false;
        }
        return Objects.equals(this.title, other.title);
    }
    
    
}
