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
 * Represents universal window
 * @author desanka
 * 
 */
public class Window extends JFrame{
    
    /**
     * wddman window 
     */
    private wddman.Window window; 
    
    /**
     * Creates transparent window according to given attributes
     * @param title title of paired non-transparent window
     * @param position position of window
     * @param height height of window
     * @param width width of window
     * @throws WDDManException
     * @throws UnsupportedOperatingSystemException 
     */
    Window(String title, Position position, int height, int width) throws WDDManException, UnsupportedOperatingSystemException{
        
        super(title + "TW");
        
        pairWddmanWindow(title + "TW");
                
        setLayout(new GridBagLayout());
        setUndecorated(true);
        
        setBackground(new Color(0, 0, 0, (float) 0.0025)); 
        setAlwaysOnTop(true);
        setResizable(false);

        setSize(width, height);
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    /**
     * Creates non-transparent window
     * @param title title of wwdman window
     * @throws UnsupportedOperatingSystemException
     * @throws WDDManException 
     */
    Window(String title) throws UnsupportedOperatingSystemException, WDDManException {
        pairWddmanWindow(title);
    }
    
    /**
     * Shows color frame on transparent window
     * @param color 
     */
    public void showFrame(Color color){
        getRootPane().setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, color));   
    }
    
    /**
     * Unshows current frame
     */
    public void unshowFrame(){
         getRootPane().setBorder(BorderFactory.createEmptyBorder());         
    }

    public Window(wddman.Window window) {      
        this.window = window;          
    }    

    public wddman.Window getWindow() {
        return window;
    }

    /**
     * Pairs wddman window according to title
     * @param title window title 
     * @throws UnsupportedOperatingSystemException
     * @throws WDDManException 
     */
    private void pairWddmanWindow(String title) throws UnsupportedOperatingSystemException, WDDManException {
        
        WDDMan wd = new WDDMan();
        window = wd.getWindowByTitle(title);
        
    }
        
}

/**
 * Represents x,y position on screen
 * @author xminarik
 */

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