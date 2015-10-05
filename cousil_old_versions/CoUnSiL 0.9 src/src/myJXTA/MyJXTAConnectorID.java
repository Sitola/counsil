/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package myJXTA;

import java.io.Serializable;
import p2p.ConnectorID;

/**
 *
 * @author maara
 */
public class MyJXTAConnectorID implements ConnectorID, Serializable {
    
    private String id;
    
    public MyJXTAConnectorID() {
    }
    
    public MyJXTAConnectorID(String id) {
        this.id = id;
    }

    public void setId(String id) {
//        if (id != null) {
//            throw new UnsupportedOperationException("ID is already set!");
//        }
//        
        this.id = id;
    }
    
    @Override
    public String getId() {
//        if (id == null) {
//            throw new NullPointerException("The ID was not set yet!");
//        }
        
        return id;
    }
    
    @Override
    public String toString() {
        return id;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        
        final MyJXTAConnectorID other = (MyJXTAConnectorID) obj;
        return this.id.equals(other.id);
    }

    @Override
    public int compareTo(Object t) {
        if (! (t instanceof MyJXTAConnectorID)) throw new IllegalArgumentException("Object " + t.getClass() + " not comparable to MyJXTAConnectorID");
        return this.id.compareTo(((MyJXTAConnectorID) t).id);
    }
    
    
}
