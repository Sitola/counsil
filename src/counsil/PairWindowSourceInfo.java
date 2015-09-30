/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

/**
 *
 * @author xminarik
 */
public class PairWindowSourceInfo {
    public PairWindowSourceInfo(Window a, SourceInfo b){
        win = a;
        soi = b;
    }
    
    public Window getWindow(){
        return win;
    }
    
    public SourceInfo getSourceInfo(){
        return soi;
    }
    
    private final Window win;
    private final SourceInfo soi;
}
