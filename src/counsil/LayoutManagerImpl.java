/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;
import wddman.UnsupportedOperatingSystemException;
import wddman.WDDMan;
import wddman.WDDManException;

/**
 *
 * @author desanka
 */
public class LayoutManagerImpl implements LayoutManager {
    
    List<DisplayableWindow> windows;
    private WDDMan wd;
    
    /*
    * recalculate new layout from JSON layout file and array of nodes or something with specify role 
    * return layout with position nad id|name 
    * !!!!!!!not finished!!!!!!!
    */
    private Layout recalculate(JSONObject input, List<Window> winsToPlace){
        if(input == null){
            return null;
        }
        Layout newLayout = new LayoutImpl();
        Map<String, Integer> numRoles;
        numRoles = new HashMap();
        //TO DO: here set all roles from input (mabe some checking if role have set size of screen, not obligatory)
        
        //for each role in input count how many windows is to distribute
        for (Window win : winsToPlace) {
            if(numRoles.containsKey(win.role)){
                Integer intIncrem = numRoles.get(win.role);
                intIncrem++;
                numRoles.replace(win.role, intIncrem);
            }
        }
        
        return new LayoutImpl();
    }
    
    @Override 
    public void LayoutManager(){
        
        windows = new ArrayList<>();
        
        try {
            wd = new WDDMan();
        } catch (UnsupportedOperatingSystemException ex) {
            Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }  
     
   
    @Override
    public void applyChanges(){
        for (DisplayableWindow window : windows){
            try {
                window.adjustWindow(wd);
            } catch (WDDManException ex) {
                Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    @Override
    public void addToLayout(String title, String role){
       
       
       //! todo
        
    }
    
    @Override
    public void removeFromLayout(String title){
        //! todo
    }
   



}
