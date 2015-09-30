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
public interface SourceInfo {
    public void SourceInfo();
    public void SourceInfo(String newRole);
    public String getRole();
    public void setRole(String newRole);           //is role a String?
    public void setCouniverseAtributes(String atributes);
    
    String role = new String();
    //probly some have some private atribute couniverse or at least some part of couniverse to be able set couniverse atributes
}

