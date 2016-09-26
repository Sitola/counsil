package counsil;

import java.awt.EventQueue;
import java.awt.Font;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import wddman.OperatingSystem;
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
    
    /**
     * initial menu layout
     */
    InitialMenuLayout menu;
    
    /**
     * input json config
     */
    JSONObject inputConfig;

    public InitialMenu(File clientConfigurationFile) {
        try {
            wd = new WDDMan();            
        } catch (UnsupportedOperatingSystemException ex) {
            Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
         
        OperatingSystem os = wd.getRunningOperatingSystem();
        
        Font font;
        
        switch(os){            
            case WINDOWS:
            case MACOS:
                font =  new Font("Arial", 0, 16);
                break;
            case LINUX:
                font = new Font("Sans", 0, 16);
                break;
            default: 
                font = null;
        }
        //File this.clientConfigurationFile = clientConfigurationFile;//new File("configs/clientConfig.json");
        
        // create menu
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    menu = new InitialMenuLayout(centerPosition(), clientConfigurationFile, font);
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
     * Gets menu position from configuration file
     * @return menu position
     * @throws JSONException 
     */
    private Position centerPosition() throws JSONException {
        
        Position position = new Position(0,0);
        
        if(wd != null){
            try {
                position.x = (int) (wd.getScreenWidth() / 2);
                position.y = (int) (wd.getScreenHeight() / 2);
                
                if(position.x > 1600){
                    position.x = 960;
                }
                if(position.y > 800){
                    position.y = 540;
                }
                
            } catch (WDDManException ex) {
                Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return position;
    }

}
