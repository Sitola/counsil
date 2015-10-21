/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import org.json.JSONObject;

/**
 *
 * @author xminarik
 */
public class SourceInfoImpl implements SourceInfo{
    
    private JSONObject role;

    @Override
    public void SourceInfo() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    
    }
    
    @Override
    public void SourceInfo(JSONObject newRole) {
        role = newRole;
        this.SourceInfo();
    }

    @Override
    public void setCouniverseAtributes(String atributes) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    
    }

    @Override
    public JSONObject getRole() {
        return role;
    }
    
    @Override
    public void setRole(JSONObject newRole) {
        role = newRole;
    }

    
}
