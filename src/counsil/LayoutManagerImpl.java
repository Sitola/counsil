/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author desanka
 */
public class LayoutManagerImpl implements LayoutManager {
    
    List<PairWindowSourceInfo> windowsInfo;
    
    @Override public void LayoutManager(){
        windowsInfo = new ArrayList<>();
    }  
     
    @Override public void setActiveSource(PairWindowSourceInfo wsi){
        PairWindowSourceInfo toBeActivated = windowsInfo.get(windowsInfo.indexOf(wsi));
        toBeActivated.setActive();
    }
    
    @Override public void setDemandingSource(PairWindowSourceInfo wsi){
       PairWindowSourceInfo toBeDemanding = windowsInfo.get(windowsInfo.indexOf(wsi)); 
       toBeDemanding.setDemanding(); 
    }
    
    @Override public void unsetActiveSource(PairWindowSourceInfo wsi){
       PairWindowSourceInfo toBeDeactivated = windowsInfo.get(windowsInfo.indexOf(wsi)); 
       toBeDeactivated.unsetActive(); 
    }
    
    @Override public void unsetDemandingSource(PairWindowSourceInfo wsi){
       PairWindowSourceInfo toBeUndemanding = windowsInfo.get(windowsInfo.indexOf(wsi)); 
       toBeUndemanding.unsetDemanding(); 
    }

    @Override public String getConfiguration(){
        throw new UnsupportedOperationException("Not supported yet."); 
    }          //how is returned configuration, need for setConfiguration of getConfiguration is reed from outside and save somewhere
    @Override public void calculateNewLayout(){
        throw new UnsupportedOperationException("Not supported yet."); 
        // WDDman?
    }
    @Override public void listenLayoutUpdate(){
        throw new UnsupportedOperationException("Not supported yet."); 
        // communication with sessionmanager
    }
    @Override public void updateLayout(){
        throw new UnsupportedOperationException("Not supported yet."); 
        // change layout
    }
    @Override public void showLayout(){
        throw new UnsupportedOperationException("Not supported yet."); 
        // show it!
    }                   //how to show

  
    

}
