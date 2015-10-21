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
public interface SourceInfo {
    public void SourceInfo();
    public void SourceInfo(JSONObject newRole);
    public JSONObject getRole();
    public void setRole(JSONObject newRole);           //is role a JSONObject?
    public void setCouniverseAtributes(String atributes);
    
    //private JSObject role;
    //probly some have some private atribute couniverse or at least some part of couniverse to be able set couniverse atributes
}

