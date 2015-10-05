/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package counsil;

import java.io.Serializable;
import java.util.Objects;
import networkRepresentation.EndpointUserRole;
import networkRepresentation.NetworkSite;

/**
 *
 * @author Jarek
 */
public class CounsilNetworkNodeLight implements Serializable{
    private NetworkSite nodeSite;
    private EndpointUserRole myEndpointUserRole;
    private boolean isDistributor;
    
    public CounsilNetworkNodeLight(NetworkSite nodeSite, EndpointUserRole myEndpointUserRole, boolean isDistributor) {
        this.nodeSite = nodeSite;
        this.myEndpointUserRole = myEndpointUserRole;
        this.isDistributor = isDistributor;
        
    }

    public NetworkSite getNodeSite() {
        return nodeSite;
    }

    public EndpointUserRole getMyEndpointUserRole() {
        return myEndpointUserRole;
    }

    public boolean isIsDistributor() {
        return isDistributor;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.nodeSite);
        hash = 97 * hash + Objects.hashCode(this.myEndpointUserRole);
        hash = 97 * hash + (this.isDistributor ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CounsilNetworkNodeLight other = (CounsilNetworkNodeLight) obj;
        if (!Objects.equals(this.nodeSite, other.nodeSite)) {
            return false;
        }
        if (!Objects.equals(this.myEndpointUserRole, other.myEndpointUserRole)) {
            return false;
        }
        if (this.isDistributor != other.isDistributor) {
            return false;
        }
        return true;
    }
    
    
}
