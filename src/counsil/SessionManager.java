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
public interface SessionManager {
    public void initCouniverse();
    public void listenMessages();
    public void updateCouniverse();
    public void addToSourceList(SourceInfo soi);
    public void addToSourceList(SourceInfo soi, int position);  //???? probably dont need this one
    public void removeFromSourceList(int position);
    public void removeFromSourceList(SourceInfo toRemove);
    public SourceInfo getFormSourceList();              //???? return last or first
    public SourceInfo getFormSourceList(int position);
    
    List<SourceInfo> soiList = new ArrayList<>();
}
