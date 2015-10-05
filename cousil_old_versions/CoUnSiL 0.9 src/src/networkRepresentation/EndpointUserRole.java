/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package networkRepresentation;

import java.io.Serializable;

/**
 *
 * @author Milan
 */
public class EndpointUserRole implements Serializable{
    
    private String myRole; // represents role of user
    
    /**
     * This is an empty JavaBean constructor in order to support XMLEncoder and XMLDecoder
     */
    public EndpointUserRole(){
       this.myRole=""; 
    }
    
    /**
     * EndpointUser constructor.
     * <p/>
     * @param myRole         name of user role
     */
    public EndpointUserRole(String myRole){
        this.myRole=myRole;
    }

    public String getMyRole() {
        return myRole;
    }
    
    public void setMyRole(String myRole) {
        this.myRole=myRole;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EndpointUserRole that = (EndpointUserRole) o;

        return myRole.equals(that.myRole);

    }

    @Override
    public int hashCode() {
        return myRole.hashCode();
    }
    
}
