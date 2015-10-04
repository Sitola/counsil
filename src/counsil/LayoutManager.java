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
 * @author xminarik
 */
public interface LayoutManager {
    public void LayoutManager();
    
    public void setActiveSource(PairWindowSourceInfo wsi);
    public void setDemandingSource(PairWindowSourceInfo wsi);    
    public void unsetActiveSource(PairWindowSourceInfo wsi);
    public void unsetDemandingSource(PairWindowSourceInfo wsi);
    public String getConfiguration();           //how is returned configuration, need for setConfiguration of getConfiguration is reed from outside and save somewhere
    public void calculateNewLayout();
    public void listenLayoutUpdate();
    public void updateLayout();
    public void showLayout();                   //how to show
    

}
