/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import wddman.UnsupportedOperatingSystemException;
import wddman.WDDMan;
import wddman.WDDManException;

/**
 *
 * @author xminarik
 */
public class InitialMenu {
        
    /**
     * Instance of wddman
     */
    private WDDMan wd;
    
    InitialMenuLayout menu;
    
    JSONObject inputConfig;

    public InitialMenu() {
        try {
            wd = new WDDMan();            
        } catch (UnsupportedOperatingSystemException ex) {
            Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
            String entireFileText = new Scanner(new File("clientConfig.json")).useDelimiter("\\A").next();
            inputConfig = new JSONObject(entireFileText);
        } catch (FileNotFoundException | JSONException ex) {
            Logger.getLogger(InitialMenu.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // create menu
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    menu = new InitialMenuLayout(centerPosition(), inputConfig);
                } catch (JSONException ex) {
                    Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }
    
    public InitialMenuLayout getInitialMenuLayout(){
        return menu;
    }
    
    /**
     * Gets menu position from configure file
     * @return menu position
     * @throws JSONException 
     */
    private Position centerPosition() throws JSONException {
        Position position = new Position();
        if(wd == null){
            position.x = 0;
            position.y = 0;
        }else{
            try {
                position.x = (int) (wd.getScreenWidth() / 2);
                position.y = (int) (wd.getScreenHeight() / 2);
                //150 is set by menu creating in InteractionMenu.java if change have to update here also
                //500 is set by menu creating in InteractionMenu.java if change have to update here also
            } catch (WDDManException ex) {
                Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return position;
    }

}
