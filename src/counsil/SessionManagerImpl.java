/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.util.List;

/**
 *
 * @author xminarik
 */
public class SessionManagerImpl implements SessionManager{
    
    private List<SourceInfo> soiList;

    @Override
    public void initCouniverse() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        
    }

    @Override
    public void listenMessages() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    
    }

    @Override
    public void updateCouniverse() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    
    }

    @Override
    public boolean addToSourceList(SourceInfo soi) {
        return soiList.add(soi);
    }

    @Override
    public void addToSourceList(SourceInfo soi, int position) {
        soiList.add(position, soi);
    }

    @Override
    public SourceInfo removeFromSourceList(int position) {
        return soiList.remove(position);
    }

    @Override
    public boolean removeFromSourceList(SourceInfo toRemove) {
        return soiList.remove(toRemove);
    }

    @Override
    public SourceInfo getFormSourceList() {
        return soiList.get(soiList.size());
    }

    @Override
    public SourceInfo getFormSourceList(int position) {
        return soiList.get(position);
    }
    
}
