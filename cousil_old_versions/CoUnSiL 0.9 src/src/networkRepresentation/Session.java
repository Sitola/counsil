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
public class Session implements Serializable{
    
    private String sessionName; //represents name od session
    
    /**
     * This is an empty JavaBean constructor in order to support XMLEncoder and XMLDecoder
     */
    public Session(){
        this.sessionName="";
    }

    /**
     * Session constructor.
     * <p/>
     * @param sessionName         name of session
     */
    public Session(String sessionName){
        this.sessionName=sessionName;
    }
    
    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Session that = (Session) o;

        return sessionName.equals(that.sessionName);

    }

    @Override
    public int hashCode() {
        return sessionName.hashCode();
    }
}
