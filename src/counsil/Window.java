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
 *
 * @author xminarik, desanka
 * 
 */
public class Window extends JFrame{
    
    private wddman.Window window; 
    
    public Window(String title, Position position, int height, int width) throws WDDManException, UnsupportedOperatingSystemException{
        
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
    
    public void showFrame(Color color){
        getRootPane().setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, color));   
    }
    
    public void unshowFrame(){
         getRootPane().setBorder(BorderFactory.createEmptyBorder());         
    }

    public Window(wddman.Window window) {      
        this.window = window;          
    }    

    public wddman.Window getWindow() {
        return window;
    }

    private void pairWddmanWindow(String title) throws UnsupportedOperatingSystemException, WDDManException {
        
        WDDMan wd = new WDDMan();
        window = wd.getWindowByTitle(title);
        
    }
        
}


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