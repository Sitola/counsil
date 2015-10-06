/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import netscape.javascript.JSObject;

/**
 *
 * @author xminarik
 */
public class SourceInfoImpl implements SourceInfo{
    
    private JSObject role;

    @Override
    public void SourceInfo() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    
    }
    
    @Override
    public void SourceInfo(JSObject newRole) {
        role = newRole;
        this.SourceInfo();
    }

    @Override
    public void setCouniverseAtributes(String atributes) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    
    }

    @Override
    public JSObject getRole() {
        return role;
    }
    
    @Override
    public void setRole(JSObject newRole) {
        role = newRole;
    }

    
}
